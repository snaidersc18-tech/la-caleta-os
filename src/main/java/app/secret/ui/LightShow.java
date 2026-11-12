package app.secret.ui;

import app.secret.audio.AudioManager;
import javafx.animation.AnimationTimer;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

/**
 * Show de luces rítmico: haces que barren el escenario y cambian de color (rojo, azul, verde, amarillo) en cada beat,
 * con un destello de estrobo en el primer tiempo de cada compás.
 * <p>
 * Todo lo mueve UN AnimationTimer que lee la posición de la música en beats ({@link AudioManager#beats()}): el cambio
 * de color ocurre en el frame en que la canción cruza un golpe y el barrido (un vaivén de 8 beats) es una función
 * directa de esa posición. Sin {@link #syncTo(AudioManager)} o sin música, usa un reloj propio a 125 BPM.
 */
public final class LightShow extends Pane {

    private static final Color[] COLORS = {Color.web("#ff2f3f"), Color.web("#2f6bff"), Color.web("#2fe05a"), Color.web("#ffd92f")};
    private static final double FALLBACK_BEAT_MS = 480;

    private final Polygon[] beams = new Polygon[7];
    private final Rotate[] rot = new Rotate[7];
    private final Rectangle strobe;
    private AudioManager audio;
    private AnimationTimer timer;
    private long wallStart;
    private long lastBeat = Long.MIN_VALUE;

    public LightShow(double w, double h) {
        setPrefSize(w, h); setMinSize(w, h); setMaxSize(w, h);
        setMouseTransparent(true);
        setBlendMode(BlendMode.ADD);
        for (int i = 0; i < beams.length; i++) {
            Polygon b = new Polygon(0, 0, -62, h + 40, 62, h + 40);
            b.setLayoutX(w * (0.12 + i * 0.126));
            b.setLayoutY(-20);
            rot[i] = new Rotate(0, 0, 0);
            b.getTransforms().add(rot[i]);
            beams[i] = b;
            getChildren().add(b);
        }
        strobe = new Rectangle(w, h, Color.WHITE);
        strobe.setOpacity(0);
        getChildren().add(strobe);
        recolor(0);
    }

    /** Engancha el show al pulso de la canción en curso. */
    public LightShow syncTo(AudioManager a) { this.audio = a; return this; }

    public void start() {
        stop();
        wallStart = System.nanoTime();
        timer = new AnimationTimer() {
            @Override public void handle(long now) { frame(now); }
        };
        timer.start();
    }

    public void stop() { if (timer != null) timer.stop(); }

    private void frame(long now) {
        double b = audio == null ? Double.NaN : audio.beats();
        if (Double.isNaN(b)) b = (now - wallStart) / 1e6 / FALLBACK_BEAT_MS;
        long beat = (long) Math.floor(b);
        double phase = b - beat;
        if (beat != lastBeat) {
            lastBeat = beat;
            recolor(beat);
            if (Math.floorMod(beat, 4) == 0) strobe.setFill(COLORS[(int) Math.floorMod(beat / 4, COLORS.length)]);
        }
        // estrobo: solo en el primer tiempo del compás, cae en ~220 ms
        strobe.setOpacity(Math.floorMod(beat, 4) == 0 ? 0.22 * Math.max(0, 1 - phase * 1.1) : 0);
        // barrido: vaivén de 8 beats (ida en 4, vuelta en 4) con EASE_BOTH
        for (int i = 0; i < beams.length; i++) {
            double amp = 18 + (i % 3) * 8, dir = i % 2 == 0 ? 1 : -1;
            double u = Math.floorMod((long) Math.floor(b * 1000), 8000) / 1000.0 / 4;   // 0..2
            double s = u <= 1 ? ease(u) : 1 - ease(u - 1);
            rot[i].setAngle((-amp + 2 * amp * s) * dir);
        }
    }

    private static double ease(double u) { return u * u * (3 - 2 * u); }

    private void recolor(long count) {
        for (int i = 0; i < beams.length; i++) {
            Color c = COLORS[(int) Math.floorMod(count + i, COLORS.length)];
            beams[i].setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.55)),
                    new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.0))));
        }
    }
}
