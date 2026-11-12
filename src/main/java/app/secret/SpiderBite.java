package app.secret;

import app.animations.ScreenShake;
import app.secret.audio.AudioManager;
import app.secret.audio.SoundEffect;
import app.ui.SpiderCanvas;
import app.ui.SpiderSprite;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * "CALLE": El Mordisco + la física pendular del Guachimán.
 * <pre>
 *   1. CAÍDA     baja desde el borde superior central colgando de su hilo        (TranslateTransition en Y)
 *   2. MORDISCO  se encoge, muerde el centro; la pantalla tiembla y una infección
 *                de píxeles verdes / glitch se come la UI del easter egg          (ScaleTransition + Canvas)
 *   3. PANTALLA CUBIERTA → onCovered: el modo se cierra DEBAJO, sin que se vea el corte
 *   4. SWING     péndulo amortiguado desde el anclaje superior; mientras sube por su arco final la infección se
 *                deshace y reaparece el Ministerio Del Rebusque                   (Rotate con pivote + TranslateTransition)
 *   5. ATERRIZAJE el arco termina EXACTAMENTE sobre el marco original de la araña (panel EL GUACHIMÁN) → onDone
 * </pre>
 *
 * <h3>La matemática del péndulo</h3>
 * El hilo nace en el ancla A (arriba al centro). La araña cuelga a distancia r de A y el conjunto {hilo + araña} gira
 * con un {@link Rotate} cuyo pivote ES el ancla. (RotateTransition gira siempre alrededor del centro de los límites del
 * nodo, que aquí cambia mientras el hilo se alarga; por eso se anima directamente el ángulo de un Rotate con pivote fijo,
 * que es la misma interpolación con el pivote correcto.) En coordenadas de pantalla (y hacia abajo) un ángulo θ
 * positivo gira en sentido horario, así que la posición de la araña es
 * <pre>   P(θ, r) = A + r · (−sen θ, cos θ)</pre>
 * <b>Fase libre</b> (tras el mordisco): oscilación amortiguada de pequeña amplitud
 * <pre>   θ(t) = θ₀ · e^(−γt) · sen(ωt),   ω = √(g / L)</pre>
 * con L = longitud del hilo en px y una "g de pantalla" de 5000 px/s² (con L ≈ 330 px da un periodo
 * T = 2π/ω ≈ 1.6 s, el de un péndulo real de ~65 cm: se siente pesado, no de juguete). Se corta en t = π/ω, cuando la
 * araña vuelve a pasar por abajo ya lanzada hacia la derecha, con velocidad angular
 * <pre>   θ'(π/ω) = −θ₀ · ω · e^(−γπ/ω)</pre>
 * <b>Arco final</b>: hay que llegar al ángulo φ del marco de destino T con velocidad CERO (en el ápice de un péndulo la
 * velocidad es nula: ahí es donde se puede "aterrizar" sin rebote). La curva θ(u) = φ · sen(πu/2), u ∈ [0, 1], tiene
 * derivada máxima al inicio y nula al final; su duración D se elige para EMPALMAR la velocidad de la fase libre:
 * <pre>   θ'(0) = φ · π / (2D) = θ'(π/ω)   ⇒   D = φ · π / (2 · θ'(π/ω))</pre>
 * Mientras tanto el hilo se alarga de L a R = |T − A| (TranslateTransition sobre la araña a lo largo del hilo) y se
 * resuelven φ y R exactos con
 * <pre>   φ = −atan2(Tx − Ax, Ty − Ay),   R = √((Tx − Ax)² + (Ty − Ay)²)</pre>
 * de modo que P(φ, R) = T por construcción. Por último la araña se "endereza" con una contra-rotación −θ·k alrededor
 * de su propio centro (k pasa de 0 a 1 en el último tramo): aterriza derecha, con su mismo tamaño de píxel (5), justo
 * encima del sprite real, que es el que se vuelve a ver.
 */
public final class SpiderBite {

    private static final double W = SecretContext.W, H = SecretContext.H;
    private static final int CELL = 12;
    private static final double PX = 5;                             // mismo tamaño de píxel que la araña del panel
    private static final double SW = SpiderSprite.W * PX, SH = SpiderSprite.H * PX;
    private static final double WEB_Y = 5 * PX;                     // el hilo nace del centro de la telaraña del cráneo
    private static final double G_SCREEN = 5000;                    // "gravedad de pantalla" en px/s²
    private static final double THETA0 = 35, GAMMA = 0.6;           // amplitud (grados) y amortiguamiento (1/s) de la fase libre
    private static final Color[] VENOM = {Color.web("#39ff14"), Color.web("#14d93a"), Color.web("#0fa82c"), Color.web("#b8ff9e")};
    private static final Color ROT = Color.web("#03140a");          // verde podrido de fondo, ya infectado

    private final Pane host;
    private final Node shakeTarget;
    private final SpiderCanvas homeSpider;
    private final AudioManager audio;
    private final Runnable onCovered, onDone;

    private final Pane stage = new Pane();
    private final Rectangle dim = new Rectangle(W, H, Color.BLACK);
    private final Canvas infection = new Canvas(W, H);
    private final SpiderCanvas spider = new SpiderCanvas(PX);
    private final Group holder = new Group(spider);                 // la araña, desplazable a lo largo del hilo
    private final Line thread = new Line();
    private final Group pendulum = new Group(thread, holder);       // {hilo + araña}: gira alrededor del ancla
    private final Rotate swing = new Rotate(0, 0, 0);               // pivote = ancla (origen local del péndulo)
    private final Rotate upright = new Rotate(0, SW / 2, SH / 2);   // contra-rotación alrededor del centro de la araña
    private final DoubleProperty uprightK = new SimpleDoubleProperty(0);
    private final Rectangle flash = new Rectangle(W, H, Color.web("#d9ffd0"));
    private final List<Node> fangs = new ArrayList<>();
    private final Random rnd = new Random();

    private final Point2D anchor = new Point2D(W / 2, -10);
    private final double restR = H / 2 - 40 - anchor.getY();        // centro de la araña a media pantalla
    private final double biteX = W / 2, biteY = anchor.getY() + restR + 36 * PX - SH / 2;   // la boca (fila ~36 del sprite)

    private SpiderBite(Pane host, Node shakeTarget, SpiderCanvas homeSpider, AudioManager audio, Runnable onCovered, Runnable onDone) {
        this.host = host;
        this.shakeTarget = shakeTarget;
        this.homeSpider = homeSpider;
        this.audio = audio;
        this.onCovered = onCovered;
        this.onDone = onDone;
    }

    /**
     * @param host        capa a pantalla completa (coordenadas de MainView) donde se monta la animación
     * @param shakeTarget lo que tiembla con el mordisco (la interfaz del easter egg)
     * @param homeSpider  la araña real del panel EL GUACHIMÁN: destino exacto del aterrizaje
     * @param onCovered   la infección cubre el 100 % de la pantalla: cerrar el modo aquí
     * @param onDone      la araña aterrizó en su marco y la animación se desmontó
     */
    public static void play(Pane host, Node shakeTarget, SpiderCanvas homeSpider, AudioManager audio, Runnable onCovered, Runnable onDone) {
        new SpiderBite(host, shakeTarget, homeSpider, audio, onCovered, onDone).start();
    }

    // ------------------------------------------------------------------ 1. caída

    private void start() {
        stage.setMinSize(W, H); stage.setPrefSize(W, H); stage.setMaxSize(W, H);
        stage.setPickOnBounds(true);                                 // se come los clics
        stage.setFocusTraversable(true);
        stage.addEventFilter(KeyEvent.ANY, KeyEvent::consume);       // y las teclas

        dim.setOpacity(0);
        flash.setOpacity(0);
        flash.setMouseTransparent(true);

        spider.setLayoutX(-SW / 2);                                  // el eje del hilo pasa por el centro de la araña
        spider.getTransforms().add(upright);
        upright.angleProperty().bind(swing.angleProperty().multiply(-1).multiply(uprightK));
        holder.setTranslateY(-anchor.getY() - SH - 60);              // arranca fuera de pantalla, arriba
        thread.setStartX(0); thread.setStartY(0);
        thread.setEndX(0);
        thread.endYProperty().bind(holder.translateYProperty().add(WEB_Y));
        thread.setStroke(Color.web("#e8e8e8"));
        thread.setStrokeWidth(2);
        thread.setOpacity(0.85);
        pendulum.setLayoutX(anchor.getX());
        pendulum.setLayoutY(anchor.getY());
        pendulum.getTransforms().add(swing);

        stage.getChildren().addAll(dim, infection, pendulum, flash);
        host.getChildren().add(stage);
        stage.requestFocus();
        homeSpider.setOpacity(0);                                    // la araña "sale" de su marco

        if (audio != null) {
            audio.stopMusic(900);                                    // silencio dramático: solo se oye el hilo
            audio.play(SoundEffect.SPIDER_WEB_DROP);
        }

        FadeTransition darken = new FadeTransition(Duration.millis(900), dim);
        darken.setToValue(0.45);
        double rest = restR - SH / 2;
        TranslateTransition drop = new TranslateTransition(Duration.millis(1000), holder);
        drop.setToY(rest);
        drop.setInterpolator(Interpolator.SPLINE(0.35, 0.0, 0.25, 1.0));
        TranslateTransition stretch = new TranslateTransition(Duration.millis(130), holder);
        stretch.setToY(rest + 22);                                   // el hilo se estira al frenar...
        stretch.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition recoil = new TranslateTransition(Duration.millis(220), holder);
        recoil.setToY(rest);                                         // ...y la devuelve
        recoil.setInterpolator(Interpolator.EASE_BOTH);

        SequentialTransition descent = new SequentialTransition(drop, stretch, recoil, new PauseTransition(Duration.millis(200)));
        descent.setOnFinished(e -> bite());
        darken.play();
        descent.play();
    }

    // ------------------------------------------------------------------ 2. mordisco + infección

    private void bite() {
        ScaleTransition windUp = new ScaleTransition(Duration.millis(150), holder);   // toma impulso: se encoge
        windUp.setToX(0.86); windUp.setToY(0.9);
        windUp.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition lunge = new ScaleTransition(Duration.millis(90), holder);      // ¡ÑAC!
        lunge.setToX(1.6); lunge.setToY(1.6);
        lunge.setInterpolator(Interpolator.EASE_IN);
        lunge.setOnFinished(e -> impact());
        ScaleTransition settle = new ScaleTransition(Duration.millis(240), holder);
        settle.setToX(1); settle.setToY(1);
        settle.setInterpolator(Interpolator.EASE_OUT);
        new SequentialTransition(windUp, lunge, settle).play();
    }

    private void impact() {
        if (audio != null) audio.play(SoundEffect.SPIDER_BITE);
        spider.glitchFor(900);
        ScreenShake.shake(shakeTarget, 16, 700);                     // camera shake del Pane del easter egg
        ScreenShake.shake(dim, 10, 500);
        flash.setOpacity(0.75);
        FadeTransition ff = new FadeTransition(Duration.millis(200), flash);
        ff.setToValue(0);
        ff.play();
        for (int s : new int[]{-1, 1}) {                             // las dos marcas de colmillo
            Ellipse fang = new Ellipse(biteX + s * 16, biteY + 8, 5, 8);
            fang.setFill(Color.web("#0a1f0f"));
            fang.setStroke(VENOM[0]);
            fang.setStrokeWidth(2);
            fangs.add(fang);
            stage.getChildren().add(stage.getChildren().indexOf(pendulum), fang);
        }
        spreadInfection();
    }

    /**
     * Cada celda de 12 px recibe un instante de contagio = distancia al mordisco, deformada por "venas" angulares
     * (unas direcciones avanzan más rápido) y algo de ruido: el frente crece como una mancha orgánica, no como un
     * círculo. Se ordenan por tiempo y un AnimationTimer pinta las que ya tocan.
     */
    private void spreadInfection() {
        int cols = (int) Math.ceil(W / CELL), rows = (int) Math.ceil(H / CELL), n = cols * rows;
        double maxD = Math.hypot(Math.max(biteX, W - biteX), Math.max(biteY, H - biteY));
        double ph1 = rnd.nextDouble() * 6.28, ph2 = rnd.nextDouble() * 6.28;
        double[] at = new double[n];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            double cx = (i % cols) * CELL + CELL / 2.0 - biteX, cy = (i / cols) * CELL + CELL / 2.0 - biteY;
            double ang = Math.atan2(cy, cx), d = Math.hypot(cx, cy) / maxD;
            double veins = 0.5 + 0.5 * Math.sin(ang * 7 + ph1) * Math.sin(ang * 3 + ph2);
            at[i] = d * (1.05 + 0.55 * (1 - veins)) + rnd.nextDouble() * 0.12;
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(at[a], at[b]));
        double total = at[order[n - 1]];

        GraphicsContext g = infection.getGraphicsContext2D();
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        List<double[]> fresh = new ArrayList<>();                    // {x, y, nacimiento}: el frente brilla y se pudre
        long[] t0 = {0};
        int[] next = {0};
        new AnimationTimer() {
            @Override public void handle(long now) {
                if (t0[0] == 0) t0[0] = now;
                double s = (now - t0[0]) / 1e9;
                fresh.removeIf(c -> {
                    if (s - c[2] < 0.28) return false;
                    g.setFill(ROT);
                    g.fillRect(c[0], c[1], CELL, CELL);
                    if (rnd.nextDouble() < 0.18) { g.setFill(Color.rgb(57, 255, 20, 0.35)); g.fillText(glyph(), c[0] + 2, c[1] + 10); }
                    return true;
                });
                while (next[0] < n && at[order[next[0]]] <= s) {
                    int i = order[next[0]++];
                    double x = (i % cols) * CELL, y = (i / cols) * CELL;
                    g.setFill(VENOM[rnd.nextInt(VENOM.length)]);
                    g.fillRect(x, y, CELL, CELL);
                    fresh.add(new double[]{x, y, s});
                }
                for (int k = 0; k < 3; k++) {                        // glitch: barras horizontales
                    double y = rnd.nextDouble() * H, w = 40 + rnd.nextDouble() * 260;
                    double x = biteX + (rnd.nextDouble() - 0.5) * W * Math.min(1, s / total * 1.4);
                    g.setFill(rnd.nextBoolean() ? Color.rgb(57, 255, 20, 0.5) : Color.rgb(0, 0, 0, 0.6));
                    g.fillRect(x - w / 2, y, w, 2 + rnd.nextInt(5));
                }
                if (next[0] >= n && fresh.isEmpty()) {
                    stop();
                    covered(g);
                }
            }
        }.start();
    }

    private String glyph() {
        String set = "01ABCDEF#$%&?";
        return String.valueOf(set.charAt(rnd.nextInt(set.length())));
    }

    // ------------------------------------------------------------------ 3. pantalla cubierta

    private void covered(GraphicsContext g) {
        g.setFill(ROT);
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < 260; i++) {
            g.setFill(Color.rgb(57, 255, 20, 0.15 + rnd.nextDouble() * 0.45));
            g.fillText(glyph(), rnd.nextDouble() * W, rnd.nextDouble() * H);
        }
        dim.setOpacity(0);
        stage.getChildren().removeAll(fangs);
        onCovered.run();                                             // el modo se cierra DEBAJO: nadie ve el corte
        PauseTransition hold = new PauseTransition(Duration.millis(250));
        hold.setOnFinished(e -> swingHome(g));
        hold.play();
    }

    // ------------------------------------------------------------------ 4-5. péndulo y aterrizaje

    private void swingHome(GraphicsContext g) {
        // Destino: el centro del sprite real, llevado a las coordenadas de esta capa (misma escala: ambas viven en MainView)
        Point2D tl = stage.sceneToLocal(homeSpider.localToScene(0, 0));
        Point2D target = tl.add(SW / 2, SH / 2);
        double dx = target.getX() - anchor.getX(), dy = target.getY() - anchor.getY();
        double phi = -Math.toDegrees(Math.atan2(dx, dy));            // ángulo del ancla al marco (negativo = derecha)
        double radius = Math.hypot(dx, dy);                          // longitud final del hilo

        // Fase libre: θ(t) = θ₀·e^(−γt)·sen(ωt), muestreada a 120 Hz en un Timeline sobre el ángulo del Rotate.
        double omega = Math.sqrt(G_SCREEN / restR);
        double tFree = Math.PI / omega;                              // medio periodo: vuelve a pasar por abajo
        Timeline free = new Timeline();
        for (double t = 0; t <= tFree + 1e-9; t += 1.0 / 120) {
            double theta = THETA0 * Math.exp(-GAMMA * t) * Math.sin(omega * t);
            free.getKeyFrames().add(new KeyFrame(Duration.seconds(t), new KeyValue(swing.angleProperty(), theta, Interpolator.LINEAR)));
        }
        // Velocidad angular (°/s) al salir de la fase libre y duración del arco final que la empalma sin saltos.
        double v0 = -THETA0 * omega * Math.exp(-GAMMA * tFree);
        double dArc = Math.max(0.7, Math.min(1.5, phi * Math.PI / (2 * v0)));

        // Arco final: θ(u) = φ·sen(πu/2) → velocidad máxima al salir, CERO al llegar (ápice = aterrizaje sin rebote).
        Interpolator quarterSine = new Interpolator() {
            @Override protected double curve(double u) { return Math.sin(u * Math.PI / 2); }
        };
        Timeline arc = new Timeline(new KeyFrame(Duration.seconds(dArc), new KeyValue(swing.angleProperty(), phi, quarterSine)));
        // el hilo se alarga a lo largo del arco hasta R (la araña se desliza por el hilo)
        TranslateTransition reel = new TranslateTransition(Duration.seconds(dArc), holder);
        reel.setToY(radius - SH / 2);
        reel.setInterpolator(Interpolator.EASE_BOTH);
        // se endereza en el último 60 % del arco: contra-rotación −θ·k alrededor de su centro
        Timeline straighten = new Timeline(
                new KeyFrame(Duration.seconds(dArc * 0.4), new KeyValue(uprightK, 0)),
                new KeyFrame(Duration.seconds(dArc), new KeyValue(uprightK, 1, Interpolator.EASE_BOTH)));

        SequentialTransition flight = new SequentialTransition(free, new ParallelTransition(arc, reel, straighten));
        flight.setOnFinished(e -> land());
        if (audio != null) audio.play(SoundEffect.SPIDER_WEB_DROP, 1.35);
        flight.play();
        dissolveInfection(g, tFree + dArc * 0.85);                   // la UI reaparece mientras sube por el arco
    }

    /** La infección se deshace celda a celda en orden aleatorio durante {@code seconds}. */
    private void dissolveInfection(GraphicsContext g, double seconds) {
        int cols = (int) Math.ceil(W / CELL), rows = (int) Math.ceil(H / CELL), n = cols * rows;
        List<Integer> cells = new ArrayList<>(n);
        for (int i = 0; i < n; i++) cells.add(i);
        Collections.shuffle(cells, rnd);
        long[] t0 = {0};
        int[] done = {0};
        new AnimationTimer() {
            @Override public void handle(long now) {
                if (t0[0] == 0) t0[0] = now;
                double s = (now - t0[0]) / 1e9;
                int target = (int) Math.min(n, n * s / seconds);
                for (; done[0] < target; done[0]++) {
                    int i = cells.get(done[0]);
                    g.clearRect((i % cols) * CELL, (i / cols) * CELL, CELL, CELL);
                }
                if (done[0] >= n) stop();
            }
        }.start();
    }

    /** Aterrizó: el hilo se corta, vuelve la araña real (idéntica, mismo píxel) y se desmonta todo. */
    private void land() {
        FadeTransition cut = new FadeTransition(Duration.millis(160), thread);
        cut.setToValue(0);
        cut.setOnFinished(e -> {
            homeSpider.setOpacity(1);
            ScaleTransition squash = new ScaleTransition(Duration.millis(220), homeSpider);   // pequeño "plop" al caer
            squash.setFromX(1.06); squash.setFromY(0.92);
            squash.setToX(1); squash.setToY(1);
            squash.play();
            homeSpider.glitchFor(250);
            upright.angleProperty().unbind();
            thread.endYProperty().unbind();
            spider.dispose();
            host.getChildren().remove(stage);
            shakeTarget.setTranslateX(0);
            shakeTarget.setTranslateY(0);
            onDone.run();
        });
        cut.play();
    }
}
