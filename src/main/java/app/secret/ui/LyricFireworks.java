package app.secret.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fuegos artificiales que ESCRIBEN: cada cohete sube desde el público y, en el instante exacto en que llega a su
 * punto máximo, explota en un anillo de chispas y revela de golpe una frase en texto neón, que se expande
 * ({@link ScaleTransition} con rebote + {@link FadeTransition}) y luego se desvanece.
 * <p>
 * Las chispas viven en UN Canvas que el dueño avanza desde su AnimationTimer con {@link #step(double)}; los textos
 * son nodos {@link Text} sin efectos (el resplandor es una copia con trazo grueso translúcido detrás), así que no hay
 * nada que se recalcule por frame salvo las transiciones de escala y opacidad, que la GPU hace casi gratis.
 * El vuelo dura exactamente lo que se pida: quien lo usa lanza el cohete {@code flightMs} antes de la frase y la
 * explosión cae en su milisegundo.
 */
public final class LyricFireworks extends Pane {

    private static final Color[] NEON = {Color.web("#00e5ff"), Color.web("#ff2bd6"), Color.web("#ffe600"),
            Color.web("#39ff14"), Color.web("#8eeaf6"), Color.web("#ff7aa2")};

    private final Canvas canvas;
    private final Group words = new Group();
    private final Random rnd = new Random(23);
    private final List<Rocket> rockets = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private int colorTurn;

    private static final class Rocket {
        double x0, y0, x1, y1, t, dur;
        String text;
        Color color;
    }

    private static final class Spark {
        double x, y, vx, vy, life, max, size;
        Color c;
    }

    public LyricFireworks(double w, double h) {
        canvas = new Canvas(w, h);
        getChildren().addAll(canvas, words);
        setMouseTransparent(true);
        setPickOnBounds(false);
    }

    /** Cohete con frase: sale de (x0, y0) y explota tras {@code flightMs} en (x, y) mostrando {@code text}. */
    public void launchPhrase(String text, double x0, double y0, double x, double y, double flightMs) {
        launch(text, x0, y0, x, y, flightMs);
    }

    /** Cohete de pirotecnia sin texto (las salvas de los drops). */
    public void launchSparkle(double x0, double y0, double x, double y, double flightMs) {
        launch(null, x0, y0, x, y, flightMs);
    }

    private void launch(String text, double x0, double y0, double x, double y, double flightMs) {
        Rocket r = new Rocket();
        r.x0 = x0; r.y0 = y0; r.x1 = x; r.y1 = y; r.dur = Math.max(0.05, flightMs / 1000); r.text = text;
        r.color = NEON[colorTurn++ % NEON.length];
        rockets.add(r);
    }

    /** Avanza y dibuja un frame (llamar desde el AnimationTimer del dueño). */
    public void step(double dt) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (int i = rockets.size() - 1; i >= 0; i--) {
            Rocket r = rockets.get(i);
            r.t += dt;
            double u = Math.min(1, r.t / r.dur);
            if (u >= 1) {
                rockets.remove(i);
                explode(r);
                continue;
            }
            g.setFill(r.color);                                          // estela: puntitos que se apagan hacia abajo
            for (int k = 6; k >= 0; k--) {
                double uk = Math.max(0, u - k * 0.018);
                g.setGlobalAlpha(1 - k / 7.0);
                double s = k == 0 ? 4 : 2.5;
                g.fillRect(x(r, uk) - s / 2, y(r, uk) - s / 2, s, s);
            }
        }
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.vy += 110 * dt;
            s.vx *= 1 - 0.9 * dt;
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.life -= dt;
            if (s.life <= 0) { sparks.remove(i); continue; }
            g.setFill(s.c);
            g.setGlobalAlpha(Math.min(1, s.life / s.max * 1.4));
            g.fillRect(s.x - s.size / 2, s.y - s.size / 2, s.size, s.size);
        }
        g.setGlobalAlpha(1);
    }

    /** Vuelo: x lineal, y con frenado (ease-out cuadrático): llega a su punto máximo con velocidad cero. */
    private static double x(Rocket r, double u) { return r.x0 + (r.x1 - r.x0) * u; }
    private static double y(Rocket r, double u) { return r.y0 + (r.y1 - r.y0) * (1 - (1 - u) * (1 - u)); }

    private void explode(Rocket r) {
        int n = r.text == null ? 54 : 36;
        for (int i = 0; i < n; i++) {
            Spark s = new Spark();
            double a = 2 * Math.PI * i / n + rnd.nextDouble() * 0.12, v = (r.text == null ? 150 : 210) + rnd.nextDouble() * 110;
            s.x = r.x1; s.y = r.y1; s.vx = Math.cos(a) * v; s.vy = Math.sin(a) * v * (r.text == null ? 1 : 0.55);
            s.life = s.max = 0.9 + rnd.nextDouble() * 0.6;
            s.size = 3;
            s.c = i % 4 == 0 ? Color.WHITE : r.color;
            sparks.add(s);
        }
        if (r.text != null) phrase(r.text, r.x1, r.y1, r.color);
    }

    /** La frase revelada: crece desde el centro de la explosión con rebote, se sostiene y se desvanece. */
    private void phrase(String text, double x, double y, Color c) {
        double size = text.length() <= 10 ? 70 : text.length() <= 18 ? 52 : 38;
        Font f = Font.font("Arial Black", FontWeight.BLACK, size);
        Text halo = text(text, f, Color.TRANSPARENT, c.deriveColor(0, 1, 1, 0.38), 14);
        Text main = text(text, f, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(1, c)), Color.WHITE, 2);
        double w = main.getLayoutBounds().getWidth();
        halo.setX(-w / 2);
        main.setX(-w / 2);
        Group g = new Group(halo, main);
        double half = w / 2 + 20;
        g.setLayoutX(Math.max(half, Math.min(canvas.getWidth() - half, x)));   // que nunca se salga de la pantalla
        g.setLayoutY(y);
        g.setScaleX(0.25); g.setScaleY(0.25);
        g.setOpacity(0);
        words.getChildren().add(g);

        ScaleTransition grow = new ScaleTransition(Duration.millis(420), g);
        grow.setToX(1); grow.setToY(1);
        grow.setInterpolator(BACK_OUT);
        FadeTransition in = new FadeTransition(Duration.millis(120), g);
        in.setToValue(1);
        ScaleTransition drift = new ScaleTransition(Duration.millis(650), g);
        drift.setToX(1.15); drift.setToY(1.15);
        FadeTransition out = new FadeTransition(Duration.millis(650), g);
        out.setToValue(0);
        SequentialTransition life = new SequentialTransition(new ParallelTransition(grow, in),
                new PauseTransition(Duration.millis(1500)), new ParallelTransition(drift, out));
        life.setOnFinished(e -> words.getChildren().remove(g));
        life.play();
    }

    private static Text text(String s, Font f, javafx.scene.paint.Paint fill, Color stroke, double strokeW) {
        Text t = new Text(s);
        t.setFont(f);
        t.setTextOrigin(VPos.CENTER);
        t.setFill(fill);
        t.setStroke(stroke);
        t.setStrokeWidth(strokeW);
        t.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return t;
    }

    /** Quita todo lo que esté en el aire (p. ej. en el flashazo del drop). */
    public void clear() {
        rockets.clear();
        sparks.clear();
        words.getChildren().clear();
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /** "Back-out": llega, se pasa un poco y vuelve (SPLINE no admite puntos de control fuera de [0, 1]). */
    private static final Interpolator BACK_OUT = new Interpolator() {
        @Override protected double curve(double t) {
            double k = 1.7, u = t - 1;
            return 1 + (k + 1) * u * u * u + k * u * u;
        }
    };
}
