package app.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/** Franja de rayas diagonales bajo el título (detalle del video). */
public final class HatchStrip extends Canvas {
    public HatchStrip(double w, double h) {
        super(w, h);
        Theme.get().paletteProperty().addListener((o, a, b) -> draw());
        draw();
    }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setFill(Theme.get().palette().fg());
        double h = getHeight(), step = 11, slant = 8;
        for (double x = 0; x < getWidth(); x += step) {
            g.fillPolygon(new double[]{x, x + 6, x + 6 - slant, x - slant},
                    new double[]{0, 0, h, h}, 4);
        }
    }
}
