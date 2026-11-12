package app.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

import java.util.function.Function;

/**
 * Fuente única de verdad de los colores. La paleta se interpola entre CALLE (estado 1)
 * y AMOR (estado 2) con la propiedad {@code mix} (0..1); cada cambio se escribe como
 * variables CSS en el root ("-ar-fg", "-ar-accent"...) y se notifica a los nodos dibujados
 * a mano (Canvas, Polygon). Así la mutación a neón es un único Timeline sobre {@code mix}.
 */
public final class Theme {

    public record Palette(Color bg, Color panel, Color fg, Color dim, Color accent, Color accent2, Color alert) {
        Palette lerp(Palette o, double t) {
            return new Palette(bg.interpolate(o.bg, t), panel.interpolate(o.panel, t), fg.interpolate(o.fg, t),
                    dim.interpolate(o.dim, t), accent.interpolate(o.accent, t),
                    accent2.interpolate(o.accent2, t), alert.interpolate(o.alert, t));
        }
    }

    /** Negro asfalto, blanco hueso, verde tóxico. */
    public static final Palette STREET = new Palette(
            Color.web("#050505"), Color.web("#0c0c0c"), Color.web("#efece1"), Color.web("#8a8a80"),
            Color.web("#39ff14"), Color.web("#b6ff3c"), Color.web("#ff2a2a"));

    /** Neón / magenta. */
    public static final Palette LOVE = new Palette(
            Color.web("#12001f"), Color.web("#1c0030"), Color.web("#ffe1fa"), Color.web("#b36aa8"),
            Color.web("#ff2bd6"), Color.web("#00f5ff"), Color.web("#ff5ec8"));

    private static final Theme INSTANCE = new Theme();

    private final DoubleProperty mix = new SimpleDoubleProperty(0);
    private final ObjectProperty<Palette> palette = new SimpleObjectProperty<>(STREET);
    private Node cssRoot;

    private Theme() {
        mix.addListener((o, a, b) -> {
            palette.set(STREET.lerp(LOVE, Math.max(0, Math.min(1, b.doubleValue()))));
            applyCss();
        });
    }

    public static Theme get() { return INSTANCE; }

    public Palette palette() { return palette.get(); }
    public ObjectProperty<Palette> paletteProperty() { return palette; }
    public boolean isLove() { return mix.get() > 0.5; }

    /** Registra el nodo raíz que recibirá las variables CSS. */
    public void attach(Node root) { this.cssRoot = root; applyCss(); }

    private void applyCss() {
        if (cssRoot == null) return;
        Palette p = palette.get();
        cssRoot.setStyle("-fx-font-family: '" + PixelFonts.family() + "'; "
                + "-ar-bg: " + web(p.bg) + "; -ar-panel: " + web(p.panel) + "; -ar-fg: " + web(p.fg) + "; "
                + "-ar-dim: " + web(p.dim) + "; -ar-accent: " + web(p.accent) + "; "
                + "-ar-accent2: " + web(p.accent2) + "; -ar-alert: " + web(p.alert) + ";");
    }

    /** Enlaza una propiedad de relleno/trazo a un color de la paleta. */
    public static ObjectBinding<Paint> bind(Function<Palette, Color> pick) {
        return Bindings.createObjectBinding(() -> pick.apply(INSTANCE.palette.get()), INSTANCE.palette);
    }

    /** Parpadeo estroboscópico de la mutación: salta entre estados antes de asentarse. */
    public Timeline strobeTo(double target) {
        double from = mix.get();
        double mid = from + (target - from) * 0.6;
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(mix, from, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(90), new KeyValue(mix, target, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(170), new KeyValue(mix, from, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(240), new KeyValue(mix, mid, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(330), new KeyValue(mix, target, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(400), new KeyValue(mix, from + (target - from) * 0.25, Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(470), new KeyValue(mix, target, Interpolator.DISCRETE)));
        t.play();
        return t;
    }

    public static String web(Color c) {
        return String.format("#%02x%02x%02x", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255));
    }
}
