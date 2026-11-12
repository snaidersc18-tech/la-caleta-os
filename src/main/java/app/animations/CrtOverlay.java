package app.animations;

import app.ui.Settings;
import app.ui.Theme;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Random;

/**
 * Capa CRT semitransparente por encima de todo (mouseTransparent):
 *  - scanlines fijas cada 3 px
 *  - barra de refresco que baja lentamente
 *  - viñeta
 *  - parpadeo suave de brillo
 *  - tiras de glitch horizontales aleatorias
 */
public final class CrtOverlay extends Pane {

    private final Random rnd = new Random();
    private final Pane glitchLayer = new Pane();
    private final double w, h;

    public CrtOverlay(double w, double h) {
        this.w = w; this.h = h;
        setMouseTransparent(true);
        setPickOnBounds(false);
        setMinSize(w, h); setPrefSize(w, h); setMaxSize(w, h);
        glitchLayer.setMouseTransparent(true);

        // 1) barra de refresco
        Rectangle roll = new Rectangle(w, 110);
        roll.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 255, 0)), new Stop(0.5, Color.rgb(255, 255, 255, 0.055)),
                new Stop(1, Color.rgb(255, 255, 255, 0))));
        TranslateTransition rollAnim = new TranslateTransition(Duration.seconds(7), roll);
        rollAnim.setFromY(-120); rollAnim.setToY(h);
        rollAnim.setInterpolator(Interpolator.LINEAR);
        rollAnim.setCycleCount(Animation.INDEFINITE);
        rollAnim.play();

        // 2) scanlines
        Canvas scan = new Canvas(w, h);
        GraphicsContext g = scan.getGraphicsContext2D();
        g.setFill(Color.rgb(0, 0, 0, 0.24));
        for (double y = 0; y < h; y += 3) g.fillRect(0, y, w, 1);
        g.setFill(Color.rgb(255, 255, 255, 0.015));
        for (double y = 1; y < h; y += 3) g.fillRect(0, y, w, 1);

        // 3) viñeta
        Rectangle vignette = new Rectangle(w, h);
        vignette.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.78, true, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.rgb(0, 0, 0, 0)), new Stop(1, Color.rgb(0, 0, 0, 0.55))));

        getChildren().addAll(roll, scan, vignette, glitchLayer);

        // 4) parpadeo de brillo
        Timeline flicker = new Timeline(new KeyFrame(Duration.millis(70),
                e -> setOpacity(0.9 + rnd.nextDouble() * 0.1)));
        flicker.setCycleCount(Animation.INDEFINITE);
        flicker.play();

        // 5) tiras de glitch aleatorias
        scheduleGlitch();

        visibleProperty().bind(Settings.get().crt);
    }

    private void scheduleGlitch() {
        PauseTransition wait = new PauseTransition(Duration.millis(1400 + rnd.nextInt(3600)));
        wait.setOnFinished(e -> {
            if (Settings.get().glitch.get()) burst();
            scheduleGlitch();
        });
        wait.play();
    }

    private void burst() {
        int n = 1 + rnd.nextInt(3);
        Color fg = Theme.get().palette().fg();
        for (int i = 0; i < n; i++) {
            double bw = w * (0.25 + rnd.nextDouble() * 0.75);
            Rectangle r = new Rectangle(bw, 2 + rnd.nextInt(12));
            r.setFill(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 0.10 + rnd.nextDouble() * 0.14));
            r.setX(rnd.nextDouble() * (w - bw));
            r.setY(rnd.nextDouble() * h);
            glitchLayer.getChildren().add(r);
            PauseTransition p = new PauseTransition(Duration.millis(50 + rnd.nextInt(110)));
            p.setOnFinished(e -> glitchLayer.getChildren().remove(r));
            p.play();
        }
    }
}
