package app.secret.audio;

import javafx.animation.AnimationTimer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hoja de cues anclada a la CANCIÓN: cada acción se dispara cuando el {@code currentTime} real del MediaPlayer
 * (vía {@link AudioManager#onMusicTime}, es decir {@code currentTimeProperty().addListener}) alcanza su milisegundo.
 * Nada de PauseTransition ni Timeline con tiempos adivinados: si el audio arranca tarde o un frame se pierde, los
 * cues siguen cayendo sobre el golpe que les toca.
 * <pre>
 *   Runnable stop = new SongCues()
 *           .at(45, () -> fadeIn(fondo))          // primer acorde
 *           .at(2_605, () -> bajar(retrato))      // entra la melodía
 *           .start(audio, MusicTrack.SOFIA_THEME, 0);
 * </pre>
 * Cada cue se ejecuta una sola vez y en orden (si llegan tarde, se ponen al día de golpe). Sin audio (falta el MP3
 * o no hay módulo de medios) corren con el reloj de pared desde {@code fallbackStartMs}: nada se queda esperando.
 */
public final class SongCues {

    private record Cue(double ms, Runnable action) { }
    private static final boolean DEBUG = Boolean.getBoolean("rebusque.debug");

    private final List<Cue> cues = new ArrayList<>();
    private int next;
    private boolean stopped;
    private AnimationTimer wallClock;

    /** Agenda {@code action} en el milisegundo {@code ms} de la canción (antes de {@link #start}). */
    public SongCues at(double ms, Runnable action) {
        cues.add(new Cue(ms, action));
        return this;
    }

    /** Dispara, en orden, todos los cues cuyo instante ya pasó. Solo hilo FX. */
    public void feed(double songMs) {
        while (!stopped && next < cues.size() && cues.get(next).ms() <= songMs) {
            Cue c = cues.get(next++);
            if (DEBUG && songMs < Double.MAX_VALUE) System.err.printf("[cue] %.0f ms → disparado en %.0f ms%n", c.ms(), songMs);
            c.action().run();
        }
    }

    /** Último instante agendado (ms de canción) o 0 si no hay cues. */
    public double lastMs() { return cues.isEmpty() ? 0 : cues.get(cues.size() - 1).ms(); }

    /**
     * Engancha la hoja al currentTime REAL de {@code track}. Devuelve el Runnable que la detiene (llamarlo al cerrar
     * la vista: los cues pendientes ya no se disparan).
     */
    public Runnable start(AudioManager audio, MusicTrack track, double fallbackStartMs) {
        cues.sort(Comparator.comparingDouble(Cue::ms));
        if (audio == null) { runOnWallClock(fallbackStartMs); return this::stop; }
        Runnable untap = audio.onMusicTime(track, this::feed);
        audio.whenMusicPlaying(pos -> { if (pos < 0) runOnWallClock(fallbackStartMs); });
        return () -> { untap.run(); stop(); };
    }

    private void runOnWallClock(double fromMs) {
        if (stopped || wallClock != null) return;
        wallClock = new AnimationTimer() {
            private long t0;
            @Override public void handle(long now) {
                if (t0 == 0) t0 = now;
                feed(fromMs + (now - t0) / 1e6);
                if (stopped || next >= cues.size()) stop();
            }
        };
        wallClock.start();
    }

    private void stop() {
        stopped = true;
        if (wallClock != null) wallClock.stop();
    }
}
