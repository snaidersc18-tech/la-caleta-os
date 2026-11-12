package app.animations;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.Random;

/** Temblor de pantalla con amplitud decreciente. */
public final class ScreenShake {
    private static final Random R = new Random();

    private ScreenShake() {}

    public static Timeline shake(Node target, double amplitude, int durationMs) {
        Timeline t = new Timeline();
        int step = 28;
        int n = Math.max(2, durationMs / step);
        for (int i = 0; i < n; i++) {
            double decay = 1.0 - i / (double) n;
            double dx = (R.nextDouble() * 2 - 1) * amplitude * decay;
            double dy = (R.nextDouble() * 2 - 1) * amplitude * decay;
            t.getKeyFrames().add(new KeyFrame(Duration.millis(i * step), e -> {
                target.setTranslateX(dx);
                target.setTranslateY(dy);
            }));
        }
        t.getKeyFrames().add(new KeyFrame(Duration.millis(n * step), e -> {
            target.setTranslateX(0);
            target.setTranslateY(0);
        }));
        t.play();
        return t;
    }
}
