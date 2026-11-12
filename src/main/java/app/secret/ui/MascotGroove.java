package app.secret.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Hace "cantar" a la mascota: rebote elástico (squash & stretch + balanceo) al ritmo de ~94 bpm y, en el
 * punto más alto de cada salto, sale de su cabeza una nota musical pixelada que flota hacia arriba y se
 * desvanece. Todo es reversible: stop() devuelve la mascota a su transformación original.
 */
public final class MascotGroove {

    private static final int[] COLORS = {0xffffff, 0xffe94a, 0xff9ec7, 0x9ff3ff};
    private static final double DESIGN_BEAT_MS = 640;              // el baile se diseñó a 94 BPM y se escala al tempo real

    private final Node mascot;
    private final Pane layer;
    private final Supplier<Point2D> headSpot;
    private final Random rnd = new Random();
    private final app.secret.audio.AudioManager audio;
    private final double beatMs;
    private app.secret.audio.BeatSync sync;
    private Timeline bounce;
    private int noteCount;

    public MascotGroove(Node mascot, Pane layer, Supplier<Point2D> headSpot) {
        this(mascot, layer, headSpot, null, DESIGN_BEAT_MS);
    }

    /**
     * Baile enganchado a la canción: un ciclo dura EXACTAMENTE un beat de {@code beatMs} y {@link app.secret.audio.BeatSync}
     * lo mantiene en fase con el currentTime del audio (el agachón cae en el golpe).
     */
    public MascotGroove(Node mascot, Pane layer, Supplier<Point2D> headSpot, app.secret.audio.AudioManager audio, double beatMs) {
        this.mascot = mascot; this.layer = layer; this.headSpot = headSpot; this.audio = audio; this.beatMs = beatMs;
    }

    public void start() {
        stop();
        Interpolator out = Interpolator.EASE_OUT, in = Interpolator.EASE_IN, both = Interpolator.EASE_BOTH;
        bounce = new Timeline(
                kf(0, 1.00, 1.00, 0, 0, both),
                kf(90, 1.06, 0.92, 2, 0, both),                 // se agacha
                kf(230, 0.96, 1.07, -10, 3, out),               // salta (estira) y se inclina
                kf(400, 1.00, 1.00, 0, 0, in),                  // cae
                kf(490, 1.05, 0.94, 1, -2, both),               // rebota abajo
                kf((int) DESIGN_BEAT_MS, 1.00, 1.00, 0, 0, both));
        bounce.getKeyFrames().add(new KeyFrame(Duration.millis(230 * beatMs / DESIGN_BEAT_MS), e -> emitNote()));
        bounce.setCycleCount(Animation.INDEFINITE);
        bounce.play();
        if (audio != null) {
            sync = new app.secret.audio.BeatSync(audio).lock(bounce, 1, 0);
            sync.start();
        }
    }

    private KeyFrame kf(int designMs, double sx, double sy, double ty, double rot, Interpolator i) {
        double ms = designMs * beatMs / DESIGN_BEAT_MS;
        return new KeyFrame(Duration.millis(ms),
                new KeyValue(mascot.scaleXProperty(), sx, i), new KeyValue(mascot.scaleYProperty(), sy, i),
                new KeyValue(mascot.translateYProperty(), ty, i), new KeyValue(mascot.rotateProperty(), rot, i));
    }

    private void emitNote() {
        Point2D head = headSpot.get();
        PixelGrid g = Sprites.note(noteCount++);
        Canvas c = PixelArt.canvas(g.g, new int[]{0, COLORS[noteCount % COLORS.length]}, 3);
        c.setMouseTransparent(true);
        c.relocate(head.getX() + (rnd.nextDouble() * 70 - 35) - 10, head.getY() - 6);
        c.setOpacity(0);
        layer.getChildren().add(c);

        double dx = rnd.nextDouble() * 50 - 25;
        TranslateTransition up = new TranslateTransition(Duration.millis(1900), c);
        up.setByY(-95 - rnd.nextDouble() * 35);
        up.setByX(dx);
        up.setInterpolator(Interpolator.EASE_OUT);
        Timeline fade = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(c.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(220), new KeyValue(c.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(1300), new KeyValue(c.opacityProperty(), 0.95)),
                new KeyFrame(Duration.millis(1900), new KeyValue(c.opacityProperty(), 0.0)));
        ParallelTransition pt = new ParallelTransition(up, fade);
        pt.setOnFinished(e -> layer.getChildren().remove(c));
        pt.play();
    }

    public void stop() {
        if (sync != null) { sync.stop(); sync = null; }
        if (bounce != null) { bounce.stop(); bounce = null; }
        mascot.setScaleX(1); mascot.setScaleY(1); mascot.setTranslateY(0); mascot.setRotate(0);
    }
}
