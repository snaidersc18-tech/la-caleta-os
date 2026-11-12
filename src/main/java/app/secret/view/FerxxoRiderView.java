package app.secret.view;

import app.secret.SecretContext;
import app.secret.audio.SoundEffect;
import app.secret.ui.FeidSprite;
import app.secret.ui.FerxxoRiderArt;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static app.secret.ui.FerxxoRiderArt.*;

/**
 * "FERXXO RIDER": el mini-juego oculto del modo FEID. Un endless runner: Feid avanza solo en su moto por una calle
 * de Medellín de noche y hay que saltar (el "pique") vallas, conos y huecos.
 * <ul>
 *   <li><b>Controles</b>: ESPACIO o ↑ = pique/salto (mantener = salto más alto), R = volver a rodar tras caerse.</li>
 *   <li><b>Puntuación</b>: kilómetros recorridos arriba a la derecha (y el récord de la sesión).</li>
 *   <li><b>Consola viva</b>: abajo, transparente. {@code CALLE} + Enter corta el juego y la música en el acto y
 *       devuelve al Ministerio con el péndulo de El Guachimán.</li>
 * </ul>
 *
 * <h3>Game loop</h3>
 * Un único {@link AnimationTimer} con dt real (tope 50 ms): física de gravedad y velocidad en Y (Euler semi-implícito),
 * salto variable (soltar la tecla corta la subida), "buffer" de salto de 120 ms (se acepta un toque justo antes de
 * aterrizar) y obstáculos generados por DISTANCIA (el hueco mínimo crece con la velocidad: siempre da tiempo a
 * reaccionar). Colisiones con {@code getBoundsInParent().intersects(...)} entre cajas de choque invisibles, más
 * pequeñas que los sprites (perdonan los píxeles de las esquinas).
 * <ul>
 *   <li><b>Dificultad progresiva</b>: la velocidad es {@code SPEED0 × 1.07^(km / 0.5)} (tope x3.2) y mueve a la vez el
 *       asfalto, el fondo y los obstáculos; la gravedad y el impulso del salto escalan con ella (mismo alto, menos
 *       tiempo en el aire). Cada nivel muestra "¡MÁS DURO, MOR!".</li>
 *   <li><b>El Cafexito</b>: vaso de tinto que flota sobre algunos obstáculos, a la altura de la cima de un pique. Al
 *       agarrarlo suena {@code playAt(CAFEXITO)} posicionado en estéreo y Feid queda 3 s invencible (parpadea en verde
 *       neón y revienta lo que toque) con los km contando doble.</li>
 *   <li><b>Fondo vivo</b>: parallax de 3 capas (cielo y montañas del valle de Aburrá · edificios con viaducto,
 *       letreros de neón que zumban/parpadean y el Metro que cruza de vez en cuando · asfalto a toda velocidad).</li>
 * </ul>
 * Rendimiento: las capas son imágenes 320x180 x4, dos copias por capa que se trasladan, sin redibujar; la moto
 * cambia entre 4 imágenes precalculadas, los obstáculos son ImageView de imágenes compartidas y solo el polvo/humo usa
 * un Canvas.
 */
public final class FerxxoRiderView extends TakeoverView {

    // ---- física (px, s)
    private static final double PX = 3;
    private static final double GROUND = ROAD_Y * 4;                  // fila de la calle donde apoyan las ruedas
    private static final double PLAYER_X = 170;
    private static final double GRAVITY = 2900, JUMP_V = -1060, CUT_V = -430, BUFFER_S = 0.12;
    private static final double SPEED0 = 520, IDLE_SPEED = 120;
    private static final double PX_PER_KM = 10_000;                   // 10 px = 1 m
    /** Lejano (montañas y cielo), medio (edificios, neón, Metro) y cercano (asfalto). */
    private static final double[] PARALLAX = {0.06, 0.28, 1.0};

    // ---- dificultad progresiva: la velocidad se MULTIPLICA con los km (compuesto, como un interés)
    /** Cada medio kilómetro la velocidad objetivo se multiplica por 1.07 (x1.97 a los 5 km, tope x3.2 hacia 8.6 km). */
    private static final double LEVEL_KM = 0.5, LEVEL_MULT = 1.07, MULT_MAX = 3.2;

    // ---- power-up "El Cafexito"
    private static final double POWER_S = 3.0;
    /** Altura (px sobre la calle) del centro del vaso: se agarra en la cima de un pique bien medido. */
    private static final double CAFE_LIFT = 300;

    // ---- el Metro que cruza el viaducto de fondo
    private static final double METRO_V = 320;
    private static final Color GREEN = Color.web("#00ff66");
    private static final String GREEN_CSS = "#00ff66";

    private static double best;                                        // récord de la sesión

    private enum State { READY, RUNNING, CRASHED, LEAVING }
    private enum Kind { CONE, BARRIER, POTHOLE }

    private static final class Obstacle {
        final Node view; final Rectangle hit; double x; final double w, hx;
        Obstacle(Node view, Rectangle hit, double x, double w, double hx) { this.view = view; this.hit = hit; this.x = x; this.w = w; this.hx = hx; }
    }

    private static final class Pickup {
        final ImageView view; final Rectangle hit; double x, t;
        Pickup(ImageView view, Rectangle hit, double x) { this.view = view; this.hit = hit; this.x = x; }
    }

    private final SecretContext ctx;
    private final Random rnd = new Random();
    private final Pane world = new Pane();
    private final ImageView[][] layers = new ImageView[3][2];
    private final ImageView[] neonA = new ImageView[2], neonB = new ImageView[2];
    private final ImageView metro = new ImageView();
    private final Image[] cafeFrames = {image(cafexito(0), CAFE_PALETTE), image(cafexito(1), CAFE_PALETTE)};
    private final List<Pickup> pickups = new ArrayList<>();
    private final Label speedLbl = new Label("VEL x1.00");
    private final Label powerLbl = new Label();
    private final Label pop = new Label();
    private final DropShadow neonGlow = new DropShadow(30, GREEN);
    private final DropShadow tintGlow = new DropShadow(34, GREEN);
    private final Pane track = new Pane();                             // obstáculos + cajas de choque (mismo padre)
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Image[] bikeFrames = new Image[FRAMES];
    private final Image[] riderFrames = new Image[2];
    private final ImageView bikeView = new ImageView(), riderView = new ImageView();
    private final Group bike = new Group(bikeView, riderView);
    private final Rotate wheelie = new Rotate(0, REAR_X * PX, CONTACT_Y * PX);
    private final Rotate riderSpin = new Rotate(0, HIP_X * PX, HIP_Y * PX);
    private final Rectangle playerHit = new Rectangle(42 * PX, 46 * PX);
    private final Image coneImg = image(cone(), OBST_PALETTE), barrierImg = image(barrier(), OBST_PALETTE),
            holeImg = image(pothole(), OBST_PALETTE);
    private final Canvas dust = new Canvas(W, H);
    private final List<double[]> particles = new ArrayList<>();      // {x, y, vx, vy, edad, vida, tamaño, verde?}

    private final Label km = new Label("0.00 KM");
    private final Label record = new Label();
    private final VBox ready = new VBox(6);
    private final VBox crashBox = new VBox(8);
    private final Label crashSub = new Label();
    private final TextField console = new TextField();
    private final Label consoleMsg = new Label("escribe CALLE + Enter pa' volver al Ministerio");

    private AnimationTimer loop;
    private State state = State.READY;
    private double scroll, speed = SPEED0, distance, height, vy, spawnIn = 900, buffer, crashT, shake;
    private double riderX, riderY, riderVX, riderVY;
    // dificultad y física escalada con la velocidad
    private double traveled, gravity = GRAVITY, jumpV = JUMP_V, cutV = CUT_V;
    private int level;
    // power-up
    private double powerT, cafeIn = 3500;
    // fondo vivo
    private double clock, lastScroll, flickerA, metroX, metroDir, metroIn = 4, popT;
    private boolean metroOn;
    private boolean jumpHeld, swallowR, grounded = true;
    private int frame = -1;
    private String kmShown = "";

    public FerxxoRiderView(SecretContext ctx, Runnable onExit) {
        this.ctx = ctx;
        setStyle("-fx-background-color: #020509;");
        setClip(new Rectangle(W, H));

        // ---- fondo parallax multicapa: dos copias por capa (las imágenes son periódicas en 320 px)
        //   lejana: cielo + montañas del valle de Aburrá · media: edificios + viaducto, letreros de neón y el Metro ·
        //   cercana: muro, andén y asfalto (va a la velocidad de los obstáculos)
        Image[] art = {FerxxoRiderArt.sky(), FerxxoRiderArt.city(), FerxxoRiderArt.street()};
        for (int l = 0; l < 3; l++) {
            if (l == 2) {                                              // entre la capa media y la calle:
                addPair(neonA, FerxxoRiderArt.neonA());                // letreros que zumban
                addPair(neonB, FerxxoRiderArt.neonB());                // letreros que parpadean
                metro.setImage(FerxxoRiderArt.metro());                // y el Metro sobre el viaducto
                metro.setFitWidth(METRO_W * 4); metro.setFitHeight(METRO_H * 4);
                metro.setSmooth(false);
                metro.setLayoutY((VIADUCT_Y - METRO_H) * 4);
                metro.setVisible(false);
                world.getChildren().add(metro);
            }
            for (int k = 0; k < 2; k++) {
                ImageView v = new ImageView(art[l]);
                v.setFitWidth(W); v.setFitHeight(H);
                v.setSmooth(false);
                layers[l][k] = v;
                world.getChildren().add(v);
            }
        }
        // parpadeo verde neón de la invencibilidad: tinte SRC_ATOP (solo pinta donde hay sprite) + resplandor
        javafx.scene.effect.Blend tint = new javafx.scene.effect.Blend(javafx.scene.effect.BlendMode.SRC_ATOP, null,
                new javafx.scene.effect.ColorInput(-60, -60, RW * PX + 120, RH * PX + 120, Color.color(0, 1, 0.4, 0.6)));
        tintGlow.setInput(tint);
        tintGlow.setSpread(0.25);

        // ---- moto + piloto
        for (int i = 0; i < FRAMES; i++) bikeFrames[i] = image(FerxxoRiderArt.bike(i), FeidSprite.PALETTE);
        for (int i = 0; i < 2; i++) riderFrames[i] = image(FerxxoRiderArt.rider(i), FeidSprite.PALETTE);
        for (ImageView v : new ImageView[]{bikeView, riderView}) {
            v.setFitWidth(RW * PX); v.setFitHeight(RH * PX); v.setSmooth(false);
        }
        bikeView.setImage(bikeFrames[0]);
        riderView.setImage(riderFrames[0]);
        riderView.getTransforms().add(riderSpin);
        bike.getTransforms().add(wheelie);
        bike.setLayoutX(PLAYER_X);

        playerHit.setVisible(false);                                   // invisible: sus límites cuentan igual
        playerHit.setMouseTransparent(true);
        track.getChildren().add(playerHit);
        track.setMouseTransparent(true);
        dust.setMouseTransparent(true);
        world.getChildren().addAll(track, dust, bike);
        world.setMouseTransparent(true);

        StackPane hud = new StackPane(title(), scoreBox(), popLabel(), readyBox(), crashBox(), consoleBar());
        hud.setPickOnBounds(false);
        getChildren().addAll(world, hud);

        installKeys();
        setOnMousePressed(e -> console.requestFocus());
        onDispose(() -> { if (loop != null) loop.stop(); });
        placeBike();
        layoutParallax();
    }

    @Override public void onShown() {
        loop = new AnimationTimer() {
            private long last;
            @Override public void handle(long now) {
                double dt = last == 0 ? 0 : Math.min(0.05, (now - last) / 1e9);
                last = now;
                tick(dt);
            }
        };
        loop.start();
        Platform.runLater(console::requestFocus);                      // BaseMode enfoca la vista justo después
    }

    // ================================================================== HUD

    private Node title() {
        Text t = new Text("FERXXO RIDER");
        t.setFont(Font.font("Impact", FontWeight.BLACK, 58));
        t.setFill(GREEN);
        t.setStroke(Color.web("#003d18"));
        t.setStrokeWidth(2);
        t.setEffect(new DropShadow(24, GREEN));
        t.setCache(true);
        t.setCacheHint(CacheHint.SPEED);
        Label how = label("ESPACIO / ↑  pique   ·   R  volver a rodar", 14, "#bfffd9");
        VBox box = new VBox(0, t, how);
        box.setMouseTransparent(true);
        box.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(box, Pos.TOP_LEFT);
        StackPane.setMargin(box, new Insets(16, 0, 0, 26));
        return box;
    }

    private Node scoreBox() {
        Label cap = label("KILÓMETROS RECORRIDOS", 13, "#9effc6");
        style(km, "Impact", 52, false, GREEN_CSS);
        km.setEffect(new DropShadow(22, GREEN));
        style(record, "Monospaced", 14, true, "#eafff2");
        record.setText(String.format(Locale.ROOT, "RÉCORD %.2f KM", best));
        record.setEffect(new DropShadow(5, Color.BLACK));            // legible aunque pase la luna por detrás
        cap.setEffect(new DropShadow(5, Color.BLACK));
        style(speedLbl, "Monospaced", 15, true, "#ffd400");
        speedLbl.setEffect(new DropShadow(5, Color.BLACK));
        style(powerLbl, "Monospaced", 16, true, GREEN_CSS);
        powerLbl.setEffect(new DropShadow(12, GREEN));
        powerLbl.setVisible(false);
        VBox box = new VBox(0, cap, km, record, speedLbl, powerLbl);
        box.setAlignment(Pos.TOP_RIGHT);
        box.setMouseTransparent(true);
        box.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(box, Pos.TOP_RIGHT);
        StackPane.setMargin(box, new Insets(14, 28, 0, 0));
        return box;
    }

    /** Aviso grande y breve en el centro ("¡MÁS DURO, MOR!", "¡CAFEXITO!"). */
    private Node popLabel() {
        style(pop, "Impact", 46, false, "#ffd400");
        pop.setEffect(new DropShadow(20, Color.web("#ffd400")));
        pop.setMouseTransparent(true);
        pop.setOpacity(0);
        StackPane.setAlignment(pop, Pos.TOP_CENTER);
        StackPane.setMargin(pop, new Insets(190, 0, 0, 0));
        return pop;
    }

    private void popup(String text, String color) {
        if (Boolean.getBoolean("rebusque.debug"))
            System.err.printf(Locale.ROOT, "[rider] %s · %.2f km · vel %.0f px/s · g %.0f%n", text, distance / PX_PER_KM, speed, gravity);
        pop.setText(text);
        style(pop, "Impact", 46, false, color);
        pop.setEffect(new DropShadow(20, Color.web(color)));
        popT = 1.3;
    }

    private Node readyBox() {
        Label a = new Label("PRESIONA ESPACIO PA' RODAR");
        style(a, "Impact", 40, false, GREEN_CSS);
        a.setEffect(new DropShadow(18, GREEN));
        Label b = label("salta vallas, conos y huecos · agarra EL CAFEXITO en el aire = invencible + km x2", 15, "#eafff2");
        ready.getChildren().addAll(a, b);
        ready.setAlignment(Pos.CENTER);
        ready.setMouseTransparent(true);
        ready.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(ready, Pos.CENTER);
        StackPane.setMargin(ready, new Insets(0, 0, 160, 0));
        return ready;
    }

    private Node crashBox() {
        Text paila = new Text("¡Paila mor, te caíste!");
        paila.setFont(Font.font("Impact", FontWeight.BLACK, 76));
        paila.setFill(Color.web("#eafff2"));
        paila.setStroke(GREEN);
        paila.setStrokeWidth(3);
        paila.setEffect(new DropShadow(30, GREEN));
        paila.setCache(true);
        style(crashSub, "Monospaced", 18, true, GREEN_CSS);
        crashBox.getChildren().addAll(paila, crashSub);
        crashBox.setAlignment(Pos.CENTER);
        crashBox.setMouseTransparent(true);
        crashBox.setVisible(false);
        crashBox.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-padding: 22 46 22 46;"
                + " -fx-border-color: " + GREEN_CSS + "; -fx-border-width: 0 0 0 6;");
        crashBox.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(crashBox, Pos.CENTER);
        StackPane.setMargin(crashBox, new Insets(0, 0, 140, 0));
        return crashBox;
    }

    /** La mini-consola transparente: prompt + campo + respuesta. Solo entiende CALLE (Enter). */
    private Node consoleBar() {
        Label prompt = label("rebusque@medallo:~$", 15, GREEN_CSS);
        console.setPromptText("CALLE");
        console.setPrefColumnCount(14);
        console.setStyle("-fx-background-color: transparent; -fx-text-fill: #eafff2; -fx-prompt-text-fill: #2c7a4b;"
                + " -fx-font-family: Monospaced; -fx-font-size: 16; -fx-padding: 2 6 2 6; -fx-highlight-fill: #00ff66;"
                + " -fx-highlight-text-fill: black;");
        console.setOnAction(e -> submit());
        style(consoleMsg, "Monospaced", 13, false, "#9effc6");
        consoleMsg.setOpacity(0.85);
        HBox.setHgrow(consoleMsg, Priority.ALWAYS);
        HBox bar = new HBox(8, prompt, console, consoleMsg);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: rgba(2,8,5,0.45); -fx-border-color: rgba(0,255,102,0.55);"
                + " -fx-border-width: 1 0 0 0; -fx-padding: 6 14 6 14;");
        bar.setMaxSize(W - 40, HBox.USE_PREF_SIZE);
        StackPane.setAlignment(bar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(bar, new Insets(0, 0, 12, 0));
        return bar;
    }

    private static Label label(String text, double size, String color) {
        Label l = new Label(text);
        style(l, "Monospaced", size, true, color);
        return l;
    }

    /** Estilo en línea: la hoja arana.css fija -fx-text-fill en .label y le ganaría a setTextFill(). */
    private static void style(Label l, String family, double size, boolean bold, String color) {
        l.setStyle("-fx-font-family: '" + family + "'; -fx-font-size: " + size + "px; -fx-font-weight: "
                + (bold ? "bold" : "normal") + "; -fx-text-fill: " + color + ";");
    }

    // ================================================================== teclado

    /**
     * Filtros en la vista (corren antes que el campo de la consola): ESPACIO y ↑ nunca se escriben en la consola, son
     * el pique; R tras caerse reinicia y su letra se descarta. Todo lo demás se escribe en la consola.
     */
    private void installKeys() {
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.SPACE || c == KeyCode.UP) {
                e.consume();
                if (!jumpHeld) { jumpHeld = true; pressJump(); }
            } else if (c == KeyCode.R && state == State.CRASHED && crashT > 0.35) {
                e.consume();
                swallowR = true;
                restart();
            }
        });
        addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP) { e.consume(); jumpHeld = false; }
        });
        addEventFilter(KeyEvent.KEY_TYPED, e -> {
            String ch = e.getCharacter();
            if (" ".equals(ch)) e.consume();
            else if (swallowR && "r".equalsIgnoreCase(ch)) { swallowR = false; e.consume(); }
        });
    }

    private void submit() {
        String cmd = console.getText().trim().toUpperCase(Locale.ROOT);
        console.clear();
        if (cmd.isEmpty() || state == State.LEAVING) return;
        if (cmd.equals("CALLE")) {
            state = State.LEAVING;
            if (loop != null) loop.stop();                             // el juego se congela en el acto
            ctx.audio().stopAll();                                     // y la música se corta ya, sin esperar al mordisco
            consoleMsg.setText("pa' la calle, mor...");
            console.setEditable(false);
            ctx.requestStreet();                                       // El Mordisco + péndulo → Ministerio
        } else {
            consoleMsg.setText("\"" + cmd + "\"? ni idea, parce. Aquí solo sirve CALLE.");
        }
    }

    private void pressJump() {
        switch (state) {
            case READY -> { state = State.RUNNING; ready.setVisible(false); speed = SPEED0; }
            case RUNNING -> buffer = BUFFER_S;                         // se ejecuta en tick (también justo antes de caer)
            default -> { }
        }
    }

    // ================================================================== game loop

    private void tick(double dt) {
        switch (state) {
            case READY -> {
                scroll += IDLE_SPEED * dt;
                ready.setOpacity(0.55 + 0.45 * Math.abs(Math.sin(scroll / IDLE_SPEED * 3)));
                riderView.setImage(riderFrames[(int) (scroll / 9) % 2]);
                dustTrail(dt, 10);
            }
            case RUNNING -> run(dt);
            case CRASHED -> crashAnim(dt);
            default -> { }
        }
        clock += dt;
        cityLife(dt);
        if (popT > 0) {
            popT -= dt;
            pop.setOpacity(Math.min(1, popT / 0.35));
            double s = 1 + 0.25 * Math.max(0, popT - 1.0) / 0.3;
            pop.setScaleX(s); pop.setScaleY(s);
        }
        layoutParallax();
        wheels();
        stepParticles(dt);
        if (shake > 0) {
            shake = Math.max(0, shake - dt * 40);
            world.setTranslateX((rnd.nextDouble() - 0.5) * 2 * shake);
            world.setTranslateY((rnd.nextDouble() - 0.5) * 2 * shake);
        }
    }

    private void run(double dt) {
        // ---- DIFICULTAD PROGRESIVA: velocidad = SPEED0 × multiplicador compuesto por los km recorridos.
        // La misma 'speed' mueve el asfalto, las capas del fondo (por su factor parallax) y los obstáculos.
        double realKm = traveled / PX_PER_KM;
        double mult = Math.min(MULT_MAX, Math.pow(LEVEL_MULT, realKm / LEVEL_KM));
        speed += (SPEED0 * mult - speed) * Math.min(1, dt * 3);       // sin tirones al cambiar de nivel
        // La física del salto se acelera con √ de la velocidad: misma altura de pique, pero dura menos en el aire
        // (g·f² y v·f mantienen h = v²/2g): a más km, menos margen y más reflejos.
        double f = Math.pow(speed / SPEED0, 0.35);
        gravity = GRAVITY * f * f; jumpV = JUMP_V * f; cutV = CUT_V * f;

        double dx = speed * dt;
        scroll += dx;
        traveled += dx;
        distance += powerT > 0 ? dx * 2 : dx;                          // Cafexito: los km cuentan doble
        int lv = (int) (traveled / PX_PER_KM / LEVEL_KM);              // nivel = medio km REAL recorrido
        if (lv > level) {
            level = lv;
            popup(String.format(Locale.ROOT, "¡MÁS DURO, MOR!  x%.2f", Math.min(MULT_MAX, Math.pow(LEVEL_MULT, lv))), "#ffd400");
        }
        if (powerT > 0) {
            powerT -= dt;
            if (powerT <= 0) endPower();
            else {
                // Feid parpadea en verde neón (más rápido en el último segundo: "se acaba el tinto")
                double rate = powerT < 1 ? 16 : 9;
                bike.setEffect(((int) (powerT * rate)) % 2 == 0 ? tintGlow : neonGlow);
                powerLbl.setText(String.format(Locale.ROOT, "CAFEXITO %.1fs · KM x2", powerT));
            }
        }
        String vel = String.format(Locale.ROOT, "VEL x%.2f · NIVEL %d", speed / SPEED0, level + 1);
        if (!vel.equals(speedLbl.getText())) speedLbl.setText(vel);

        // ---- salto: buffer de 120 ms; solo desde el piso
        if (buffer > 0) {
            buffer -= dt;
            if (grounded) {
                buffer = 0;
                grounded = false;
                vy = jumpV;
                ctx.audio().play(SoundEffect.MOTO_REV, 0.94 + rnd.nextDouble() * 0.12);
                burst(PLAYER_X + REAR_X * PX, GROUND, 16, true);
            }
        }
        // ---- gravedad + salto variable (Euler semi-implícito: primero la velocidad, luego la posición)
        if (!grounded) {
            if (!jumpHeld && vy < cutV) vy = cutV;                     // soltar la tecla corta la subida
            vy += gravity * dt;
            height -= vy * dt;
            if (height <= 0) {
                height = 0; vy = 0; grounded = true;
                shake = Math.max(shake, 3);
                burst(PLAYER_X + 30 * PX, GROUND, 14, false);
            }
            double target = Math.max(-22, Math.min(9, vy * 0.017));    // pique: la delantera arriba al subir
            wheelie.setAngle(wheelie.getAngle() + (target - wheelie.getAngle()) * Math.min(1, dt * 14));
        } else {
            wheelie.setAngle(wheelie.getAngle() * Math.max(0, 1 - dt * 18));
            riderView.setImage(riderFrames[(int) (distance / 14) % 2]);
            dustTrail(dt, 30 + speed / 20);
        }
        placeBike();

        // ---- obstáculos: generar por distancia, mover, retirar
        spawnIn -= dx;
        cafeIn -= dx;
        if (spawnIn <= 0) spawn();
        for (Iterator<Obstacle> it = obstacles.iterator(); it.hasNext(); ) {
            Obstacle o = it.next();
            o.x -= dx;
            o.view.setLayoutX(Math.round(o.x));
            o.hit.setLayoutX(o.x + o.hx);
            if (o.x + o.w < -40) { track.getChildren().removeAll(o.view, o.hit); it.remove(); }
        }
        // ---- El Cafexito: flota, gira el vapor, se recoge al tocarlo
        for (Iterator<Pickup> it = pickups.iterator(); it.hasNext(); ) {
            Pickup c = it.next();
            c.x -= dx;
            c.t += dt;
            double bobY = Math.sin(c.t * 5) * 8;
            c.view.setLayoutX(Math.round(c.x));
            c.view.setTranslateY(bobY);
            c.hit.setLayoutX(c.x);
            c.hit.setTranslateY(bobY);
            c.view.setImage(cafeFrames[(int) (c.t * 4) % 2]);
            if (playerHit.getBoundsInParent().intersects(c.hit.getBoundsInParent())) {
                track.getChildren().removeAll(c.view, c.hit);
                it.remove();
                collect(c);
            } else if (c.x < -80) {
                track.getChildren().removeAll(c.view, c.hit);
                it.remove();
            }
        }
        // ---- colisiones (cajas invisibles en el mismo padre). Con Cafexito, Feid las atraviesa y las revienta.
        for (Iterator<Obstacle> it = obstacles.iterator(); it.hasNext(); ) {
            Obstacle o = it.next();
            if (!playerHit.getBoundsInParent().intersects(o.hit.getBoundsInParent())) continue;
            if (powerT > 0) {
                track.getChildren().removeAll(o.view, o.hit);
                it.remove();
                shake = Math.max(shake, 7);
                burst(o.x + o.w / 2, GROUND - 20, 26, true);
                ctx.audio().playAt(SoundEffect.MOTO_CRUSH, o.x, W);
            } else { crash(); return; }
        }
        String s = String.format(Locale.ROOT, "%.2f KM", distance / PX_PER_KM);
        if (!s.equals(kmShown)) { kmShown = s; km.setText(s); }
    }

    /** Posiciona la moto y su caja de choque según la altura del salto. */
    private void placeBike() {
        double top = GROUND - height - CONTACT_Y * PX - PX;
        bike.setLayoutY(top);
        playerHit.setX(PLAYER_X + 9 * PX);
        playerHit.setY(top + 16 * PX);
        playerHit.setHeight((CONTACT_Y - 17) * PX);
    }

    /**
     * Un obstáculo nuevo justo fuera de la pantalla. El hueco hasta el siguiente crece con la velocidad (siempre hay
     * tiempo de aterrizar y reaccionar); con velocidad alta aparecen dobles conos.
     */
    private void spawn() {
        double r = rnd.nextDouble();
        Kind k = r < 0.36 ? Kind.CONE : r < 0.7 ? Kind.BARRIER : Kind.POTHOLE;
        add(k, W + 20);
        Obstacle first = obstacles.get(obstacles.size() - 1);
        // dobles conos: más frecuentes cuanto más rápido (~33 % cuando empiezan, hasta 65 %)
        double doubles = Math.min(0.65, 0.4 * (speed / SPEED0 - 1) / 0.5 * 0.5 + 0.15);
        if (k == Kind.CONE && speed > 760 && rnd.nextDouble() < doubles) add(Kind.CONE, W + 20 + 16 * PX + 6);
        spawnIn = speed * 0.85 + 220 + rnd.nextDouble() * speed * 0.9;
        // El Cafexito flota sobre el obstáculo, a la altura de la cima de un pique largo
        if (cafeIn <= 0 && powerT <= 0 && pickups.isEmpty()) {
            addPickup(first.x + first.w / 2 - CAFE_W * PX / 2);
            cafeIn = 6000 + rnd.nextDouble() * 8000;
        }
    }

    private void addPickup(double x) {
        ImageView v = new ImageView(cafeFrames[0]);
        v.setFitWidth(CAFE_W * PX); v.setFitHeight(CAFE_H * PX);
        v.setSmooth(false);
        v.setEffect(new DropShadow(22, GREEN));
        double top = GROUND - CAFE_LIFT - CAFE_H * PX / 2;
        v.setLayoutY(top);
        v.setLayoutX(x);
        Rectangle hit = new Rectangle(0, top + 2 * PX, CAFE_W * PX, (CAFE_H - 4) * PX);
        hit.setVisible(false);
        hit.setLayoutX(x);
        track.getChildren().addAll(v, hit);
        pickups.add(new Pickup(v, hit, x));
    }

    /** ¡Cafexito! Sonido posicionado, chispas verdes, 3 s de invencibilidad y km x2. */
    private void collect(Pickup c) {
        ctx.audio().playAt(SoundEffect.CAFEXITO, c.x, W);
        burst(c.x + CAFE_W * PX / 2, GROUND - CAFE_LIFT, 34, true);
        powerT = POWER_S;
        bike.setEffect(tintGlow);                                      // el parpadeo arranca en este mismo frame
        powerLbl.setVisible(true);
        popup("¡EL CAFEXITO!  INVENCIBLE · KM x2", GREEN_CSS);
    }

    private void endPower() {
        powerT = 0;
        bike.setEffect(null);
        powerLbl.setVisible(false);
    }

    private void add(Kind k, double x) {
        Image img = switch (k) { case CONE -> coneImg; case BARRIER -> barrierImg; case POTHOLE -> holeImg; };
        ImageView v = new ImageView(img);
        double w = img.getWidth() * PX, h = img.getHeight() * PX;
        v.setFitWidth(w); v.setFitHeight(h);
        v.setSmooth(false);
        Rectangle hit;
        double hx;
        switch (k) {
            case CONE -> { v.setLayoutY(GROUND - h + PX); hx = 4 * PX; hit = new Rectangle(0, GROUND - h + 3 * PX, 8 * PX, h - 3 * PX); }
            case BARRIER -> { v.setLayoutY(GROUND - h + PX); hx = 3 * PX; hit = new Rectangle(0, GROUND - h + 2 * PX, 28 * PX, h - 2 * PX); }
            default -> { v.setLayoutY(GROUND - h / 2); hx = 8 * PX; hit = new Rectangle(0, GROUND - 10, 24 * PX, 12); }
        }
        hit.setVisible(false);
        Obstacle o = new Obstacle(v, hit, x, w, hx);
        v.setLayoutX(x);
        hit.setLayoutX(x + hx);
        track.getChildren().add(1, v);                                 // detrás de nada importante; la caja va aparte
        track.getChildren().add(hit);
        obstacles.add(o);
    }

    // ------------------------------------------------------------------ choque y reinicio

    private void crash() {
        state = State.CRASHED;
        crashT = 0;
        endPower();
        shake = 18;
        ctx.audio().play(SoundEffect.MOTO_CRASH);
        burst(PLAYER_X + FRONT_X * PX, GROUND - 30, 40, true);
        riderX = 0; riderY = 0;
        riderVX = 220 + speed * 0.3; riderVY = -760;
        double total = distance / PX_PER_KM;
        boolean newBest = total > best;
        if (newBest) best = total;
        record.setText(String.format(Locale.ROOT, "RÉCORD %.2f KM", best));
        crashSub.setText(String.format(Locale.ROOT, "%.2f KM%s   ·   R pa' volver a rodar", total, newBest ? "  ¡NUEVO RÉCORD!" : ""));
        crashBox.setVisible(true);
    }

    /**
     * El choque: la moto se encabrita contra el obstáculo, rebota hacia atrás y cae sobre las ruedas; el Ferxxo sale
     * volando por encima con una voltereta hacia atrás y queda tendido en la calle (giro final −450° = −90°: la cabeza
     * queda arriba a la izquierda de la cadera, nada se hunde bajo el asfalto).
     */
    private void crashAnim(double dt) {
        crashT += dt;
        double kick = crashT < 0.18 ? -26 * crashT / 0.18 : -26 * Math.max(0, 1 - (crashT - 0.18) / 0.4);
        wheelie.setAngle(kick);
        if (height > 0) { vy += GRAVITY * dt; height = Math.max(0, height - vy * dt); placeBike(); }
        double back = -40 * Math.min(1, crashT / 0.3);
        bike.setTranslateX(back);
        riderVY += GRAVITY * 0.8 * dt;
        riderX += riderVX * dt;
        riderY += riderVY * dt;
        double floor = RIDER_FLOOR;                                       // el piloto aterriza en la calle
        boolean landed = riderY >= floor;
        if (landed) { riderY = floor; riderVX *= Math.max(0, 1 - dt * 7); riderVY = 0; }
        riderView.setTranslateX(riderX - back);                         // el piloto no rebota con la moto
        riderView.setTranslateY(riderY);
        riderSpin.setAngle(landed ? lerp(riderSpin.getAngle(), -450, Math.min(1, dt * 12)) : Math.max(-430, -crashT * 760));
    }

    /** Traslación Y con la que la cadera del piloto tendido queda 15 px sobre la calle (moto apoyada en el piso). */
    private static final double RIDER_FLOOR = (RH - 1) * PX - HIP_Y * PX - 15;

    private void restart() {
        for (Obstacle o : obstacles) track.getChildren().removeAll(o.view, o.hit);
        obstacles.clear();
        for (Pickup c : pickups) track.getChildren().removeAll(c.view, c.hit);
        pickups.clear();
        endPower();
        distance = 0; traveled = 0; level = 0; cafeIn = 3500; popT = 0; pop.setOpacity(0);
        gravity = GRAVITY; jumpV = JUMP_V; cutV = CUT_V;
        speedLbl.setText("VEL x1.00 · NIVEL 1");
        height = 0; vy = 0; speed = SPEED0; spawnIn = 900; buffer = 0;
        grounded = true;
        wheelie.setAngle(0);
        bike.setTranslateX(0);
        riderView.setTranslateX(0); riderView.setTranslateY(0); riderSpin.setAngle(0);
        kmShown = "";
        km.setText("0.00 KM");
        crashBox.setVisible(false);
        placeBike();
        state = State.RUNNING;
    }

    // ------------------------------------------------------------------ fondo, ruedas y partículas

    /** Cada capa se desplaza a su velocidad; dos copias de 1280 px empalman sin costura (las capas son periódicas). */
    private void layoutParallax() {
        for (int l = 0; l < 3; l++) {
            double off = Math.floorMod(Math.round(scroll * PARALLAX[l]), (long) W);
            layers[l][0].setTranslateX(-off);
            layers[l][1].setTranslateX(W - off);
            if (l == 1) {                                              // el neón va pegado a los edificios
                for (ImageView[] pair : new ImageView[][]{neonA, neonB}) {
                    pair[0].setTranslateX(-off);
                    pair[1].setTranslateX(W - off);
                }
            }
        }
    }

    private void addPair(ImageView[] pair, Image img) {
        for (int k = 0; k < 2; k++) {
            ImageView v = new ImageView(img);
            v.setFitWidth(W); v.setFitHeight(H);
            v.setSmooth(false);
            pair[k] = v;
            world.getChildren().add(v);
        }
    }

    /**
     * La ciudad respira aunque el jugador esté quieto o caído: letreros que zumban (caídas breves al azar) y letreros
     * que parpadean on/off con un tartamudeo al encender, y el Metro que cruza el viaducto cada 7–16 s en cualquier
     * sentido (su velocidad en pantalla = la propia ± el arrastre de la capa media).
     */
    private void cityLife(double dt) {
        // ---- neón A: zumbido
        if (flickerA > 0) flickerA -= dt;
        else if (rnd.nextDouble() < dt * 0.9) flickerA = 0.08 + rnd.nextDouble() * 0.18;
        double a = flickerA > 0 ? (rnd.nextDouble() < 0.5 ? 0.15 : 0.6) : 1;
        neonA[0].setOpacity(a); neonA[1].setOpacity(a);
        // ---- neón B: on/off cada 1.6 s, encendido tartamudo
        double ph = clock % 1.6;
        double b = ph < 1.05 ? (ph < 0.12 ? ((int) (ph / 0.03) % 2 == 0 ? 1 : 0.2) : 1) : 0.06;
        neonB[0].setOpacity(b); neonB[1].setOpacity(b);

        // ---- el Metro
        double dScroll = scroll - lastScroll;
        lastScroll = scroll;
        double trainW = METRO_W * 4;
        if (!metroOn) {
            metroIn -= dt;
            if (metroIn <= 0) {
                metroOn = true;
                metroDir = rnd.nextBoolean() ? 1 : -1;
                metroX = metroDir > 0 ? -trainW - 10 : W + 10;
                metro.setScaleX(metroDir > 0 ? -1 : 1);                // los faros blancos van delante
                metro.setVisible(true);
            }
        } else {
            metroX += metroDir * METRO_V * dt - dScroll * PARALLAX[1];
            metro.setLayoutX(Math.round(metroX));
            if (metroX > W + 40 || metroX < -trainW - 40) {
                metroOn = false;
                metro.setVisible(false);
                metroIn = 7 + rnd.nextDouble() * 9;
            }
        }
    }

    private void wheels() {
        int f = (int) Math.floorMod((long) (scroll / 6), FRAMES);
        if (state == State.CRASHED) return;
        if (f != frame) { frame = f; bikeView.setImage(bikeFrames[f]); }
    }

    private double dustAcc;

    /** Polvo que levanta la rueda trasera. */
    private void dustTrail(double dt, double rate) {
        dustAcc += rate * dt;
        while (dustAcc >= 1) {
            dustAcc -= 1;
            particles.add(new double[]{PLAYER_X + REAR_X * PX - 10, GROUND - 4 - rnd.nextDouble() * 8,
                    -160 - rnd.nextDouble() * 220, -40 - rnd.nextDouble() * 60, 0, 0.35 + rnd.nextDouble() * 0.35,
                    5 + rnd.nextDouble() * 8, 0});
        }
    }

    private void burst(double x, double y, int n, boolean green) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI + rnd.nextDouble() * Math.PI, sp = 80 + rnd.nextDouble() * 260;
            particles.add(new double[]{x, y, Math.cos(a) * sp, Math.sin(a) * sp * 0.6, 0,
                    0.4 + rnd.nextDouble() * 0.5, 6 + rnd.nextDouble() * 12, green ? 1 : 0});
        }
    }

    private void stepParticles(double dt) {
        if (particles.isEmpty()) { dust.setVisible(false); return; }
        dust.setVisible(true);
        GraphicsContext g = dust.getGraphicsContext2D();
        g.clearRect(0, 0, W, H);
        for (Iterator<double[]> it = particles.iterator(); it.hasNext(); ) {
            double[] p = it.next();
            p[4] += dt;
            if (p[4] >= p[5]) { it.remove(); continue; }
            double life = p[4] / p[5];
            double drag = Math.exp(-2.6 * dt);
            p[2] *= drag;
            p[3] = p[3] * drag - 30 * dt;
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            double s = p[6] * (1 + life * 1.8);
            g.setGlobalAlpha(Math.pow(1 - life, 1.6) * (p[7] == 1 ? 0.6 : 0.4));
            g.setFill(p[7] == 1 ? GREEN : Color.web("#8a9a90"));
            g.fillOval(p[0] - s / 2, p[1] - s / 2, s, s);
        }
        g.setGlobalAlpha(1);
    }

    private static double lerp(double a, double b, double u) { return a + (b - a) * u; }
}
