package app.secret.view;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.List;

/**
 * MINI-GUÍA tipo HUD de videojuego: panel semitransparente que explica qué hace la pantalla y cómo usarla.
 * <ul>
 *   <li>Abierta al entrar ({@link #start()}); se pliega sola a los 12 s si el ratón no está encima.</li>
 *   <li>Plegada queda como una pastilla "? GUÍA · F1"; clic en la cabecera o F1 la abre/cierra.</li>
 *   <li>Es una capa superpuesta: no empuja ni reordena los controles de la vista.</li>
 * </ul>
 */
public final class GuideHud extends VBox {

    private final VBox body = new VBox(8);
    private final Label head;
    private final Label toggle;
    private final PauseTransition autoCollapse = new PauseTransition(Duration.seconds(12));
    private boolean open;

    /**
     * @param accent color de neón del modo (#rrggbb)
     * @param title  nombre del módulo
     * @param what   una frase: qué hace esta pantalla
     * @param steps  pasos de uso, en orden
     * @param tip    advertencia o consejo final (puede ser null)
     */
    public GuideHud(String accent, String title, String what, List<String> steps, String tip) {
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setPrefWidth(350);
        setPadding(new Insets(10, 14, 12, 14));
        setSpacing(8);
        setPickOnBounds(false);
        setStyle("-fx-background-color: rgba(4, 7, 14, 0.82); -fx-background-radius: 10; -fx-border-radius: 10;"
                + " -fx-border-width: 2; -fx-border-color: " + accent + ";"
                + " -fx-effect: dropshadow(gaussian, " + accent + "66, 18, 0.2, 0, 0);");

        head = label("◆ " + title, 14, accent, true);
        toggle = label("", 12, accent, true);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox header = new HBox(8, head, gap, toggle);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setCursor(Cursor.HAND);
        header.setOnMouseClicked(e -> setOpen(!open));

        body.getChildren().add(label(what, 13, "#f2f2f8", false));
        for (int i = 0; i < steps.size(); i++) body.getChildren().add(label((i + 1) + ". " + steps.get(i), 12.5, "#cfd3e6", false));
        if (tip != null) body.getChildren().add(label("⚠ " + tip, 12, "#ffd166", false));
        getChildren().addAll(header, body);

        // si el usuario la está leyendo (ratón encima), no se pliega sola
        hoverProperty().addListener((o, was, now) -> { if (now) autoCollapse.stop(); });
        autoCollapse.setOnFinished(e -> { if (!isHover()) setOpen(false); });
        setOpen(false);
    }

    /**
     * La coloca sobre {@code host} (capa superior de un StackPane) y engancha F1. El filtro se instala en el host,
     * no en la escena: así deja de escuchar cuando la vista se retira, sin limpiar nada a mano.
     */
    public GuideHud attachTo(StackPane host, Pos pos, Insets margin) {
        host.getChildren().add(this);
        StackPane.setAlignment(this, pos);
        StackPane.setMargin(this, margin);
        host.addEventFilter(KeyEvent.KEY_PRESSED, e -> { if (e.getCode() == KeyCode.F1) { setOpen(!open); e.consume(); } });
        return this;
    }

    /** Llamar al mostrarse la vista: abre la guía y arma el plegado automático. */
    public void start() {
        setOpen(true);
        autoCollapse.playFromStart();
    }

    public void stop() { autoCollapse.stop(); }

    /** Abrir/cerrar: el cuerpo se gestiona fuera del layout (managed=false) para que el panel encoja de verdad. */
    public void setOpen(boolean value) {
        open = value;
        toggle.setText(value ? "▲ cerrar · F1" : "? GUÍA DEL MÓDULO · F1");
        head.setManaged(value);                                  // plegada = solo la pastilla, ancho mínimo
        head.setVisible(value);
        setPrefWidth(value ? 350 : Region.USE_COMPUTED_SIZE);
        body.setManaged(value);
        body.setVisible(value);
        if (value) {
            FadeTransition f = new FadeTransition(Duration.millis(220), body);
            f.setFromValue(0); f.setToValue(1);
            f.play();
        } else autoCollapse.stop();
    }

    private static Label label(String text, double size, String hex, boolean bold) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(322);
        l.setFont(Font.font("Monospaced", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setStyle("-fx-text-fill: " + hex + ";");
        return l;
    }
}
