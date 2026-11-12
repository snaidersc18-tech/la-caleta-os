package app.secret.dave;

import javafx.scene.Group;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/** Piezas vectoriales del tributo a Sofía que acompañan al retrato pixel art: estrellas de 4 puntas y aureola. */
public final class SofiaArt {
    private SofiaArt() {}

    /** Estrella de 4 puntas: 8 vértices alternando radio exterior s y radio interior 0.22·s. */
    public static Polygon star4(double cx, double cy, double s, Color c) {
        Polygon p = new Polygon(cx, cy - s, cx + s * 0.22, cy - s * 0.22, cx + s, cy, cx + s * 0.22, cy + s * 0.22,
                cx, cy + s, cx - s * 0.22, cy + s * 0.22, cx - s, cy, cx - s * 0.22, cy - s * 0.22);
        p.setFill(c);
        return p;
    }

    /**
     * Aureola en perspectiva. Truco: se construye un anillo CIRCULAR de radio r con n estrellas y se hace girar
     * ({@link Halo#orbit}); el grupo exterior lo aplasta con scaleY = tilt. Una rotación 2D de un círculo achatado
     * equivale a que las estrellas recorran la elipse (r·cos θ, tilt·r·sin θ): parece un aro 3D girando sin usar 3D.
     */
    public static Halo halo(double r, double tilt, int n) { return new Halo(r, tilt, n); }

    public static final class Halo extends Group {
        public final Group orbit = new Group();

        private Halo(double r, double tilt, int n) {
            Circle ring = new Circle(0, 0, r);
            ring.setFill(null);
            ring.setStroke(Color.web("#ffe08a"));
            ring.setStrokeWidth(5);
            ring.setEffect(new Glow(1.0));
            orbit.getChildren().add(ring);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n;
                Polygon st = star4(Math.cos(a) * r, Math.sin(a) * r, i % 2 == 0 ? 13 : 8, i % 2 == 0 ? Color.WHITE : Color.web("#ffd6e7"));
                st.setEffect(new Glow(1.0));
                orbit.getChildren().add(st);
            }
            getChildren().add(orbit);
            setScaleY(tilt);
        }
    }
}
