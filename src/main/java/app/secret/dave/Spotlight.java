package app.secret.dave;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

/** Foco dramático: pantalla oscura con un círculo de luz animable (centro, radio y oscuridad). */
public final class Spotlight extends Rectangle {
    public final DoubleProperty cx = new SimpleDoubleProperty(Cine.W / 2), cy = new SimpleDoubleProperty(Cine.H / 2),
            radius = new SimpleDoubleProperty(10), dark = new SimpleDoubleProperty(0.0);

    public Spotlight() {
        super(Cine.W, Cine.H);
        setMouseTransparent(true);
        cx.addListener((o, a, b) -> paint()); cy.addListener((o, a, b) -> paint());
        radius.addListener((o, a, b) -> paint()); dark.addListener((o, a, b) -> paint());
        paint();
    }

    private void paint() {
        double r = Math.max(1, radius.get());
        double d = dark.get();
        setFill(new RadialGradient(0, 0, cx.get(), cy.get(), r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0, 0, 0, 0)), new Stop(0.55, Color.color(0, 0, 0, d * 0.25)), new Stop(1, Color.color(0, 0, 0, d))));
    }
}
