package app.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** Panel "MÁSCARA": cabecera blanca + marco con rejilla, esquinas en L y la araña pixelada. */
final class MaskPanel extends VBox {

    static final double W = 372, BODY_H = 330;

    private final Label header = new Label("EL GUACHIMÁN");
    private final Label inner = new Label("EL GUACHIMÁN");
    private final StackPane spiderHolder = new StackPane();
    private final Pane particles = new Pane();
    private final StackPane body = new StackPane();

    MaskPanel(SpiderCanvas spider) {
        setPrefWidth(W); setMinWidth(W); setMaxWidth(W);
        header.getStyleClass().add("ar-header-label");
        header.setMaxWidth(Double.MAX_VALUE);
        header.getStyleClass().add("ar-header");

        Canvas grid = new Canvas(W - 24, BODY_H - 24);
        Runnable draw = () -> drawGrid(grid);
        Theme.get().paletteProperty().addListener((o, a, b) -> draw.run());
        draw.run();

        inner.getStyleClass().add("ar-inner-title");
        StackPane.setAlignment(inner, Pos.TOP_CENTER);
        inner.setTranslateY(14);

        spiderHolder.getChildren().add(spider);
        spiderHolder.setTranslateY(14);
        particles.setMouseTransparent(true);

        body.getStyleClass().add("ar-frame");
        body.setPrefHeight(BODY_H);
        body.setMinHeight(BODY_H);
        body.getChildren().addAll(grid, spiderHolder, inner, particles);

        getChildren().addAll(header, body);
    }

    Pane particleLayer() { return particles; }
    Node frame() { return this; }

    void setTitle(String t) { header.setText(t); inner.setText(t); }

    private void drawGrid(Canvas c) {
        GraphicsContext g = c.getGraphicsContext2D();
        g.clearRect(0, 0, c.getWidth(), c.getHeight());
        Color fg = Theme.get().palette().fg();
        g.setStroke(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 0.16));
        g.setLineWidth(1);
        for (double x = 0; x <= c.getWidth(); x += 26) g.strokeLine(x + 0.5, 0, x + 0.5, c.getHeight());
        for (double y = 0; y <= c.getHeight(); y += 26) g.strokeLine(0, y + 0.5, c.getWidth(), y + 0.5);
        g.setStroke(fg);
        g.setLineWidth(2);
        double L = 16, w = c.getWidth(), h = c.getHeight();
        g.strokePolyline(new double[]{1, 1, L}, new double[]{L, 1, 1}, 3);
        g.strokePolyline(new double[]{w - L, w - 1, w - 1}, new double[]{1, 1, L}, 3);
        g.strokePolyline(new double[]{1, 1, L}, new double[]{h - L, h - 1, h - 1}, 3);
        g.strokePolyline(new double[]{w - L, w - 1, w - 1}, new double[]{h - 1, h - 1, h - L}, 3);
    }
}
