package app.secret.ui;

import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/** Pad de controlador DJ: cuadrado neón con brillo, se hunde al pulsarlo y se enciende al pasar el mouse. */
public final class DjPad extends StackPane {

    private final Rectangle face = new Rectangle();
    private final Color neon;
    private Runnable action = () -> { };

    public DjPad(String text, String sub, String hex, double w, double h) {
        neon = Color.web(hex);
        setPrefSize(w, h); setMinSize(w, h); setMaxSize(w, h);
        face.setWidth(w - 8); face.setHeight(h - 8);
        face.setArcWidth(16); face.setArcHeight(16);
        paint(false);
        Label t = new Label(text);
        t.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));
        t.setTextFill(Color.WHITE);
        t.setWrapText(true);
        t.setTextAlignment(TextAlignment.CENTER);
        t.setMaxWidth(w - 24);
        Label s = new Label(sub);
        s.setFont(Font.font("Monospaced", 11));
        s.setTextFill(neon.brighter());
        javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(4, t, s);
        v.setAlignment(Pos.CENTER);
        getChildren().addAll(face, v);
        setEffect(new DropShadow(16, neon));
        setStyle("-fx-cursor: hand;");
        setOnMouseEntered(e -> paint(true));
        setOnMouseExited(e -> paint(false));
        setOnMousePressed(e -> { setScaleX(0.95); setScaleY(0.95); });
        setOnMouseReleased(e -> {
            ScaleTransition up = new ScaleTransition(Duration.millis(120), this);
            up.setToX(1); up.setToY(1);
            up.play();
        });
        setOnMouseClicked(e -> { if (!isDisabled()) action.run(); });
    }

    public void setOnAction(Runnable r) { this.action = r; }

    private void paint(boolean hot) {
        face.setFill(Color.color(neon.getRed() * (hot ? 0.42 : 0.18), neon.getGreen() * (hot ? 0.42 : 0.18), neon.getBlue() * (hot ? 0.42 : 0.18)));
        face.setStroke(neon);
        face.setStrokeWidth(hot ? 3.5 : 2.5);
        setOpacity(isDisabled() ? 0.45 : 1.0);
    }
}
