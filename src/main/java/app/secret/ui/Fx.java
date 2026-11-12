package app.secret.ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/** Pequeñas utilidades para orquestar cinemáticas: "cue" instantáneo dentro de una SequentialTransition, fades, etiquetas. */
public final class Fx {
    private Fx() {}

    /**
     * Acción puntual dentro de una SequentialTransition. Dura 1 ms a propósito: los hijos de duración CERO se saltan
     * sin ejecutar sus KeyFrame, y la acción nunca correría.
     */
    public static Timeline cue(Runnable r) { return new Timeline(new KeyFrame(Duration.millis(1), e -> r.run())); }

    public static FadeTransition fade(Node n, double ms, double from, double to) {
        FadeTransition f = new FadeTransition(Duration.millis(ms), n);
        f.setFromValue(from);
        f.setToValue(to);
        return f;
    }

    public static Label label(String text, double size, String hex) {
        Label l = new Label(text);
        l.setFont(Font.font("Monospaced", FontWeight.BOLD, size));
        l.setTextFill(Color.web(hex));
        return l;
    }
}
