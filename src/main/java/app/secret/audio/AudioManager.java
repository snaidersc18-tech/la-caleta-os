package app.secret.audio;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sistema de audio de los easter eggs.
 * <ul>
 *   <li><b>SFX</b> ({@link SoundEffect}) con {@link AudioClip}: se decodifican una vez y quedan en memoria; play() es
 *       seguro desde cualquier hilo y varias instancias pueden sonar a la vez.</li>
 *   <li><b>Música</b> ({@link MusicTrack}) con {@link MediaPlayer}: streaming en bucle, una sola pista activa y
 *       crossfade de potencia constante al cambiar.</li>
 * </ul>
 * Si falta un archivo en {@code /audio/} o la plataforma no trae el módulo de medios, el sonido simplemente no suena:
 * el audio nunca puede romper una cinemática.
 */
public final class AudioManager {

    private static final String ROOT = "/audio/";
    private static final String[] SFX_EXT = {".wav", ".mp3", ".aiff"};
    private static final String[] MUSIC_EXT = {".mp3", ".m4a", ".wav", ".aiff"};
    private static final long KEY_GAP_NANOS = 28_000_000L;       // máx. ~35 clics/s: más rápido suena a zumbido

    /**
     * Fundido de ENTRADA de potencia constante: fracción = sen(t·π/2).
     * En un crossfade lineal la pista que sale y la que entra valen 0.5 a mitad de camino y la potencia percibida
     * (amplitud²) cae a 0.5² + 0.5² = 0.5, un "bache" de -3 dB. Con seno/coseno, sen² + cos² = 1 en todo instante.
     */
    private static final Interpolator EQUAL_POWER_IN = new Interpolator() {
        @Override protected double curve(double t) { return Math.sin(t * Math.PI / 2); }
    };
    /**
     * Fundido de SALIDA: queremos volumen(t) = v0·cos(t·π/2). El Timeline interpola v0 + (0 − v0)·f, es decir
     * v0·(1 − f); igualando, f = 1 − cos(t·π/2).
     */
    private static final Interpolator EQUAL_POWER_OUT = new Interpolator() {
        @Override protected double curve(double t) { return 1 - Math.cos(t * Math.PI / 2); }
    };

    private final Map<SoundEffect, Optional<AudioClip>> clips = new ConcurrentHashMap<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();
    private volatile boolean enabled = true;
    private static final double MASTER = 0.85;                    // volumen general (los niveles finos van en cada enum)
    private volatile long lastKeyClick;

    // Estado de la música: solo se toca desde el hilo de JavaFX (ver fx()).
    private MediaPlayer music;
    private MusicTrack current;
    private Timeline musicFade;

    /** Decodifica todos los SFX en un hilo daemon para que el primer play() no congele la interfaz. */
    public void preload() {
        Thread t = new Thread(() -> { for (SoundEffect s : SoundEffect.values()) clip(s); }, "rebusque-audio-preload");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------ SFX

    public void play(SoundEffect s) { play(s, 1.0); }

    /** @param rate velocidad de reproducción (1.0 = normal; también cambia el tono). */
    public void play(SoundEffect s, double rate) {
        AudioClip c = clip(s);
        if (c != null) c.play(s.volume * MASTER, 0.0, rate, 0.0, 1);
    }

    /**
     * Efecto corto POSICIONADO en pantalla: el paneo estéreo sigue la X del objeto ({@code x} sobre un ancho
     * {@code width}: borde izquierdo = −0.8, derecho = +0.8), así el sonido sale de donde ocurre.
     */
    public void playAt(SoundEffect s, double x, double width) {
        AudioClip c = clip(s);
        if (c == null) return;
        double pan = Math.max(-0.8, Math.min(0.8, (x / Math.max(1, width)) * 1.6 - 0.8));
        c.play(s.volume * MASTER, 0.0, 1.0, pan, 1);                  // WAV mono: "pan" (no "balance") lo ubica
    }

    /**
     * Posición de la música en curso (ms dentro del bucle) o -1 si no hay. Sirve para enganchar animaciones al pulso:
     * retraso = beat − (posición mod beat) hace que la animación arranque justo en el siguiente golpe. Solo hilo FX.
     */
    public double musicPositionMs() {
        return music == null || music.getStatus() != MediaPlayer.Status.PLAYING ? -1 : music.getCurrentTime().toMillis();
    }

    // ------------------------------------------------------------------ reloj de beats (rhythm sync)

    /**
     * Posición de la música en BEATS de su propio tempo ({@link MusicTrack#bpm}, contados desde
     * {@link MusicTrack#firstBeatMs}), o NaN si no suena una pista con tempo. Se lee del {@code currentTime} REAL del
     * MediaPlayer en cada llamada: una animación que la consulta en su AnimationTimer nunca se desfasa del audio
     * (un Timeline con duración fija sí acumula deriva frente al reloj de la tarjeta de sonido).
     * <p>
     * {@code floor(beats())} = número de golpe; {@code beats() % 1} = fase dentro del golpe (0 = justo en el bombo).
     */
    public double beats() {
        if (music == null || current == null || current.bpm <= 0 || music.getStatus() != MediaPlayer.Status.PLAYING) return Double.NaN;
        return (music.getCurrentTime().toMillis() - current.firstBeatMs) / current.beatMs();
    }

    /** Duración de un beat de la pista en curso (ms) o NaN. */
    public double beatMs() { return current == null || current.bpm <= 0 ? Double.NaN : current.beatMs(); }

    /**
     * Llama a {@code r} (hilo FX) con la posición en ms en cuanto la música en curso está REALMENTE sonando (el
     * MediaPlayer tarda 50–300 ms en arrancar). Así una cinemática arranca anclada al audio y no al reloj de pared.
     * Si no hay música (falta el archivo o el audio está desactivado), llama enseguida con -1: nada se queda colgado.
     */
    public void whenMusicPlaying(java.util.function.DoubleConsumer r) {
        fx(() -> {
            MediaPlayer p = music;
            if (p == null) { r.accept(-1); return; }
            if (p.getStatus() == MediaPlayer.Status.PLAYING && p.getCurrentTime().toMillis() > 0) {
                r.accept(p.getCurrentTime().toMillis());
                return;
            }
            boolean[] done = {false};
            javafx.beans.value.ChangeListener<Duration> l = new javafx.beans.value.ChangeListener<>() {
                @Override public void changed(javafx.beans.value.ObservableValue<? extends Duration> o, Duration a, Duration b) {
                    if (done[0] || b.toMillis() <= 0 || p.getStatus() != MediaPlayer.Status.PLAYING) return;
                    done[0] = true;
                    p.currentTimeProperty().removeListener(this);
                    r.accept(b.toMillis());
                }
            };
            p.currentTimeProperty().addListener(l);
            // red de seguridad: si el reproductor falla o tarda demasiado, la cinemática sigue sin sincronía
            Timeline guard = new Timeline(new KeyFrame(Duration.millis(4000), e -> {
                if (done[0]) return;
                done[0] = true;
                p.currentTimeProperty().removeListener(l);
                r.accept(-1);
            }));
            guard.play();
        });
    }

    // ------------------------------------------------------------------ reloj de la canción (currentTimeProperty)

    /** Suscriptores al reloj de la canción. Solo se tocan desde el hilo FX; CopyOnWrite porque un suscriptor puede
     *  darse de baja DENTRO de su propia notificación. */
    private final java.util.List<TimeTap> taps = new java.util.concurrent.CopyOnWriteArrayList<>();
    private record TimeTap(MusicTrack track, java.util.function.DoubleConsumer sink) { }
    /** El MediaPlayer al que está enganchado {@link #timeListener} (null = ninguno). */
    private MediaPlayer tapped;
    private final javafx.beans.value.ChangeListener<Duration> timeListener = (o, a, b) -> dispatchTime(b.toMillis());
    private javafx.animation.AnimationTimer timeRefiner;

    /**
     * Engancha {@code sink} a la posición REAL de la pista {@code t} vía {@code mediaPlayer.currentTimeProperty()}.
     * <p>
     * Sobre la carga del hilo FX: JavaFX actualiza {@code currentTime} desde un {@code java.util.Timer} cada 100 ms
     * con UN {@code Platform.runLater} por tick (10/s, verificado en javafx-media 21: {@code MediaTimerTask}); el
     * listener que añadimos aquí corre dentro de ese mismo runLater y solo compara números, así que no encolamos nada
     * extra. Como 100 ms es demasiado grueso para un golpe (un beat dura 486 ms), un {@code AnimationTimer} (pulso de
     * render, ya en el hilo FX, sin runLater) afina entre ticks leyendo {@code getCurrentTime()}, que consulta en vivo
     * la posición del reproductor nativo. Ambos alimentan al mismo {@code sink}; los consumidores (p. ej.
     * {@link VocalCues.Tracker}) son monótonos, así que recibir la misma posición dos veces es inocuo.
     * <p>
     * El enganche sobrevive a cambios de reproductor (si la pista se reinicia se vuelve a colgar del nuevo) y solo
     * entrega posiciones mientras suena {@code t}. Devuelve el Runnable que lo desengancha (llamarlo al cerrar la vista).
     */
    public Runnable onMusicTime(MusicTrack t, java.util.function.DoubleConsumer sink) {
        TimeTap tap = new TimeTap(t, sink);
        fx(() -> { taps.add(tap); retap(); });
        return () -> fx(() -> { taps.remove(tap); retap(); });
    }

    /** Cuelga el listener del reproductor actual (o lo descuelga) y enciende/apaga el afinador por frame. */
    private void retap() {
        MediaPlayer want = taps.isEmpty() ? null : music;
        if (tapped != want) {
            if (tapped != null) tapped.currentTimeProperty().removeListener(timeListener);
            tapped = want;
            if (tapped != null) tapped.currentTimeProperty().addListener(timeListener);
        }
        if (!taps.isEmpty() && timeRefiner == null) {
            timeRefiner = new javafx.animation.AnimationTimer() {
                @Override public void handle(long now) {
                    MediaPlayer p = music;
                    if (p != null && p.getStatus() == MediaPlayer.Status.PLAYING) dispatchTime(p.getCurrentTime().toMillis());
                }
            };
            timeRefiner.start();
        } else if (taps.isEmpty() && timeRefiner != null) {
            timeRefiner.stop();
            timeRefiner = null;
        }
    }

    private void dispatchTime(double ms) {
        MusicTrack c = current;
        if (c == null) return;
        for (TimeTap tp : taps) if (tp.track == c) tp.sink.accept(ms);
    }

    /** Tecla mecánica con tono aleatorio ±8 % y limitador de frecuencia (el typewriter puede ir muy rápido). */
    public void keyClick() {
        long now = System.nanoTime();
        if (now - lastKeyClick < KEY_GAP_NANOS) return;
        lastKeyClick = now;
        play(SoundEffect.KEY_CLICK, 0.92 + ThreadLocalRandom.current().nextDouble() * 0.16);
    }

    private AudioClip clip(SoundEffect s) {
        if (!enabled) return null;
        // computeIfAbsent es atómico en ConcurrentHashMap: el hilo de precarga y el de JavaFX nunca cargan dos veces.
        return clips.computeIfAbsent(s, k -> {
            URL url = resolve(k.file, SFX_EXT);
            if (url == null) return Optional.empty();
            try {
                return Optional.of(new AudioClip(url.toExternalForm()));
            } catch (RuntimeException | LinkageError ex) {          // MediaException / falta jfxmedia en la plataforma
                disable(ex);
                return Optional.empty();
            }
        }).orElse(null);
    }

    // ------------------------------------------------------------------ intro underground

    /** Momento (ms desde playIntro) en que el aerosol empieza a pintar: la cinemática arranca el trazo aquí. */
    public static final int INTRO_PAINT_AT_MS = 1250;

    private Timeline introCues;

    /**
     * Efectos de la cinemática de arranque: aerosol agitándose → siseo pintando ({@link #INTRO_PAINT_AT_MS}, la
     * cinemática usa la misma constante). La canción NO va aquí: la dispara la cinemática con {@link #playStreetTheme}
     * en el milisegundo en que termina el graffiti. Además deja el tema de la CALLE precargado (pausado) para que ese
     * play() suene sin los 50–300 ms que tarda un MediaPlayer nuevo en abrir el MP3.
     */
    public void playIntro() {
        fx(() -> {
            if (introCues != null) introCues.stop();
            play(SoundEffect.SPRAY_SHAKE);
            introCues = new Timeline(new KeyFrame(Duration.millis(INTRO_PAINT_AT_MS), e -> play(SoundEffect.SPRAY_PAINT)));
            introCues.play();
            if (current == null) prime(MusicTrack.CALLE_THEME);
        });
    }

    /** Fin del graffiti: el tema de la CALLE arranca desde el ms 0 (la voz de Jaime Garzón). */
    public void playStreetTheme() { playMusic(MusicTrack.CALLE_THEME, 0); }

    /**
     * Saltar la intro o volver con CALLE: el tema de la CALLE suena desde el beat ({@link MusicTrack#dropMs}), sin la
     * intro hablada. Si ya estaba sonando la intro hablada, salta al beat; si ya va por el beat, sigue sin cortes.
     */
    public void playStreetThemeFromDrop() {
        fx(() -> {
            if (introCues != null) { introCues.stop(); introCues = null; }
            MusicTrack t = MusicTrack.CALLE_THEME;
            if (current == t && music != null && !primed) {
                if (music.getCurrentTime().toMillis() < t.dropMs) music.seek(Duration.millis(t.dropMs));
                return;
            }
            playMusic(t, t.dropMs);
        });
    }

    // ------------------------------------------------------------------ música

    /** true = {@link #music} está precargado pero aún no se le dio play() (ver {@link #prime}). */
    private boolean primed;

    /** Cambia a la pista {@code t} desde el principio. Si ya está sonando, no hace nada (la música continúa entre fases). */
    public void playMusic(MusicTrack t) { playMusic(t, 0); }

    /**
     * Cambia a la pista {@code t} empezando en {@code startMs}. La pista anterior se detiene en seco con
     * {@code stop()} + {@code dispose()} (nunca se enciman dos canciones). Los temas de los modos (con tempo) entran
     * directo a su volumen, sin fundido, para que el primer golpe suene en su sitio; el resto entra con fundido.
     * Si {@code t} estaba precargada con {@link #prime}, se usa ese reproductor (play() inmediato).
     */
    public void playMusic(MusicTrack t, double startMs) {
        fx(() -> {
            if (t == null) return;
            if (t == current && music != null) {
                if (primed) { primed = false; start(music, t, startMs); }
                return;
            }
            MediaPlayer p = create(t);
            if (p != null) start(p, t, startMs);
        });
    }

    /** Abre la pista {@code t} y la deja lista, sin sonar, para que el próximo playMusic(t) arranque al instante. */
    private void prime(MusicTrack t) {
        if (create(t) != null) primed = true;
    }

    /** Detiene lo que suene y abre {@code t} en un MediaPlayer nuevo (null si falta el archivo o no hay audio). */
    private MediaPlayer create(MusicTrack t) {
        hardStop();
        current = t;
        URL url = enabled ? resolve(t.file, MUSIC_EXT) : null;
        if (url == null) return null;
        try {
            MediaPlayer p = new MediaPlayer(new Media(url.toExternalForm()));
            p.setCycleCount(MediaPlayer.INDEFINITE);
            p.setOnError(() -> warn("No pude reproducir " + t.file + ": " + p.getError()));
            if (t.loopsFromDrop()) loopFromDrop(p, t);
            music = p;
            retap();
            respec();
            return p;
        } catch (RuntimeException | LinkageError ex) {
            disable(ex);
            return null;
        }
    }

    private void start(MediaPlayer p, MusicTrack t, double startMs) {
        if (Boolean.getBoolean("rebusque.debug")) System.err.printf("[musica] %s desde %.0f ms%n", t, startMs);
        boolean synced = t.bpm > 0;
        p.setVolume(synced ? t.volume * MASTER : 0);
        if (startMs > 0) {
            Runnable go = () -> { if (music == p) { p.seek(Duration.millis(startMs)); p.play(); } };
            if (p.getStatus() == MediaPlayer.Status.READY || p.getStatus() == MediaPlayer.Status.PAUSED) go.run();
            else p.setOnReady(go);
        } else {
            p.play();
        }
        if (!synced) musicFade = fade(p, t.volume * MASTER, 900, EQUAL_POWER_IN, null);
    }

    /**
     * En cuanto la reproducción pasa por el drop, {@code startTime} se mueve ahí: con {@code INDEFINITE} cada vuelta
     * del bucle arranca en el beat y la intro hablada no se repite. (Fijarlo antes haría que el play() inicial también
     * empezara en el drop.)
     */
    private static void loopFromDrop(MediaPlayer p, MusicTrack t) {
        p.currentTimeProperty().addListener(new javafx.beans.value.ChangeListener<>() {
            @Override public void changed(javafx.beans.value.ObservableValue<? extends Duration> o, Duration a, Duration b) {
                if (b.toMillis() < t.dropMs) return;
                p.currentTimeProperty().removeListener(this);
                p.setStartTime(Duration.millis(t.dropMs));
            }
        });
    }

    // ------------------------------------------------------------------ espectro en vivo (AudioSpectrumListener)

    /** Bandas del espectro: 128 de 0 a 22.05 kHz (~172 Hz cada una; la 0 lleva el bombo y el bajo). */
    public static final int SPECTRUM_BANDS = 128;
    private static final int SPECTRUM_FLOOR_DB = -70;
    private static final double SPECTRUM_INTERVAL_S = 1 / 40.0;

    private record SpecTap(MusicTrack track, java.util.function.Consumer<float[]> sink) { }
    private final java.util.List<SpecTap> specTaps = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final float[] spectrum = new float[SPECTRUM_BANDS];
    /** El MediaPlayer que tiene puesto nuestro AudioSpectrumListener (null = ninguno). */
    private MediaPlayer specHooked;

    /**
     * Espectro REAL de la pista {@code t} mientras suena: {@code mediaPlayer.setAudioSpectrumListener} entrega 40 veces/s
     * (en el hilo FX) la magnitud de {@link #SPECTRUM_BANDS} bandas, que aquí se normaliza a 0..1 (0 = −70 dB o menos,
     * 1 = 0 dB). El arreglo se reutiliza entre llamadas: el suscriptor lo lee en el momento y no lo guarda.
     * Varios suscriptores comparten un solo listener; si la pista se reinicia en otro reproductor, se vuelve a colgar.
     * El análisis cuesta CPU, así que solo corre mientras haya alguien suscrito: llamar al Runnable devuelto al cerrar.
     */
    public Runnable onMusicSpectrum(MusicTrack t, java.util.function.Consumer<float[]> sink) {
        SpecTap tap = new SpecTap(t, sink);
        fx(() -> { specTaps.add(tap); respec(); });
        return () -> fx(() -> { specTaps.remove(tap); respec(); });
    }

    /** Pone (o quita) el AudioSpectrumListener en el reproductor actual según haya suscriptores. */
    private void respec() {
        MediaPlayer want = specTaps.isEmpty() ? null : music;
        if (specHooked == want) return;
        if (specHooked != null) {
            try { specHooked.setAudioSpectrumListener(null); }
            catch (RuntimeException ignored) { /* ya estaba dispose()-ado: no queda nada que descolgar */ }
        }
        specHooked = want;
        if (want == null) return;
        want.setAudioSpectrumThreshold(SPECTRUM_FLOOR_DB);
        want.setAudioSpectrumNumBands(SPECTRUM_BANDS);
        want.setAudioSpectrumInterval(SPECTRUM_INTERVAL_S);
        want.setAudioSpectrumListener((ts, dur, mags, phases) -> {
            MusicTrack c = current;
            if (c == null) return;
            int n = Math.min(SPECTRUM_BANDS, mags.length);
            for (int i = 0; i < n; i++) spectrum[i] = Math.max(0, Math.min(1, (mags[i] - SPECTRUM_FLOOR_DB) / -SPECTRUM_FLOOR_DB));
            for (SpecTap tp : specTaps) if (tp.track == c) tp.sink.accept(spectrum);
        });
    }

    /**
     * Nivel de VOZ (0..1) de la pista {@code t}: energía media de las bandas de ~340 a ~3400 Hz (donde vive la voz
     * hablada), leída de {@link #onMusicSpectrum}. Devuelve el Runnable que lo apaga.
     */
    public Runnable onMusicVoiceLevel(MusicTrack t, java.util.function.DoubleConsumer sink) {
        return onMusicSpectrum(t, b -> {
            double sum = 0;
            for (int i = 2; i <= 19; i++) sum += b[i];
            sink.accept(sum / 18);
        });
    }

    /** Apaga la música con un fundido de {@code ms} milisegundos (0 = stop() en seco). */
    public void stopMusic(double ms) {
        fx(() -> {
            if (ms <= 0) hardStop(); else fadeOutAndDispose(music, ms);
            primed = false;
            music = null;
            current = null;
            retap();
            respec();
        });
    }

    /** CALLE / cambio de modo: la música y los efectos que sigan sonando se cortan YA (stop() limpio). */
    public void stopAll() {
        fx(() -> {
            hardStop();
            current = null;
            clips.values().forEach(o -> o.ifPresent(AudioClip::stop));
        });
    }

    private void hardStop() {
        primed = false;
        if (musicFade != null) { musicFade.stop(); musicFade = null; }
        if (music != null) { music.stop(); music.dispose(); music = null; }
        retap();
        respec();
    }

    /** Libera todo (cerrar la app). */
    public void shutdown() {
        fx(() -> {
            if (introCues != null) introCues.stop();
            if (musicFade != null) musicFade.stop();
            if (music != null) { music.stop(); music.dispose(); music = null; }
            current = null;
            taps.clear();
            retap();
            respec();
            clips.values().forEach(o -> o.ifPresent(AudioClip::stop));
        });
    }

    private void fadeOutAndDispose(MediaPlayer p, double ms) {
        if (p == null) return;
        if (musicFade != null) musicFade.stop();
        fade(p, 0, ms, EQUAL_POWER_OUT, () -> { p.stop(); p.dispose(); });
    }

    private static Timeline fade(MediaPlayer p, double to, double ms, Interpolator curve, Runnable done) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(p.volumeProperty(), p.getVolume())),
                new KeyFrame(Duration.millis(Math.max(1, ms)), new KeyValue(p.volumeProperty(), to, curve)));
        if (done != null) t.setOnFinished(e -> done.run());
        t.play();
        return t;
    }

    // ------------------------------------------------------------------ utilidades

    /** MediaPlayer y Timeline pertenecen al hilo de JavaFX: si nos llaman desde otro hilo se reencola con runLater. */
    private static void fx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    private URL resolve(String base, String[] extensions) {
        for (String ext : extensions) {
            URL u = AudioManager.class.getResource(ROOT + base + ext);
            if (u != null) return u;
        }
        warn("Falta el audio " + ROOT + base + " (se omite en silencio).");
        return null;
    }

    private void disable(Throwable ex) {
        enabled = false;
        warn("Audio desactivado: " + ex);
    }

    private void warn(String msg) {
        if (warned.add(msg)) System.err.println("[audio] " + msg);
    }
}
