package app.secret.view;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.BeatSync;
import app.secret.audio.MusicTrack;
import app.secret.audio.SoundEffect;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import app.secret.ui.ConchoSprite;
import app.secret.ui.Fx;
import app.secret.ui.PixelArt;
import app.secret.ui.PixelBackdrop;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * CINEMÁTICA "UN VERANO SIN CIFRAR" (segunda fase, tras el preludio del Corazón Triste):
 *   1. Barrido (clip animado + barra brillante) que revela la playa pixel art del atardecer.
 *   2. Concho entra caminando al ritmo de la canción: TranslateTransition lineal de 6 beats + un paso por medio beat
 *      + un rebote por beat anclado al audio con BeatSync.
 *   3. Se detiene, levanta la cámara (ScaleTransition + TranslateTransition hacia el centro) y apunta al usuario.
 *   4. ¡CLICK! en el primer tiempo del siguiente compás: obturador y la pantalla queda en blanco puro; avisa al orquestador, que monta la UI
 *      tropical DEBAJO; dissolve() baja la opacidad de 1.0 a 0.0 en 200 ms y el flash "revela" la interfaz.
 */
public final class VeranoCinematicView extends TakeoverView implements Cinematic {

    private static final double PX = 8;

    private final SecretContext ctx;
    private final Rectangle black = new Rectangle(W, H, Color.BLACK);
    private final Pane beachWrap = new Pane();
    private final PixelBackdrop beach = PixelBackdrop.beach(160, 90, 8);
    private final Rectangle clip = new Rectangle(0, H);
    private final Rectangle sweepBar = new Rectangle(14, H, Color.web("#fff0c8"));
    private final Canvas concho = new Canvas(ConchoSprite.W * PX, ConchoSprite.H * PX);
    private final Label bubble = Fx.label("", 24, "#2a1020");
    private final Rectangle white = new Rectangle(W, H, Color.WHITE);

    /** Beat MEDIDO de la canción de Bad Bunny (108 BPM = 555.6 ms). */
    private static final double BEAT = MusicTrack.BAD_BUNNY_THEME.beatMs();

    private Runnable onFinished = () -> { };
    private SequentialTransition seq;
    private Timeline bob;
    private AnimationTimer waiter;
    private final BeatSync sync;
    private boolean clicked;

    public VeranoCinematicView(SecretContext ctx) {
        this.ctx = ctx;
        this.sync = new BeatSync(ctx.audio());
        onDispose(() -> { sync.stop(); if (waiter != null) waiter.stop(); });

        beachWrap.setPrefSize(W, H);
        beachWrap.setClip(clip);
        beachWrap.getChildren().add(beach);
        sweepBar.setOpacity(0);
        concho.relocate(0, 296);
        concho.setTranslateX(-320);
        bubble.setStyle("-fx-background-color: white; -fx-padding: 10 18 10 18; -fx-border-color: #2a1020; -fx-border-width: 4;"
                + " -fx-background-radius: 14; -fx-border-radius: 14;");
        bubble.relocate(430, 120);
        bubble.setOpacity(0);
        white.setOpacity(0);
        white.setMouseTransparent(true);
        sweepBar.setMouseTransparent(true);

        getChildren().addAll(black, beachWrap, sweepBar, concho, bubble, white);
        drawConcho(0, false);

        setOnMouseClicked(e -> skipToShot());
        setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) skipToShot(); });
        onDispose(beach::stop);
    }

    @Override public Node node() { return this; }

    @Override public void play(Runnable onFinished) {
        this.onFinished = onFinished;
        requestFocus();
        black.setOpacity(0);

        // 1) barrido: el ancho del clip va de 0 a W con EASE_BOTH y la barra brillante sigue su borde (mismo KeyFrame,
        //    mismo interpolador → nunca se despegan).
        Timeline sweep = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(clip.widthProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(sweepBar.translateXProperty(), 0, Interpolator.EASE_BOTH), new KeyValue(sweepBar.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(1800), new KeyValue(clip.widthProperty(), W, Interpolator.EASE_BOTH),
                        new KeyValue(sweepBar.translateXProperty(), W, Interpolator.EASE_BOTH), new KeyValue(sweepBar.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(2000), new KeyValue(sweepBar.opacityProperty(), 0)));

        // 2) Concho camina AL RITMO de la canción: 500 px en 6 beats LINEAL; un paso por medio beat y un rebote por
        //    beat (cae en el golpe). El rebote lo ancla BeatSync al currentTime real del audio.
        double walkMs = 6 * BEAT;
        TranslateTransition walk = new TranslateTransition(Duration.millis(walkMs), concho);
        walk.setToX(500);
        walk.setInterpolator(Interpolator.LINEAR);
        Timeline steps = new Timeline(new KeyFrame(Duration.millis(BEAT / 2), e -> drawConcho(((int) (concho.getTranslateX() / 40) % 2 == 0) ? 1 : 2, false)));
        steps.setCycleCount(12);
        bob = track(new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(concho.layoutYProperty(), 296)),
                new KeyFrame(Duration.millis(BEAT / 2), new KeyValue(concho.layoutYProperty(), 288, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(BEAT), new KeyValue(concho.layoutYProperty(), 296, Interpolator.EASE_IN))));
        bob.setCycleCount(Animation.INDEFINITE);
        sync.lock(bob, 1, 0.5);                                         // abajo en el golpe, arriba en el contratiempo
        ParallelTransition walking = new ParallelTransition(walk, steps);

        // 3) levanta la cámara y apunta al usuario: zoom + traslación al centro
        ScaleTransition grow = new ScaleTransition(Duration.millis(1000), concho);
        grow.setToX(1.3); grow.setToY(1.3);
        TranslateTransition center = new TranslateTransition(Duration.millis(1000), concho);
        center.setToX(W / 2 - concho.getWidth() / 2);
        center.setToY(-20);
        ParallelTransition aim = new ParallelTransition(grow, center);
        aim.setInterpolator(Interpolator.EASE_BOTH);

        seq = track(new SequentialTransition(
                Fx.fade(black, 500, 0, 1),
                Fx.cue(beach::start), sweep,
                new PauseTransition(Duration.millis(400)),
                Fx.cue(() -> { bob.play(); sync.start(); }),
                walking,
                Fx.cue(() -> { sync.stop(); bob.stop(); concho.setLayoutY(296); drawConcho(0, false); }),
                new PauseTransition(Duration.millis(600)),
                Fx.cue(() -> { drawConcho(3, false); say("¡Sonríe pa' la foto!"); }),
                aim,
                new PauseTransition(Duration.millis(300))));
        seq.setOnFinished(e -> clickOnDownbeat());
        seq.play();
    }

    /**
     * El ¡CLICK! cae en el PRIMER TIEMPO del siguiente compás de la canción (posición real del audio, leída cada
     * frame): el flash blanco es un golpe más de la música. Sin audio, dispara ya.
     */
    private void clickOnDownbeat() {
        double b = ctx.audio().beats();
        if (Double.isNaN(b)) { click(); return; }
        double target = Math.ceil((b + 0.5) / 4) * 4;
        waiter = new AnimationTimer() {
            @Override public void handle(long now) {
                double x = ctx.audio().beats();
                if (Double.isNaN(x) || x >= target) { stop(); click(); }
            }
        };
        waiter.start();
    }

    /** Clic / ESC: salta directo a la foto. */
    private void skipToShot() {
        if (clicked) return;
        if (seq != null) seq.stop();
        if (waiter != null) waiter.stop();
        click();
    }

    private void say(String msg) {
        bubble.setText(msg);
        Fx.fade(bubble, 250, 0, 1).play();
    }

    /** ¡CLICK! Flash blanco a opacidad 1.0 + obturador; bajo el blanco se apaga la playa y se entrega el control. */
    private void click() {
        if (clicked) return;
        clicked = true;
        ctx.audio().play(SoundEffect.CAMERA_FLASH);
        drawConcho(3, true);
        white.setOpacity(1.0);
        beachWrap.setVisible(false);
        sweepBar.setVisible(false);
        concho.setVisible(false);
        bubble.setVisible(false);
        black.setVisible(false);
        beach.stop();
        onFinished.run();
    }

    /** El flash: como solo queda el rectángulo blanco, desvanecer la vista entera = opacidad 1.0 → 0.0 en 200 ms. */
    @Override public void dissolve() {
        setMouseTransparent(true);
        javafx.animation.FadeTransition f = Fx.fade(this, 200, 1.0, 0.0);
        f.setOnFinished(e -> disposeAndRemove());
        f.play();
    }

    @Override public void stop() {
        clicked = true;
        disposeAndRemove();
    }

    private void drawConcho(int pose, boolean flash) {
        GraphicsContext g = concho.getGraphicsContext2D();
        g.clearRect(0, 0, concho.getWidth(), concho.getHeight());
        PixelArt.draw(g, ConchoSprite.generate(pose, flash), ConchoSprite.PALETTE, 0, 0, PX);
    }
}
