package app.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * "CONSOLA DE EVENTOS": cabecera blanca + cuerpo negro con TextFlow y cursor parpadeante.
 * Expone operaciones de bajo nivel (typeChar/backspace) para el efecto máquina de escribir.
 * En la cabecera, casi invisible, vive el botón oculto del backdoor.
 */
public final class ConsolePanel extends VBox {

    private static final int MAX_CHARS = 14_000;

    private final StringBuilder buf = new StringBuilder();
    private final Text body = new Text();
    private final Text cursor = new Text("█");
    /** Línea de comandos viva: eco de lo que se teclea (CALLE / SALIR). Es la única navegación de escape. */
    private final Text prompt = new Text();
    private String command = "";
    private final ScrollPane scroll;
    private final Label header = new Label("CONSOLA DE EVENTOS");
    private final Button backdoor = new Button("[ >_ EJECUTAR BACKDOOR: S.P. ]");

    public ConsolePanel() {
        getStyleClass().add("ar-console");
        body.fillProperty().bind(Theme.bind(Theme.Palette::fg));
        cursor.fillProperty().bind(Theme.bind(Theme.Palette::accent));
        body.setFont(PixelFonts.font(15, false));
        cursor.setFont(PixelFonts.font(15, false));
        prompt.fillProperty().bind(Theme.bind(Theme.Palette::accent));
        prompt.setFont(PixelFonts.font(15, true));
        updatePrompt();

        TextFlow flow = new TextFlow(body, prompt, cursor);
        flow.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
        flow.setLineSpacing(2);
        scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("ar-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        header.getStyleClass().add("ar-console-title");
        backdoor.getStyleClass().add("ar-backdoor");
        backdoor.setFocusTraversable(false);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(header, spacer, backdoor);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("ar-header");

        getChildren().addAll(bar, scroll);

        Timeline blink = new Timeline(new KeyFrame(Duration.millis(480),
                e -> cursor.setVisible(!cursor.isVisible())));
        blink.setCycleCount(Animation.INDEFINITE);
        blink.play();
    }

    public void setOnBackdoor(Runnable r) { backdoor.setOnAction(e -> r.run()); }
    public Label titleLabel() { return header; }

    /** Texto del cuerpo de la consola, de solo lectura (para escuchar el tecleo sin conocer los nodos internos). */
    public ReadOnlyStringProperty bodyTextProperty() { return body.textProperty(); }

    // ---- API de escritura (llamar siempre desde el hilo de JavaFX)

    public void println(String s) { buf.append(s).append('\n'); refresh(); }
    public void typeChar(char c) { buf.append(c); refresh(); }
    public void newLine() { buf.append('\n'); refresh(); }

    public void backspace() {
        if (buf.length() > 0 && buf.charAt(buf.length() - 1) != '\n') {
            buf.setLength(buf.length() - 1);
            refresh();
        }
    }

    public void clear() { buf.setLength(0); refresh(); }

    /** Muestra lo que el usuario lleva tecleado en la línea de comandos (vacío = solo el prompt). */
    public void setCommandLine(String typed) {
        command = typed == null ? "" : typed;
        updatePrompt();
    }

    private void updatePrompt() {
        prompt.setText("rebusque@calle:~$ " + command);
        // solo al inicio de una línea: si un efecto máquina de escribir va a mitad de frase, el prompt espera
        prompt.setVisible(buf.length() == 0 || buf.charAt(buf.length() - 1) == '\n');
    }

    /** Seguro desde cualquier hilo. */
    public void log(String s) {
        if (Platform.isFxApplicationThread()) println(">>> " + s);
        else Platform.runLater(() -> println(">>> " + s));
    }

    private void refresh() {
        if (buf.length() > MAX_CHARS) {
            int cut = buf.indexOf("\n", buf.length() - MAX_CHARS);
            buf.delete(0, cut < 0 ? buf.length() - MAX_CHARS : cut + 1);
        }
        body.setText(buf.toString());
        updatePrompt();
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }
}
