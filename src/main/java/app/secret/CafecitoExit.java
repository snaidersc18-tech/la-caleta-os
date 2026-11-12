package app.secret;

import app.animations.FpsProbe;
import app.secret.audio.AudioManager;
import app.secret.audio.MusicTrack;
import app.secret.audio.SongCues;
import app.secret.dave.ParticleCanvas;
import app.secret.ui.MusicReactor;
import app.ui.SpiderSprite;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * "SALIR": EL CAFECITO. El Ministerio se convierte en una cafetería de tonos verdes cálidos y El Guachimán baja por su
 * hilo con un frappé en las patas; sonríe, abraza el vaso y la app se apaga. Todo va coreografiado sobre "Bonito,
 * Bonito" (MusicTrack.CAFECITO_THEME): cada cambio es un cue de {@link SongCues}, anclado al currentTime REAL del
 * MediaPlayer ({@code currentTimeProperty().addListener} vía AudioManager.onMusicTime), y las poses se mezclan en un
 * único AnimationTimer leyendo esa misma posición.
 * <pre>
 *    0.0 s  la cafetería aparece sobre la interfaz congelada (fundido 1.2 s); se encienden las lámparas con la voz
 *    9.8 s  entra el bombo: el emblema salta, el vapor de las tazas empieza a subir, el emblema late con los graves
 *   17.1 s  cae un hilo de telaraña desde el techo
 *   20.65 s 1.ª frase (el cafecito): El Guachimán baja por el hilo sosteniendo el frappé con las patas delanteras
 *   25.5 s  2.ª frase (la sonrisita): parpadea, abre los ojos mirando a cámara, sonrisa grande y cachetes rosados
 *   30.0 s  3.ª frase (el abracito): cruza las patas abrazando el vaso, cierra los ojos feliz, se acerca a la cámara
 *   32.7 s  fundido a negro rápido (0.3 s) con la música → 33.0 s Platform.exit()
 * </pre>
 * Los instantes de las frases salen del análisis del MP3 (respiros de la voz alineados a la rejilla de compases; ver
 * MusicTrack.CAFECITO_THEME): si hay que afinarlos, solo se cambian las constantes de abajo.
 *
 * <h3>Rendimiento</h3>
 * La cafetería y el emblema se pintan UNA vez en dos Canvas. Por frame solo se redibujan: la araña (rejilla de 84x80
 * celdas en tramos horizontales, ~250 rectángulos), el vapor (6 curvas) y las partículas. Nada de efectos por frame.
 */
public final class CafecitoExit {

    private static final double W = SecretContext.W, H = SecretContext.H;
    private static final MusicTrack SONG = MusicTrack.CAFECITO_THEME;

    // ---- guion (ms DENTRO de la canción)
    private static final double LAMP_1 = 600, LAMP_2 = 2_300, LAMP_3 = 4_700;   // entradas de la voz
    private static final double BEAT_IN = 9_800;                                // entra el bombo
    private static final double THREAD = 17_100;                                // golpe de graves: baja el hilo
    private static final double COFFEE = SONG.dropMs;                           // 20.65 s: "el cafecito"
    private static final double SMILE = 25_500;                                 // "la sonrisita"
    private static final double HUG = 30_000;                                   // "el abracito"
    private static final double FADE = 32_700, FADE_MS = 300;                   // fundido → exit a los 33.0 s
    private static final double ENTER_MS = 1_100, POSE_MS = 600, BLINK_MS = 150;

    // ---- la araña: rejilla propia (más grande que el sprite, para que quepan el vaso y los "brazos")
    private static final int GW = 84, GH = 80, OX = 12;            // el cuerpo del sprite va corrido OX celdas
    private static final double PX = 6;
    private static final double SPIDER_X = (W - GW * PX) / 2, SPIDER_Y = 178;
    private static final int CX = 30 + OX;                          // eje del cuerpo y del vaso
    private static final int[] SHOULDER_L = {SpiderSprite.FRONT_SHOULDER[0] + OX, SpiderSprite.FRONT_SHOULDER[1]};
    private static final int[] SHOULDER_R = {SpiderSprite.W - 1 - SpiderSprite.FRONT_SHOULDER[0] + OX, SpiderSprite.FRONT_SHOULDER[1]};
    // codo y mano de cada pata delantera, por pose (izquierda; la derecha es el espejo respecto de CX)
    private static final double[] HOLD = {14, 34, 34, 56};          // sostiene el vaso por los lados
    private static final double[] HUG_POSE = {11, 45, 51, 63};      // las patas se cruzan por delante del vaso

    // ---- valores de celda (1-4 son los del sprite)
    private static final int BLUSH = 5, TONGUE = 6, GREEN = 7, CREAM = 8, CARAMEL = 9, COFFEE_C = 10, FOAM = 11,
            LID = 12, PLASTIC = 13;
    private static final Color[] PALETTE = {
            Color.TRANSPARENT, Color.web("#f4ead6"), Color.web("#1d130d"), Color.web("#9c8b78"), Color.WHITE,
            Color.web("#ff8fab"), Color.web("#ff5d7a"), Color.web("#00704a"), Color.web("#fffaf0"), Color.web("#c98a3e"),
            Color.web("#9b6a43"), Color.web("#ead7bb"), Color.web("#e9e9e9"), Color.web("#cfd8d4")};

    private static final Color WALL_TOP = Color.web("#1e3932"), WALL_BOTTOM = Color.web("#0f2a22");
    private static final Color LAMP = Color.web("#ffd27a");

    private final Pane host;
    private final Node homeSpider, behind;
    private final AudioManager audio;
    private final Runnable onStart, beforeExit;

    private final Pane stage = new Pane();
    private final Canvas cafe = new Canvas(W, H);
    private final Canvas emblem = new Canvas(300, 300);
    private final Canvas steam = new Canvas(W, 260);
    private final Canvas spider = new Canvas(GW * PX, GH * PX);
    private final Line thread = new Line(W / 2, -10, W / 2, -10);
    private final Rectangle warmth = new Rectangle(W, H);
    private final Rectangle blackout = new Rectangle(W, H, Color.BLACK);
    private final ParticleCanvas hearts = new ParticleCanvas();
    private final Group[] lamps = new Group[3];
    private final int[][][] bodies = {SpiderSprite.generate(0, false), SpiderSprite.generate(1, false)};
    private final int[][] grid = new int[GH][GW];

    private double songMs = -1;
    private boolean wallClock, exiting;
    private long wallT0;
    private Runnable untapTime, stopCues;
    private MusicReactor reactor;
    private AnimationTimer director;

    private CafecitoExit(Pane host, Node homeSpider, Node behind, AudioManager audio, Runnable onStart, Runnable beforeExit) {
        this.host = host;
        this.homeSpider = homeSpider;
        this.behind = behind;
        this.audio = audio;
        this.onStart = onStart;
        this.beforeExit = beforeExit;
    }

    /**
     * @param host       capa a pantalla completa en coordenadas de MainView
     * @param homeSpider la araña del panel EL GUACHIMÁN (se oculta cuando la cafetería ya la tapa)
     * @param behind     la interfaz base: se congela en caché y deja de dibujarse tras el fundido (rendimiento)
     * @param onStart    al empezar (p. ej. ocultar el letrero del bloqueo de entrada)
     * @param beforeExit justo antes de Platform.exit() (liberar audio)
     */
    public static void play(Pane host, Node homeSpider, Node behind, AudioManager audio, Runnable onStart, Runnable beforeExit) {
        new CafecitoExit(host, homeSpider, behind, audio, onStart, beforeExit).start();
    }

    // ------------------------------------------------------------------ montaje

    private void start() {
        FpsProbe.scene("salir");
        long t0 = System.nanoTime();
        stage.setMinSize(W, H); stage.setPrefSize(W, H); stage.setMaxSize(W, H);
        stage.setPickOnBounds(true);                                   // nada se puede tocar durante la despedida
        stage.setOpacity(0);

        paintCafe(cafe.getGraphicsContext2D());
        paintEmblem(emblem.getGraphicsContext2D());
        emblem.relocate(96, 92);
        emblem.setCache(true);
        emblem.setCacheHint(CacheHint.SCALE);
        double[][] lampAt = {{455, 70}, {825, 96}, {1235, 40}};       // a los lados del hilo del centro
        for (int i = 0; i < lamps.length; i++) lamps[i] = lamp(lampAt[i][0], lampAt[i][1]);
        steam.setLayoutY(330);
        steam.setOpacity(0);
        thread.setStroke(Color.web("#e8e2d4", 0.85));
        thread.setStrokeWidth(2);
        spider.relocate(SPIDER_X, SPIDER_Y);
        spider.setVisible(false);
        warmth.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.7, true, CycleMethod.NO_CYCLE,
                new Stop(0.35, Color.web("#ffb3c7", 0.0)), new Stop(1, Color.web("#ff8fab", 0.34))));
        warmth.setOpacity(0);
        warmth.setMouseTransparent(true);
        blackout.setOpacity(0);
        blackout.setMouseTransparent(true);

        Group lampLayer = new Group(lamps);
        stage.getChildren().addAll(cafe, emblem, lampLayer, steam, thread, spider, warmth, hearts, blackout);
        host.getChildren().add(stage);

        // La interfaz queda congelada (entrada bloqueada): textura en GPU mientras la cafetería entra encima
        behind.setCache(true);
        behind.setCacheHint(CacheHint.SPEED);
        debug(String.format("preparación: %.0f ms", (System.nanoTime() - t0) / 1e6));
        onStart.run();

        reactor = new MusicReactor(audio, SONG).heartbeat(emblem, 0.035);
        reactor.start();
        stopCues = cues().start(audio, SONG, 0);
        if (audio != null) {
            audio.playMusic(SONG);                                     // stop() + dispose() de la CALLE y suena el cafecito
            untapTime = audio.onMusicTime(SONG, ms -> songMs = ms);
            audio.whenMusicPlaying(pos -> { if (pos < 0) useWallClock(); });
        } else {
            useWallClock();
        }
        director = new AnimationTimer() {
            @Override public void handle(long now) { frame(now); }
        };
        director.start();
        // red de seguridad: pase lo que pase con el audio, la app se cierra
        PauseTransition guard = new PauseTransition(Duration.millis(FADE + 12_000));
        guard.setOnFinished(e -> exit());
        guard.play();
        debug("SALIR recibido: arranca el cafecito (" + (FADE + FADE_MS) / 1000 + " s)");
    }

    private void useWallClock() { if (!wallClock) { wallClock = true; wallT0 = System.nanoTime(); } }

    /** La coreografía: cada entrada cae en su momento de la canción. */
    private SongCues cues() {
        return new SongCues()
                .at(0, () -> {
                    FadeTransition in = new FadeTransition(Duration.millis(1200), stage);
                    in.setToValue(1);
                    in.setOnFinished(e -> { behind.setVisible(false); homeSpider.setVisible(false); });
                    in.play();
                })
                .at(LAMP_1, () -> lightUp(lamps[0]))
                .at(LAMP_2, () -> lightUp(lamps[1]))
                .at(LAMP_3, () -> lightUp(lamps[2]))
                .at(BEAT_IN, () -> {
                    pop(emblem, 1.12);
                    fade(steam, 1500, 1);
                })
                .at(COFFEE, () -> spider.setVisible(true))
                .at(SMILE, () -> hearts.burst(W / 2, SPIDER_Y + 190, 10, ParticleCanvas.Kind.STAR, 220, 0.1, 1.0, 10,
                        Color.WHITE, Color.web("#ffe08a")))
                .at(HUG, () -> {
                    ScaleTransition closer = new ScaleTransition(Duration.millis(900), spider);
                    closer.setToX(1.2); closer.setToY(1.2);
                    closer.setInterpolator(Interpolator.SPLINE(0.3, 0, 0.2, 1));
                    closer.play();
                    fade(warmth, 900, 1);
                    hearts.burst(W / 2, SPIDER_Y + 330, 34, ParticleCanvas.Kind.HEART, 360, -0.2, 2.0, 18,
                            Color.web("#ff5d8f"), Color.web("#ffb3c7"), Color.web("#ffd166"));
                    hearts.floatHearts(14, Color.web("#ff5d8f"), Color.web("#ffb3c7"), Color.web("#ffd166"));
                })
                .at(FADE, () -> {
                    if (audio != null) audio.stopMusic(FADE_MS);       // la música se apaga con la imagen
                    FadeTransition black = new FadeTransition(Duration.millis(FADE_MS), blackout);
                    black.setToValue(1);
                    black.setOnFinished(e -> exit());
                    black.play();
                });
    }

    private void exit() {
        if (exiting) return;
        exiting = true;
        if (director != null) director.stop();
        if (reactor != null) reactor.stop();
        if (stopCues != null) stopCues.run();
        if (untapTime != null) untapTime.run();
        hearts.stop();
        debug("fin del cafecito → Platform.exit()");
        beforeExit.run();
        Platform.exit();
    }

    // ------------------------------------------------------------------ el director (un frame = una llamada)

    private void frame(long now) {
        double ms = wallClock ? (now - wallT0) / 1e6 : songMs;
        if (ms < 0) return;
        drawSteam(ms / 1000);
        if (ms >= THREAD) {                                           // el hilo baja desde el techo
            double u = smooth((ms - THREAD) / 1400);
            double end = ms < COFFEE ? -10 + u * 200 : spider.getLayoutY() + spider.getTranslateY() + 5 * PX;  // coronilla
            thread.setEndY(end);
        }
        if (ms < COFFEE) return;

        // 1) entrada: baja por el hilo y frena con un rebote suave
        double e = (ms - COFFEE) / ENTER_MS;
        double drop = e >= 1 ? 0 : -720 * (1 - backOut(Math.max(0, e)));
        double bob = 5 * Math.sin((ms - COFFEE) / 1000 * Math.PI * 1.65);   // ≈ un vaivén por compás
        spider.setTranslateY(drop + bob);
        spider.setRotate(e < 1 ? 0 : 1.8 * Math.sin((ms - COFFEE) / 1000 * Math.PI * 0.82));

        // 2) pose de las patas delanteras (sostener → abrazar) y altura del vaso
        double hug = smooth((ms - HUG) / POSE_MS);
        double lift = 3 * smooth((ms - SMILE) / POSE_MS) * (1 - hug);   // con la sonrisa levanta el vaso
        Face face = ms < SMILE ? Face.TOUGH
                : ms < SMILE + BLINK_MS ? Face.BLINK
                : ms < HUG + 160 ? Face.SMILE
                : ms < HUG + 160 + BLINK_MS ? Face.BLINK
                : Face.HAPPY;
        drawSpider((int) (ms / 420) % 2, hug, lift, face);
    }

    // ------------------------------------------------------------------ la araña

    private enum Face { TOUGH, BLINK, SMILE, HAPPY }

    private void drawSpider(int phase, double hug, double lift, Face face) {
        int[][] body = bodies[phase];
        for (int[] row : grid) java.util.Arrays.fill(row, 0);
        for (int y = 0; y < SpiderSprite.H; y++)
            for (int x = 0; x < SpiderSprite.W; x++) if (body[y][x] != 0) grid[y][x + OX] = body[y][x];

        switch (face) {
            case BLINK -> { eyesClosed(false); }
            case SMILE -> { eyesOpen(); smile(); blush(); }
            case HAPPY -> { eyesClosed(true); smile(); blush(); }
            default -> { }
        }
        int top = 44 - (int) Math.round(lift);
        cup(top);
        double[] pose = new double[4];
        for (int i = 0; i < 4; i++) pose[i] = HOLD[i] + (HUG_POSE[i] - HOLD[i]) * hug;
        pose[3] -= lift;
        limb(SHOULDER_L, pose[0], pose[1], pose[2], pose[3], false);
        limb(SHOULDER_R, pose[0], pose[1], pose[2], pose[3], true);

        GraphicsContext g = spider.getGraphicsContext2D();
        g.clearRect(0, 0, spider.getWidth(), spider.getHeight());
        for (int y = 0; y < GH; y++) {
            int x = 0;
            while (x < GW) {
                int v = grid[y][x];
                if (v == 0) { x++; continue; }
                int run = 1;
                while (x + run < GW && grid[y][x + run] == v) run++;
                g.setFill(PALETTE[v]);
                g.fillRect(x * PX, y * PX, run * PX, PX);
                x += run;
            }
        }
    }

    /** Pata delantera como "brazo": hombro → codo → mano (la derecha se refleja respecto del eje del cuerpo). */
    private void limb(int[] shoulder, double ex, double ey, double hx, double hy, boolean mirror) {
        int[][] pts = {
                {shoulder[0], shoulder[1]},
                {mirror ? (int) Math.round(2 * CX - ex) : (int) Math.round(ex), (int) Math.round(ey)},
                {mirror ? (int) Math.round(2 * CX - hx) : (int) Math.round(hx), (int) Math.round(hy)}};
        SpiderSprite.drawLimb(grid, pts);
        set(pts[2][0], pts[2][1], SpiderSprite.BONE);                  // la "mano" un poco más gruesa
        set(pts[2][0] + (mirror ? -1 : 1), pts[2][1], SpiderSprite.BONE);
    }

    /** Frappé con domo de crema, chorrito de caramelo, pajilla verde y un emblema verde redondo. */
    private void cup(int top) {
        for (int i = 0; i < 9; i++) set(CX + 4 - i / 4, top - 9 + i, GREEN);          // pajilla
        SpiderSprite.paintEllipse(grid, CX, top + 1, 8, 4, CREAM);                    // crema
        for (int x = -6; x <= 6; x += 2) set(CX + x, top + 1 + (Math.abs(x) % 4 == 0 ? 0 : -1), CARAMEL);
        set(CX - 1, top - 2, CARAMEL); set(CX + 2, top - 1, CARAMEL);
        for (int x = -9; x <= 9; x++) set(CX + x, top + 4, LID);                       // borde de la tapa
        for (int r = 0; r <= 21; r++) {                                                 // cuerpo: se angosta hacia abajo
            int y = top + 5 + r, hw = 8 - (int) Math.round(2.0 * r / 21);
            int fill = r < 4 ? FOAM : r < 17 ? COFFEE_C : r < 21 ? CARAMEL : FOAM;
            for (int x = -hw; x <= hw; x++) set(CX + x, y, Math.abs(x) == hw ? PLASTIC : fill);
        }
        int ey = top + 13;                                                              // emblema (original, no la sirena)
        SpiderSprite.paintEllipse(grid, CX, ey, 4, 4, GREEN);
        SpiderSprite.paintEllipse(grid, CX, ey, 3, 3, SpiderSprite.WHITE);
        SpiderSprite.paintEllipse(grid, CX, ey, 2, 2, GREEN);
        set(CX, ey - 1, SpiderSprite.WHITE); set(CX, ey, SpiderSprite.WHITE); set(CX - 1, ey, SpiderSprite.WHITE);
        set(CX + 1, ey, SpiderSprite.WHITE); set(CX, ey + 1, SpiderSprite.WHITE);
    }

    /** Ojos grandes y redondos, pupila al centro (mira a la cámara) con brillo. */
    private void eyesOpen() {
        for (int[] e : eyes()) {
            SpiderSprite.paintEllipse(grid, e[0], e[1], 7, 7, SpiderSprite.INK);
            SpiderSprite.paintEllipse(grid, e[0], e[1], 5, 5, SpiderSprite.WHITE);
            SpiderSprite.paintEllipse(grid, e[0], e[1] + 1, 2, 2, SpiderSprite.INK);
            set(e[0] - 1, e[1], SpiderSprite.WHITE);
            set(e[0] + 2, e[1] + 3, SpiderSprite.WHITE);
        }
    }

    /** Ojos cerrados: una línea (parpadeo) o un arco "^" (feliz). */
    private void eyesClosed(boolean happy) {
        for (int[] e : eyes()) {
            SpiderSprite.paintEllipse(grid, e[0], e[1], 8, 7, SpiderSprite.BONE);
            for (int x = -5; x <= 5; x++) {
                int y = happy ? e[1] + 1 - (int) Math.round(2.5 * (1 - (x / 5.0) * (x / 5.0))) : e[1] + 1;
                set(e[0] + x, y, SpiderSprite.INK);
                set(e[0] + x, y + 1, SpiderSprite.INK);
            }
        }
    }

    /** Sonrisa grande y tierna: boca en "D" con lengüita (borra los dientes de la mandíbula). */
    private void smile() {
        int cx = 30 + OX;
        SpiderSprite.paintEllipse(grid, cx, 34, 12, 9, SpiderSprite.BONE);
        for (int x = -9; x <= 9; x++) {
            int bottom = 33 + (int) Math.round(6 * (1 - (x / 9.5) * (x / 9.5)));
            for (int y = 32; y <= bottom; y++) set(cx + x, y, SpiderSprite.INK);
            if (Math.abs(x) <= 4) { set(cx + x, bottom - 1, TONGUE); if (Math.abs(x) <= 2) set(cx + x, bottom - 2, TONGUE); }
        }
    }

    private void blush() {
        SpiderSprite.paintEllipse(grid, 15 + OX, 30, 2, 1, BLUSH);
        SpiderSprite.paintEllipse(grid, 45 + OX, 30, 2, 1, BLUSH);
    }

    private static int[][] eyes() {
        return new int[][]{{SpiderSprite.EYE_L[0] + OX, SpiderSprite.EYE_L[1]}, {SpiderSprite.EYE_R[0] + OX, SpiderSprite.EYE_R[1]}};
    }

    private void set(int x, int y, int v) { if (x >= 0 && y >= 0 && x < GW && y < GH) grid[y][x] = v; }

    // ------------------------------------------------------------------ la cafetería (se pinta una sola vez)

    private static void paintCafe(GraphicsContext g) {
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, WALL_TOP), new Stop(1, WALL_BOTTOM)));
        g.fillRect(0, 0, W, H);
        g.setFill(Color.web("#ffffff", 0.025));                                         // listones verticales sutiles
        for (double x = 0; x < W; x += 48) g.fillRect(x, 0, 22, 430);
        // franja de madera detrás del mostrador
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8a5c36")), new Stop(1, Color.web("#5e3b22"))));
        g.fillRect(0, 430, W, 130);
        g.setStroke(Color.web("#000000", 0.18));
        g.setLineWidth(2);
        for (double x = 0; x < W; x += 36) g.strokeLine(x, 430, x, 560);
        paintMenu(g, 930, 96);
        // mostrador
        g.setFill(Color.web("#3b2416"));
        g.fillRect(0, 572, W, H - 572);
        g.setFill(Color.web("#a0714a"));
        g.fillRect(0, 560, W, 14);
        g.setStroke(Color.web("#000000", 0.25));
        for (double x = 80; x < W; x += 160) g.strokeLine(x, 590, x, H);
        mug(g, 200, 520);
        mug(g, 1080, 520);
        // pastel en su plato
        g.setFill(Color.web("#f2f2f2"));
        g.fillOval(300, 548, 110, 18);
        g.setFill(Color.web("#f3d9a4"));
        g.fillRect(322, 516, 66, 36);
        g.setFill(Color.web("#ff8fab"));
        g.fillRect(322, 516, 66, 9);
        g.setFill(Color.web("#c0392b"));
        g.fillOval(349, 506, 12, 12);
        // viñeta cálida
        g.setFill(new RadialGradient(0, 0, 0.5, 0.45, 0.85, true, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.TRANSPARENT), new Stop(1, Color.web("#000000", 0.55))));
        g.fillRect(0, 0, W, H);
    }

    private static void mug(GraphicsContext g, double x, double y) {
        g.setFill(Color.web("#f2f2f2"));
        g.fillOval(x - 56, y + 34, 112, 16);                                            // plato
        g.setFill(Color.WHITE);
        g.fillRoundRect(x - 34, y - 10, 68, 52, 18, 18);
        g.setStroke(Color.WHITE);
        g.setLineWidth(8);
        g.strokeOval(x + 24, y + 2, 26, 24);                                            // asa
        g.setFill(Color.web("#00704a"));
        g.fillRect(x - 34, y + 8, 68, 12);
        g.setFill(Color.web("#6f4428"));
        g.fillOval(x - 30, y - 16, 60, 14);                                             // el tinto
    }

    private static void paintMenu(GraphicsContext g, double x, double y) {
        g.setFill(Color.web("#6b4226"));
        g.fillRoundRect(x - 10, y - 10, 280, 270, 12, 12);
        g.setFill(Color.web("#1d1d1b"));
        g.fillRect(x, y, 260, 250);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        g.setFill(Color.web("#f4ead6"));
        g.setFont(Font.font("Georgia", FontWeight.BOLD, FontPosture.ITALIC, 30));
        g.fillText("Menú", x + 130, y + 14);
        g.setFont(Font.font("Georgia", FontPosture.ITALIC, 19));
        String[][] items = {{"Tintico", "1.000"}, {"Perico", "1.500"}, {"Frappé caramelo", "6.900"},
                {"Café del Guachimán", "gratis"}, {"Pandebono", "2.000"}};
        g.setTextAlign(TextAlignment.LEFT);
        for (int i = 0; i < items.length; i++) {
            double ty = y + 66 + i * 34;
            g.setFill(Color.web("#f4ead6"));
            g.fillText(items[i][0], x + 16, ty);
            g.setFill(Color.web("#9ad1b9"));
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(items[i][1], x + 244, ty);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    /**
     * Emblema ORIGINAL de la cafetería (círculo verde y blanco al estilo de las grandes cadenas, pero con una taza
     * humeante y el nombre del Ministerio: no reproduce ningún logo real).
     */
    private static void paintEmblem(GraphicsContext g) {
        double c = 150;
        g.setFill(Color.web("#00704a"));
        g.fillOval(c - 140, c - 140, 280, 280);
        g.setFill(Color.WHITE);
        g.fillOval(c - 92, c - 92, 184, 184);
        g.setFill(Color.web("#00704a"));
        g.fillOval(c - 86, c - 86, 172, 172);
        // texto en el aro
        String ring = "CAFECITO  ★  DEL REBUSQUE  ★  ";
        g.setFont(Font.font("Arial Black", FontWeight.BLACK, 21));
        g.setFill(Color.WHITE);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        for (int i = 0; i < ring.length(); i++) {
            g.save();
            g.translate(c, c);
            g.rotate(-90 + 360.0 * i / ring.length() + 90);
            g.fillText(String.valueOf(ring.charAt(i)), 0, -114);
            g.restore();
        }
        // taza humeante
        g.setFill(Color.WHITE);
        g.fillPolygon(new double[]{c - 44, c + 44, c + 34, c - 34}, new double[]{c - 6, c - 6, c + 52, c + 52}, 4);
        g.setStroke(Color.WHITE);
        g.setLineWidth(9);
        g.strokeOval(c + 30, c + 2, 30, 30);
        g.fillOval(c - 60, c + 48, 120, 16);
        g.setLineWidth(5);
        g.setLineCap(StrokeLineCap.ROUND);
        for (int k = -1; k <= 1; k++) {
            double sx = c + k * 22;
            g.beginPath();
            g.moveTo(sx, c - 16);
            g.bezierCurveTo(sx - 12, c - 30, sx + 12, c - 44, sx, c - 60);
            g.stroke();
        }
        g.setFill(Color.web("#00704a"));
        g.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        g.fillText("♥", c, c + 22);
    }

    /** Lámpara colgante: cable, pantalla y halo (apagado hasta que la voz la enciende). */
    private static Group lamp(double x, double drop) {
        Line cord = new Line(x, 0, x, drop);
        cord.setStroke(Color.web("#0b1a15"));
        cord.setStrokeWidth(3);
        Polygon shade = new Polygon(x - 14, drop, x + 14, drop, x + 34, drop + 34, x - 34, drop + 34);
        shade.setFill(Color.web("#2a2a2a"));
        Circle bulb = new Circle(x, drop + 36, 9, Color.web("#5a4a2a"));
        Circle glow = new Circle(x, drop + 70, 130, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffd27a", 0.55)), new Stop(1, Color.web("#ffd27a", 0))));
        glow.setOpacity(0);
        glow.setMouseTransparent(true);
        return new Group(glow, cord, shade, bulb);
    }

    private static void lightUp(Group lamp) {
        ((Circle) lamp.getChildren().get(3)).setFill(LAMP);
        fade(lamp.getChildren().get(0), 700, 1);
    }

    // ------------------------------------------------------------------ vapor de las tazas del mostrador

    private void drawSteam(double t) {
        if (steam.getOpacity() <= 0) return;
        GraphicsContext g = steam.getGraphicsContext2D();
        g.clearRect(0, 0, W, steam.getHeight());
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(5);
        for (double base : new double[]{200, 1080}) {
            for (int i = 0; i < 3; i++) {
                double x0 = base - 14 + i * 14;
                g.setStroke(Color.web("#ffffff", 0.18 + 0.06 * i));
                g.beginPath();
                for (int k = 0; k <= 20; k++) {
                    double y = 176 - k * 8;
                    double x = x0 + 9 * Math.sin(k * 0.45 + t * 2.1 + i * 1.7) * (k / 20.0);
                    if (k == 0) g.moveTo(x, y); else g.lineTo(x, y);
                }
                g.stroke();
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static void fade(Node n, double ms, double to) {
        FadeTransition f = new FadeTransition(Duration.millis(ms), n);
        f.setToValue(to);
        f.play();
    }

    private static void pop(Node n, double peak) {
        ScaleTransition s = new ScaleTransition(Duration.millis(260), n);
        s.setFromX(peak); s.setFromY(peak);
        s.setToX(1); s.setToY(1);
        s.setInterpolator(Interpolator.EASE_OUT);
        s.play();
    }

    private static double smooth(double u) {
        u = Math.max(0, Math.min(1, u));
        return u * u * (3 - 2 * u);
    }

    /** Llega, se pasa un poquito y vuelve (rebote suave al frenar en el hilo). */
    private static double backOut(double t) {
        double c = 1.4, u = t - 1;
        return 1 + (c + 1) * u * u * u + c * u * u;
    }

    /** Diagnóstico (-Drebusque.debug=true): marca de tiempo en stderr. */
    static void debug(String msg) {
        if (Boolean.getBoolean("rebusque.debug")) System.err.printf("[salir %d ms] %s%n", System.currentTimeMillis() % 100_000, msg);
    }
}
