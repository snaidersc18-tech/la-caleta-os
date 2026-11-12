package app.secret.dave;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Random;

/** Ecualizador neón: barras que saltan al ritmo. */
public final class EqBars extends Group {
    private final Rectangle[] bars;
    private final Random r = new Random();
    private final Timeline t;

    public EqBars(int n, double w, double maxH, double gap) {
        bars = new Rectangle[n];
        Color[] cols = {Color.web("#00e5ff"), Color.web("#ff2bd6"), Color.web("#ffe600"), Color.web("#39ff14")};
        for (int i = 0; i < n; i++) {
            bars[i] = new Rectangle(i * (w + gap), 0, w, 10);
            bars[i].setFill(cols[i % cols.length]);
            getChildren().add(bars[i]);
        }
        setEffect(new Glow(0.9));   // UN resplandor para todo el grupo (antes: una pasada de Glow por barra y frame)
        t = new Timeline(new KeyFrame(Duration.millis(70), e -> {
            for (Rectangle b : bars) { double h = 10 + r.nextDouble() * (maxH - 10); b.setHeight(h); b.setY(maxH - h); }
        }));
        t.setCycleCount(Timeline.INDEFINITE);
    }

    public Timeline timeline() { return t; }
}
