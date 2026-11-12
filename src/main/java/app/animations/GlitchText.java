package app.animations;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Labeled;
import javafx.util.Duration;

import java.util.Random;

/** Utilidades de texto corrupto: glitch rápido y "morph" estilo descifrado. */
public final class GlitchText {
    private static final String NOISE = "█▓▒░▌▐#%&@$?/\\<>=+*";
    private static final Random R = new Random();

    private GlitchText() {}

    public static String corrupt(String s, double intensity) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c != ' ' && R.nextDouble() < intensity) sb.append(NOISE.charAt(R.nextInt(NOISE.length())));
            else sb.append(c);
        }
        return sb.toString();
    }

    /** El texto se resuelve de izquierda a derecha desde basura hasta el objetivo. */
    public static Timeline morph(Labeled node, String target, Duration total) {
        int frames = 16;
        Timeline t = new Timeline();
        for (int i = 0; i <= frames; i++) {
            final int k = i;
            t.getKeyFrames().add(new KeyFrame(total.multiply(i / (double) frames), e -> {
                int reveal = (int) Math.round(target.length() * (k / (double) frames));
                String head = target.substring(0, reveal);
                String tail = corrupt(target.substring(reveal), k == frames ? 0 : 0.85);
                node.setText(head + tail);
            }));
        }
        t.play();
        return t;
    }
}
