package app.secret.ui;

import app.secret.audio.AudioManager;
import app.secret.audio.MusicTrack;
import javafx.animation.AnimationTimer;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * La música mueve la interfaz: lee el espectro REAL de la pista ({@link AudioManager#onMusicSpectrum}, que es el
 * {@code AudioSpectrumListener} del MediaPlayer, 40 veces/s) y en cada frame:
 * <ul>
 *   <li><b>Latido</b>: el nivel de graves (banda 0-344 Hz: bombo y bajo) escala los nodos registrados con
 *       {@link #heartbeat}. Envolvente de ataque rápido / caída lenta, normalizada entre el promedio reciente y el pico
 *       reciente: palpita igual de sutil en la intro suave que tras el drop.</li>
 *   <li><b>Ondas</b>: alimenta los {@link SpectrumWave} con las bandas suavizadas.</li>
 *   <li><b>Golpes</b>: cada subida brusca de graves (un bombo) llama a {@link #onKick}, p. ej. para soltar corazones.</li>
 * </ul>
 * Sin audio no hace nada (los nodos quedan en escala 1). Llamar a {@link #stop()} al cerrar la vista.
 */
public final class MusicReactor {

    private static final double KICK_GAP_S = 0.26;              // a 99.75 BPM una corchea dura 0.30 s

    private final AudioManager audio;
    private final MusicTrack track;
    private final List<Node> pulsing = new ArrayList<>();
    private final List<Double> depths = new ArrayList<>();
    private final List<SpectrumWave> waves = new ArrayList<>();
    private Runnable onKick;

    private final float[] raw = new float[SpectrumWave.USED_BANDS], smooth = new float[SpectrumWave.USED_BANDS];
    private double rawBass, prevBass, bassAvg = 0.3, bass, bassPeak = 0.4, floor = 0.3;
    private long lastKickNs;
    private boolean kickPending;
    private Runnable untap;
    private AnimationTimer timer;

    public MusicReactor(AudioManager audio, MusicTrack track) {
        this.audio = audio;
        this.track = track;
    }

    /** {@code n} late con los graves: escala 1 → 1 + {@code depth} en cada golpe. */
    public MusicReactor heartbeat(Node n, double depth) { pulsing.add(n); depths.add(depth); return this; }

    public MusicReactor wave(SpectrumWave w) { waves.add(w); return this; }

    /** Se llama (hilo FX) en cada golpe de graves detectado. */
    public MusicReactor onKick(Runnable r) { onKick = r; return this; }

    public void start() {
        if (audio == null || timer != null) return;
        untap = audio.onMusicSpectrum(track, this::sample);
        timer = new AnimationTimer() {
            private long last;
            @Override public void handle(long now) {
                double dt = last == 0 ? 1 / 60.0 : Math.min(0.1, (now - last) / 1e9);
                last = now;
                frame(dt, now / 1e9);
            }
        };
        timer.start();
    }

    public void stop() {
        if (untap != null) untap.run();
        if (timer != null) timer.stop();
        untap = null;
        timer = null;
        for (Node n : pulsing) { n.setScaleX(1); n.setScaleY(1); }
    }

    /** 40 veces/s desde el AudioSpectrumListener. Aquí solo se copian números y se detecta el golpe. */
    private void sample(float[] bands) {
        System.arraycopy(bands, 0, raw, 0, raw.length);
        prevBass = rawBass;
        rawBass = bands[0] * 0.7 + bands[1] * 0.3;
        bassAvg += (rawBass - bassAvg) * 0.05;                   // promedio de ~0.5 s
        long now = System.nanoTime();
        boolean rising = rawBass - prevBass > 0.025;
        if (rising && rawBass > bassAvg + 0.04 && (now - lastKickNs) / 1e9 > KICK_GAP_S) {
            lastKickNs = now;
            kickPending = true;
        }
    }

    private void frame(double dt, double t) {
        double release = Math.min(1, dt * 5);
        for (int i = 0; i < raw.length; i++) {
            float target = raw[i];
            smooth[i] += (float) ((target - smooth[i]) * (target > smooth[i] ? 0.6 : release));
        }
        bass += (rawBass - bass) * (rawBass > bass ? 0.7 : Math.min(1, dt * 7));
        bassPeak = Math.max(bass, bassPeak - dt * 0.08);
        floor += (bassAvg - floor) * Math.min(1, dt * 2);
        double lvl = Math.max(0, Math.min(1, (bass - floor) / Math.max(0.04, bassPeak - floor)));
        for (int i = 0; i < pulsing.size(); i++) {
            double s = 1 + depths.get(i) * lvl;
            pulsing.get(i).setScaleX(s);
            pulsing.get(i).setScaleY(s);
        }
        for (SpectrumWave w : waves) w.draw(smooth, t);
        if (kickPending) {
            kickPending = false;
            if (onKick != null) onKick.run();
        }
    }
}
