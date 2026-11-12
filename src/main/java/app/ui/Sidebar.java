package app.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Menú lateral; el botón seleccionado queda invertido (blanco sobre negro).
 * El número de botones puede cambiar en caliente (calle = 3 opciones, modo amor = 4).
 */
final class Sidebar extends ChamferPane {

    static final double W = 262;

    private final List<GlitchButton> buttons = new ArrayList<>();
    private final VBox list = new VBox(12);
    private IntConsumer onSelect = i -> {};
    private int selected = -1;

    Sidebar(String... labels) {
        super(12, false, false, true, false);
        setPrefWidth(W); setMinWidth(W); setMaxWidth(W);

        Label title = new Label("OPERACIONES");
        title.getStyleClass().add("ar-header-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label deco = new Label("//x");
        deco.getStyleClass().add("ar-header-label");
        HBox header = new HBox(title, sp, deco);
        header.getStyleClass().add("ar-header");

        list.setPadding(new Insets(14, 12, 12, 12));
        rebuild(labels);
        VBox root = new VBox(header, list);
        root.setPadding(new Insets(3));
        getChildren().add(root);
    }

    private void rebuild(String... labels) {
        buttons.clear();
        list.getChildren().clear();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            GlitchButton b = new GlitchButton(labels[i]);
            b.getStyleClass().add("ar-menu");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> onSelect.accept(idx));
            buttons.add(b);
            list.getChildren().add(b);
        }
    }

    void setOnSelect(IntConsumer c) { this.onSelect = c; }

    void setLabels(String... labels) {
        if (labels.length != buttons.size()) {
            rebuild(labels);
            select(selected < labels.length ? selected : 0);
        }
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setBaseText(labels[i]);
            buttons.get(i).glitch();
        }
    }

    void select(int idx) {
        selected = idx;
        for (int i = 0; i < buttons.size(); i++) {
            var sc = buttons.get(i).getStyleClass();
            sc.remove("selected");
            if (i == idx) sc.add("selected");
        }
    }

    int selected() { return selected; }
}
