package app.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;

import java.util.function.Function;

/**
 * Panel con borde de 2px y esquinas biseladas (las "cuñas" del video).
 * Dibuja dos polígonos (borde + relleno) y delega el contenido a StackPane.
 */
public class ChamferPane extends StackPane {

    private final Polygon outer = new Polygon();
    private final Polygon inner = new Polygon();
    private final double cut;
    private final boolean tl, tr, br, bl;
    private final double border;

    public ChamferPane(double cut, boolean tl, boolean tr, boolean br, boolean bl) {
        this(cut, 2, tl, tr, br, bl, p -> p.bg());
    }

    public ChamferPane(double cut, double border, boolean tl, boolean tr, boolean br, boolean bl,
                       Function<Theme.Palette, javafx.scene.paint.Color> fill) {
        this.cut = cut; this.border = border;
        this.tl = tl; this.tr = tr; this.br = br; this.bl = bl;
        outer.setManaged(false); inner.setManaged(false);
        outer.setMouseTransparent(true); inner.setMouseTransparent(true);
        outer.fillProperty().bind(Theme.bind(Theme.Palette::fg));
        inner.fillProperty().bind(Theme.bind(fill));
        getChildren().addAll(outer, inner);
    }


    @Override
    protected void layoutChildren() {
        double w = getWidth(), h = getHeight();
        outer.getPoints().setAll(points(0, 0, w, h, cut));
        inner.getPoints().setAll(points(border, border, w - 2 * border, h - 2 * border, Math.max(0, cut - border * 0.6)));
        super.layoutChildren();
    }

    private Double[] points(double x, double y, double w, double h, double c) {
        java.util.List<Double> p = new java.util.ArrayList<>();
        if (tl) { p.add(x + c); p.add(y); } else { p.add(x); p.add(y); }
        if (tr) { p.add(x + w - c); p.add(y); p.add(x + w); p.add(y + c); } else { p.add(x + w); p.add(y); }
        if (br) { p.add(x + w); p.add(y + h - c); p.add(x + w - c); p.add(y + h); } else { p.add(x + w); p.add(y + h); }
        if (bl) { p.add(x + c); p.add(y + h); p.add(x); p.add(y + h - c); } else { p.add(x); p.add(y + h); }
        if (tl) { p.add(x); p.add(y + c); }
        return p.toArray(new Double[0]);
    }
}
