package app.secret.dave;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

import java.util.Random;

/** Líneas de velocidad radiales de manga; parpadean cambiando de ángulo cada 60 ms. */
public final class SpeedLines extends Group {
    private final Random r = new Random();
    private final Timeline flicker;

    public SpeedLines(double cx, double cy, int n, Color c) {
        setMouseTransparent(true);
        build(cx, cy, n, c);
        flicker = new Timeline(new KeyFrame(Duration.millis(60), e -> { setRotate(r.nextDouble() * 8); setScaleX(0.97 + r.nextDouble() * 0.08); setScaleY(getScaleX()); }));
        flicker.setCycleCount(Timeline.INDEFINITE);
        setOpacity(0);
        // 70 triángulos enormes que solo se giran/escalan en bloque: se rasterizan UNA vez y la GPU gira el bitmap
        setCache(true);
        setCacheHint(javafx.scene.CacheHint.SPEED);
    }

    private void build(double cx, double cy, int n, Color c) {
        double inner = 230, outer = 1100;
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, w = 0.01 + r.nextDouble() * 0.03;
            double in = inner + r.nextDouble() * 170;
            Polygon p = new Polygon(cx + Math.cos(a - w) * outer, cy + Math.sin(a - w) * outer,
                    cx + Math.cos(a + w) * outer, cy + Math.sin(a + w) * outer, cx + Math.cos(a) * in, cy + Math.sin(a) * in);
            p.setFill(c);
            getChildren().add(p);
        }
    }

    public void start() { flicker.play(); }
    public void stop() { flicker.stop(); }
}
