package app.secret.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

/** Cuadrícula synthwave en perspectiva que avanza hacia la cámara, mezclada en ADD sobre la interfaz. */
public final class SynthwaveGrid extends Canvas {

    private final Timeline loop;
    private double offset;

    public SynthwaveGrid(double w, double h) {
        super(w, h);
        setMouseTransparent(true);
        setBlendMode(BlendMode.ADD);
        setOpacity(0.0);
        loop = new Timeline(new KeyFrame(Duration.millis(45), e -> { offset = (offset + 0.012) % 1.0; draw(); }));
        loop.setCycleCount(Animation.INDEFINITE);
        draw();
    }

    public void start() { loop.play(); }
    public void stop() { loop.stop(); }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth(), h = getHeight(), horizon = h * 0.56;
        g.clearRect(0, 0, w, h);

        // sol con franjas
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffe94a", 0.55)), new Stop(1, Color.web("#ff2bd6", 0.55))));
        g.fillOval(w / 2 - 120, horizon - 150, 240, 240);
        g.setFill(Color.BLACK);
        for (int i = 0; i < 6; i++) g.fillRect(w / 2 - 125, horizon - 60 + i * 15, 250, 2 + i * 1.6);
        g.clearRect(0, horizon, w, h - horizon);

        // suelo en perspectiva
        g.setLineWidth(1.4);
        g.setStroke(Color.web("#ff2bd6", 0.85));
        for (int i = -24; i <= 24; i++) g.strokeLine(w / 2, horizon, w / 2 + i * 120, h);
        for (int k = 0; k < 16; k++) {
            double f = (k + offset) / 16.0;
            double y = horizon + (h - horizon) * f * f;
            g.setStroke(Color.web("#00f5ff", 0.35 + 0.6 * f));
            g.strokeLine(0, y, w, y);
        }
        g.setStroke(Color.web("#00f5ff", 0.9));
        g.setLineWidth(2);
        g.strokeLine(0, horizon, w, horizon);
    }
}
