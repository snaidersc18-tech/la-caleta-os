package app.secret.dave;

import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Random;

/** Caja de herramientas de montaje estilo "Dave the Diver": interpoladores secos, cortes, cámara temblorosa, paneles de cómic. */
public final class Cine {
    private Cine() {}

    public static final double W = 1280, H = 720;
    private static final Random RND = new Random();

    /** Sale disparado y se asienta (snappy). */
    public static final Interpolator SNAP = Interpolator.SPLINE(0.12, 0.92, 0.18, 1.0);
    /** Arranca lento y revienta al final (golpe, caída). */
    public static final Interpolator SLAM = Interpolator.SPLINE(0.85, 0.0, 1.0, 0.55);
    /** Suave para cámara lenta. */
    public static final Interpolator SOFT = Interpolator.SPLINE(0.35, 0.0, 0.15, 1.0);
    /** Se pasa un poco y vuelve (back-out). */
    public static final Interpolator OVERSHOOT = new Interpolator() {
        @Override protected double curve(double t) { double c = 1.9, u = t - 1; return u * u * ((c + 1) * u + c) + 1; }
    };

    /** Acción puntual en una SequentialTransition (1 ms: los hijos de duración 0 se saltan sin ejecutarse). */
    public static Timeline cue(Runnable r) { return new Timeline(new KeyFrame(Duration.millis(1), e -> r.run())); }

    public static Animation hold(double ms) { return new PauseTransition(Duration.millis(ms)); }

    public static FadeTransition fade(Node n, double ms, double from, double to) {
        FadeTransition f = new FadeTransition(Duration.millis(ms), n);
        f.setFromValue(from); f.setToValue(to); f.setInterpolator(Interpolator.EASE_BOTH);
        return f;
    }

    public static ScaleTransition scale(Node n, double ms, double from, double to, Interpolator i) {
        ScaleTransition s = new ScaleTransition(Duration.millis(ms), n);
        s.setFromX(from); s.setFromY(from); s.setToX(to); s.setToY(to); s.setInterpolator(i);
        return s;
    }

    public static TranslateTransition move(Node n, double ms, double fx, double fy, double tx, double ty, Interpolator i) {
        TranslateTransition t = new TranslateTransition(Duration.millis(ms), n);
        t.setFromX(fx); t.setFromY(fy); t.setToX(tx); t.setToY(ty); t.setInterpolator(i);
        return t;
    }

    /** Cámara temblorosa: desplaza el nodo con amplitud decreciente y lo deja en (0,0). */
    public static Timeline shake(Node n, double ms, double amp) {
        Timeline t = new Timeline();
        int steps = Math.max(4, (int) (ms / 28));
        for (int i = 0; i < steps; i++) {
            double k = 1.0 - (double) i / steps;
            t.getKeyFrames().add(new KeyFrame(Duration.millis(ms * i / steps),
                    new javafx.animation.KeyValue(n.translateXProperty(), (RND.nextDouble() * 2 - 1) * amp * k),
                    new javafx.animation.KeyValue(n.translateYProperty(), (RND.nextDouble() * 2 - 1) * amp * k)));
        }
        t.getKeyFrames().add(new KeyFrame(Duration.millis(ms),
                new javafx.animation.KeyValue(n.translateXProperty(), 0), new javafx.animation.KeyValue(n.translateYProperty(), 0)));
        return t;
    }

    /** Fogonazo a pantalla completa. */
    public static Animation flash(Pane host, Color c, double ms) {
        Rectangle r = new Rectangle(W, H, c);
        r.setMouseTransparent(true);
        FadeTransition f = fade(r, ms, 1, 0);
        return new SequentialTransition(cue(() -> host.getChildren().add(r)), f, cue(() -> host.getChildren().remove(r)));
    }

    /** Ondas concéntricas que se escalan y se desvanecen desde un punto. */
    public static Animation rings(Pane host, double cx, double cy, int n, Color c, double maxScale) {
        ParallelTransition all = new ParallelTransition();
        for (int i = 0; i < n; i++) {
            Circle ring = new Circle(cx, cy, 60, Color.TRANSPARENT);
            ring.setStroke(c); ring.setStrokeWidth(7 - i); ring.setOpacity(0); ring.setMouseTransparent(true);
            ring.setEffect(new DropShadow(24, c));
            ScaleTransition s = scale(ring, 900, 0.2, maxScale, SNAP);
            FadeTransition f = new FadeTransition(Duration.millis(900), ring);
            f.setFromValue(0.95); f.setToValue(0);
            SequentialTransition one = new SequentialTransition(cue(() -> host.getChildren().add(ring)), new ParallelTransition(s, f),
                    cue(() -> host.getChildren().remove(ring)));
            one.setDelay(Duration.millis(i * 130));
            all.getChildren().add(one);
        }
        return all;
    }

    /** Onomatopeya de cómic ("THUD!", "DROP!"): entra con rebote y sale. */
    public static Text sfx(String s, double size, Color fill, double x, double y, double rot) {
        Text t = new Text(s);
        t.setFont(Font.font("Impact", FontWeight.BOLD, size));
        t.setFill(fill); t.setStroke(Color.BLACK); t.setStrokeWidth(size / 14); t.setStrokeType(StrokeType.OUTSIDE);
        t.setRotate(rot); t.setOpacity(0); t.setMouseTransparent(true);
        t.setX(x - t.getLayoutBounds().getWidth() / 2); t.setY(y);
        t.setEffect(new DropShadow(0, 8, 8, Color.color(0, 0, 0, 0.8)));
        return t;
    }

    public static Animation pop(Pane host, Text t, double holdMs) {
        t.setScaleX(0.1); t.setScaleY(0.1);
        return new SequentialTransition(cue(() -> { host.getChildren().add(t); t.setOpacity(1); }),
                scale(t, 160, 0.1, 1.0, OVERSHOOT), hold(holdMs), fade(t, 140, 1, 0), cue(() -> host.getChildren().remove(t)));
    }

    /** Panel de cómic: paralelogramo con borde blanco+negro, sombra intensa y entrada deslizante. */
    public static final class Panel extends Group {
        private final Group content = new Group();
        public final double w, h;

        public Panel(double x, double y, double w, double h, double skew) {
            this.w = w; this.h = h;
            Polygon shape = new Polygon(skew, 0, w, 0, w - skew, h, 0, h);
            Polygon clip = new Polygon(skew, 0, w, 0, w - skew, h, 0, h);
            Polygon bg = new Polygon(skew, 0, w, 0, w - skew, h, 0, h);
            bg.setFill(Color.web("#050508"));
            content.setClip(clip);
            shape.setFill(null); shape.setStroke(Color.WHITE); shape.setStrokeWidth(7);
            Polygon outer = new Polygon(skew, 0, w, 0, w - skew, h, 0, h);
            outer.setFill(null); outer.setStroke(Color.BLACK); outer.setStrokeWidth(13);
            getChildren().addAll(bg, content, outer, shape);
            setEffect(new DropShadow(36, 0, 14, Color.color(0, 0, 0, 0.9)));
            setLayoutX(x); setLayoutY(y);
        }

        public Group content() { return content; }
        public Panel put(Node... n) { content.getChildren().addAll(n); return this; }

        public Animation slideIn(double fromX, double fromY, double ms) {
            return new ParallelTransition(move(this, ms, fromX, fromY, 0, 0, SNAP), fade(this, ms * 0.6, 0, 1));
        }
        public Animation slideOut(double toX, double toY, double ms) {
            return new ParallelTransition(move(this, ms, 0, 0, toX, toY, SLAM), fade(this, ms, 1, 0));
        }
    }

    public static Point2D center() { return new Point2D(W / 2, H / 2); }
}
