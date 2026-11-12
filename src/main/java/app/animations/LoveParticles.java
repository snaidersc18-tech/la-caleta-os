package app.animations;

import app.ui.Theme;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Random;

/** Corazones pixelados y notas musicales que flotan hacia arriba. */
public final class LoveParticles {

    private static final int[][] HEART = {
            {0, 1, 1, 0, 1, 1, 0},
            {1, 2, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 0},
            {0, 0, 1, 1, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0}
    };
    private static final Random R = new Random();

    private Timeline spawner;

    /** Corazón pixel-art como nodo (unit = tamaño de cada "píxel"). */
    public static Canvas heart(double unit, Color base) {
        Canvas c = new Canvas(7 * unit, 6 * unit);
        GraphicsContext g = c.getGraphicsContext2D();
        for (int y = 0; y < HEART.length; y++)
            for (int x = 0; x < HEART[y].length; x++) {
                int v = HEART[y][x];
                if (v == 0) continue;
                g.setFill(v == 2 ? Color.WHITE : base);
                g.fillRect(x * unit, y * unit, unit, unit);
            }
        return c;
    }

    /** Dibuja un corazón directamente en un GraphicsContext existente (ojos de la araña). */
    public static void drawHeart(GraphicsContext g, double x, double y, double unit, Color base) {
        for (int j = 0; j < HEART.length; j++)
            for (int i = 0; i < HEART[j].length; i++) {
                int v = HEART[j][i];
                if (v == 0) continue;
                g.setFill(v == 2 ? Color.WHITE : base);
                g.fillRect(x + i * unit, y + j * unit, unit, unit);
            }
    }

    private static Node note() {
        String[] glyphs = {"♪", "♫", "♬"};
        Label l = new Label(glyphs[R.nextInt(glyphs.length)]);
        l.setFont(Font.font("Monospaced", FontWeight.BOLD, 26 + R.nextInt(12)));
        l.setTextFill(R.nextBoolean() ? Theme.get().palette().accent2() : Theme.get().palette().accent());
        return l;
    }

    private static Color heartColor() {
        Theme.Palette p = Theme.get().palette();
        double r = R.nextDouble();
        return r < 0.6 ? p.accent() : (r < 0.85 ? p.accent2() : p.fg());
    }

    private static void float_(Pane layer, Node n, double x, double y, double rise, double drift, double ms) {
        n.setMouseTransparent(true);
        n.relocate(x, y);
        n.setOpacity(0);
        layer.getChildren().add(n);
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), n);
        tt.setByY(-rise);
        tt.setByX(drift);
        // pulso de opacidad: sube rápido y se apaga al final
        Timeline fade = new Timeline(
                new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(n.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(ms * 0.15), new javafx.animation.KeyValue(n.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(ms * 0.75), new javafx.animation.KeyValue(n.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(ms), new javafx.animation.KeyValue(n.opacityProperty(), 0.0)));
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), n);
        st.setFromX(0.6); st.setFromY(0.6); st.setToX(1.25); st.setToY(1.25);
        ParallelTransition pt = new ParallelTransition(tt, fade, st);
        pt.setOnFinished(e -> layer.getChildren().remove(n));
        pt.play();
    }

    /** Ráfaga de n corazones alrededor de (cx, cy). */
    public static void burst(Pane layer, double cx, double cy, int n, double spread, boolean big) {
        for (int i = 0; i < n; i++) {
            double unit = big ? 5 + R.nextInt(3) : 3 + R.nextInt(2);
            Canvas h = heart(unit, heartColor());
            double x = cx + (R.nextDouble() * 2 - 1) * spread - 3.5 * unit;
            double y = cy + (R.nextDouble() * 2 - 1) * spread * 0.6;
            float_(layer, h, x, y, 90 + R.nextDouble() * 150, (R.nextDouble() * 2 - 1) * 60, 1400 + R.nextDouble() * 1400);
        }
    }

    /** Ráfaga de notas musicales (saxofón). */
    public static void notes(Pane layer, double cx, double cy, int n, double spread) {
        for (int i = 0; i < n; i++) {
            double x = cx + (R.nextDouble() * 2 - 1) * spread;
            double y = cy + (R.nextDouble() * 2 - 1) * 40;
            float_(layer, note(), x, y, 110 + R.nextDouble() * 160, (R.nextDouble() * 2 - 1) * 80, 1500 + R.nextDouble() * 1300);
        }
    }

    /** Corazones continuos mientras el modo amor está activo. */
    public void startAmbient(Pane layer, double x0, double x1, double yBase) {
        stop();
        spawner = new Timeline(new KeyFrame(Duration.millis(420), e -> {
            double unit = 2 + R.nextInt(3);
            Canvas h = heart(unit, heartColor());
            float_(layer, h, x0 + R.nextDouble() * (x1 - x0), yBase, 80 + R.nextDouble() * 110,
                    (R.nextDouble() * 2 - 1) * 30, 1800 + R.nextDouble() * 1200);
        }));
        spawner.setCycleCount(Animation.INDEFINITE);
        spawner.play();
    }

    public void stop() {
        if (spawner != null) { spawner.stop(); spawner = null; }
    }
}
