package app.secret.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Bitácora propia de cada interfaz secreta (la consola base queda oculta). Segura desde cualquier hilo. */
public final class LogBox extends ScrollPane {

    private final VBox box = new VBox(2);
    private final String color;

    public LogBox(String hexColor, String hexBackground, double w, double h) {
        this.color = hexColor;
        setPrefSize(w, h); setMinSize(w, h); setMaxSize(w, h);
        box.setStyle("-fx-padding: 8 12 8 12;");
        setContent(box);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background: " + hexBackground + "; -fx-background-color: " + hexBackground
                + "; -fx-border-color: " + hexColor + "; -fx-border-width: 2;");
    }

    public void println(String s) {
        Runnable r = () -> {
            Label l = new Label(s);
            l.setWrapText(true);
            l.setMaxWidth(getPrefWidth() - 40);
            l.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
            l.setTextFill(Color.web(color));
            box.getChildren().add(l);
            if (box.getChildren().size() > 80) box.getChildren().remove(0);
            Platform.runLater(() -> setVvalue(1.0));                 // tras el siguiente layout, cuando ya mide la línea nueva
        };
        // los nodos solo se tocan en el hilo de JavaFX: desde un Task se reencola la misma acción con runLater
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    /** Línea con prefijo ">>> " (mismo formato que la consola base). */
    public void log(String s) { println(">>> " + s); }
}
