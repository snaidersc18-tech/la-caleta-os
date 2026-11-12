package app.secret.view;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.MusicTrack;
import app.secret.audio.SoundEffect;
import app.secret.ui.FeidSprite;
import app.secret.ui.FerxxoRiderArt;
import app.secret.ui.PixelArt;
import app.ui.SpiderCanvas;
import app.ui.SpiderSprite;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static app.secret.ui.FerxxoRiderArt.*;

/**
 * CINEMÁTICA "EL DRIFT DEL FERXXO" (modo FEID / FERXXO): un callejón oscuro de Medellín bañado en neón verde.
 * <pre>
 *   0.0 s  el callejón se enciende (letrero de neón titilando, farol); El Guachimán aparece en la calle
 *   0.35 s se oye una moto que se acerca (MOTO_ENGINE)
 *   1.0 s  ENTRA: Feid en su moto de motocross a toda velocidad desde la izquierda (ruedas girando, motor sacudiendo)
 *   2.0 s  DERRAPE: la moto se cruza (squash horizontal + inclinación), marcas de llanta y humo verde (MOTO_DRIFT)
 *   ~2.4 s la rueda delantera pasa sobre la araña: la aplasta, temblor de cámara y explosión de humo verde (MOTO_CRUSH)
 *   2.9 s  la moto queda quieta, ronroneando
 *   3.25 s Feid se baja de un salto y cae frente a la moto
 *   3.95 s la cámara se le acerca a la cara → primer plano: serio → media sonrisa → SONRISA con grills de oro
 *   5.4 s  un destello cruza los grills… 5.9 s se expande hasta dejar la pantalla en BLANCO → onFinished
 *          (BaseMode monta FERXXO RIDER debajo y {@link #dissolve()} disipa el blanco)
 * </pre>
 * Rendimiento: un único {@link AnimationTimer} director con tiempo real (si un frame llega tarde la escena no se
 * atrasa). El callejón es UNA imagen 320x180 pintada en segundo plano tras la intro; la moto cambia de imagen
 * (4 cuadros precalculados) en vez de redibujarse; solo el humo y las marcas de llanta usan Canvas.
 */
public final class FerxxoDriftCinematic extends TakeoverView implements Cinematic {

    // ---- guion (segundos) — la primera parte es fija; lo demás se mide en BEATS desde el aplastón (ver abajo)
    private static final double SPIDER_AT = 0.25, ENGINE_AT = 0.35, ENTER_AT = 1.0, DRIFT_AT = 2.0, STOP_AT = 2.9;

    // ---- geometría (lienzo lógico 1280x720; el callejón es 320x180 x4)
    private static final double K = 4, PX = 4, FACE_PX = 10;
    private static final double FLOOR_Y = 150 * K;
    private static final double BIKE_Y = FLOOR_Y - CONTACT_Y * PX - PX;
    private static final double SPIDER_PX = 2, SPIDER_X = 770;
    private static final double ENTER_FROM = -RW * PX - 40, DRIFT_FROM = SPIDER_X - 430, STOP_X = SPIDER_X - 70;
    private static final double CRUSH_X = SPIDER_X - FRONT_X * PX;     // la rueda delantera llega a la araña
    private static final double FEID_Y = FLOOR_Y - 57 * PX;           // los tenis de Feid de pie apoyan en el piso
    private static final double LAND_X = STOP_X + RW * PX - 30;
    private static final double FACE_X = (W - FeidSprite.FW * FACE_PX) / 2, FACE_Y = (H - FeidSprite.FH * FACE_PX) / 2 + 10;
    private static final double GRILL_SX = FACE_X + (FeidSprite.GRILL_X + 0.5) * FACE_PX;
    private static final double GRILL_SY = FACE_Y + (FeidSprite.GRILL_Y + 0.5) * FACE_PX;
    private static final Color GREEN = Color.web("#00ff66");

    // ---- RHYTHM SYNC con feid_theme (97 BPM medidos). El derrape es x(u) = A + (B−A)·(1−(1−u)²), así que la rueda
    // delantera pisa a la araña en un instante calculable: CRUSH_T. A partir de ahí todo cae en golpes de la canción
    // y el blanco puro llega EXACTO al drop (32.555 s), 8 beats (2 compases) después del aplastón.
    private static final double BEAT_S = MusicTrack.FEID_THEME.beatMs() / 1000;
    private static final double CRUSH_T = DRIFT_AT + (STOP_AT - DRIFT_AT)
            * (1 - Math.sqrt(1 - (CRUSH_X - DRIFT_FROM) / (STOP_X - DRIFT_FROM)));
    private static final double HOP_END = CRUSH_T + 3 * BEAT_S, HOP_AT = HOP_END - 0.5;   // aterriza en un golpe
    private static final double SMIRK_AT = CRUSH_T + 5 * BEAT_S, FACE_AT = SMIRK_AT - 0.45, ZOOM_AT = FACE_AT - 0.6;
    private static final double GRIN_AT = CRUSH_T + 6 * BEAT_S, SHINE_AT = CRUSH_T + 6.5 * BEAT_S;
    private static final double END_AT = CRUSH_T + 8 * BEAT_S, WHITE_AT = END_AT - 0.6;
    /** Segundo de la canción en que arranca la cinemática: así END_AT suena justo en el drop. */
    private static final double SONG_START_MS = MusicTrack.FEID_THEME.dropMs - END_AT * 1000;

    private final SecretContext ctx;
    private final Pane world = new Pane();
    private final Scale camScale = new Scale(1, 1, 0, 0);
    private final Text neon = new Text("FERXXO");
    private final Polygon lampCone = new Polygon(184, 118, 224, 118, 360, 600, 60, 600);
    private final SpiderCanvas spider = new SpiderCanvas(SPIDER_PX);
    private final Canvas skid = new Canvas(W, H);
    private final Canvas smoke = new Canvas(W, H);
    private final Image[] bikeFrames = new Image[FRAMES];
    private final Image[] riderFrames = new Image[2];
    private final ImageView bikeView = new ImageView(), riderView = new ImageView();
    private final Group bike = new Group(bikeView, riderView);
    private final Rotate lean = new Rotate(0, REAR_X * PX, CONTACT_Y * PX);
    private final Scale squash = new Scale(1, 1, RW * PX / 2, CONTACT_Y * PX);
    private final ImageView standing = new ImageView();
    private final Canvas face = new Canvas(FeidSprite.FW * FACE_PX, FeidSprite.FH * FACE_PX);
    private final Group shine = new Group();
    private final Circle burst = new Circle(GRILL_SX, GRILL_SY, 10, Color.WHITE);
    private final Rectangle white = new Rectangle(W, H, Color.WHITE);
    private final List<double[]> particles = new ArrayList<>();     // {x, y, vx, vy, edad, vida, tamaño, tipo}
    private final Random rnd = new Random();

    private Runnable onFinished = () -> { };
    private AnimationTimer director;
    private double clock, skipTo = -1, skipShift, bikeX = ENTER_FROM, lastSkidX = Double.NaN, smokeAcc;
    private int frame = -1, expr = -1;
    private boolean engine, drift, crushed, finished, flicker;

    public FerxxoDriftCinematic(SecretContext ctx) {
        this.ctx = ctx;
        setStyle("-fx-background-color: black;");
        world.setPrefSize(W, H); world.setMinSize(W, H); world.setMaxSize(W, H);
        world.getTransforms().add(camScale);

        ImageView alley = new ImageView(FerxxoRiderArt.alley());
        alley.setFitWidth(W); alley.setFitHeight(H);
        alley.setSmooth(false);

        neon.setFont(Font.font("Impact", FontWeight.BLACK, 92));
        neon.setFill(GREEN);
        neon.setStroke(Color.web("#eafff2"));
        neon.setStrokeWidth(2);
        neon.setEffect(new DropShadow(34, GREEN));
        neon.setX(512); neon.setY(232);
        neon.setRotate(-4);
        neon.setCache(true);
        neon.setCacheHint(CacheHint.SPEED);

        lampCone.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#c8ffe0", 0.45)), new Stop(1, Color.web("#00ff66", 0))));
        lampCone.setBlendMode(BlendMode.ADD);
        lampCone.setMouseTransparent(true);
        lampCone.setCache(true);

        for (int i = 0; i < FRAMES; i++) bikeFrames[i] = FerxxoRiderArt.image(FerxxoRiderArt.bike(i), FeidSprite.PALETTE);
        for (int i = 0; i < 2; i++) riderFrames[i] = FerxxoRiderArt.image(FerxxoRiderArt.rider(i), FeidSprite.PALETTE);
        for (ImageView v : new ImageView[]{bikeView, riderView}) {
            v.setFitWidth(RW * PX); v.setFitHeight(RH * PX); v.setSmooth(false);
        }
        bikeView.setImage(bikeFrames[0]);
        riderView.setImage(riderFrames[0]);
        bike.getTransforms().addAll(squash, lean);
        bike.relocate(ENTER_FROM, BIKE_Y);

        standing.setImage(FerxxoRiderArt.image(FeidSprite.body(FeidSprite.STAND), FeidSprite.PALETTE));
        standing.setFitWidth(FeidSprite.BW * PX); standing.setFitHeight(FeidSprite.BH * PX);
        standing.setSmooth(false);
        standing.setVisible(false);

        spider.relocate(SPIDER_X - SpiderSprite.W * SPIDER_PX / 2, FLOOR_Y - SpiderSprite.H * SPIDER_PX + 4);
        spider.setScaleX(0); spider.setScaleY(0);
        skid.setMouseTransparent(true);
        smoke.setMouseTransparent(true);

        world.getChildren().addAll(alley, neon, lampCone, skid, spider, bike, standing, smoke);
        world.setOpacity(0);

        face.relocate(FACE_X, FACE_Y);
        face.setOpacity(0);
        buildShine();
        burst.setOpacity(0);
        white.setOpacity(0);
        white.setMouseTransparent(true);
        setClip(new Rectangle(W, H));
        getChildren().add(new Pane(world, face, shine, burst, white));

        setOnMouseClicked(e -> skip());
        setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.SPACE) skip(); });
        onDispose(() -> { if (director != null) director.stop(); spider.dispose(); });
    }

    // ================================================================== Cinematic

    @Override public Node node() { return this; }

    /**
     * Arranca feid_theme en {@link #SONG_START_MS} y, cuando el audio suena DE VERDAD, el director: su reloj es la
     * posición real de la canción (no la suma de dt), así el aplastón y el flash no se desfasan aunque se pierdan
     * frames. Sin audio, el reloj vuelve a ser el tiempo real transcurrido.
     */
    @Override public void play(Runnable onFinished) {
        this.onFinished = onFinished;
        requestFocus();
        ctx.audio().playMusic(MusicTrack.FEID_THEME, SONG_START_MS);
        ctx.audio().whenMusicPlaying(pos -> {
            if (finished || isDisposed()) return;
            director = new AnimationTimer() {
                private long last;
                @Override public void handle(long now) {
                    double dt = last == 0 ? 0 : Math.min(0.25, (now - last) / 1e9);
                    last = now;
                    double song = ctx.audio().musicPositionMs();
                    double t = song >= 0 ? (song - SONG_START_MS) / 1000 + skipShift : clock + dt;
                    if (skipTo > t) { skipShift += skipTo - t; t = skipTo; }
                    skipTo = -1;
                    clock = Math.max(clock, t);                      // nunca retrocede
                    frame(clock, dt);
                }
            };
            director.start();
        });
    }

    /** El blanco puro se disipa revelando FERXXO RIDER, que BaseMode ya montó debajo. */
    @Override public void dissolve() {
        setMouseTransparent(true);
        if (director != null) director.stop();
        world.setVisible(false);
        face.setVisible(false);
        shine.setVisible(false);
        burst.setVisible(false);
        setStyle("-fx-background-color: transparent;");
        white.setOpacity(1);
        FadeTransition out = new FadeTransition(Duration.millis(750), white);
        out.setToValue(0);
        out.setOnFinished(e -> disposeAndRemove());
        out.play();
    }

    @Override public void stop() { disposeAndRemove(); }

    /** Clic / ESC / ESPACIO: directo al destello final. */
    private void skip() { if (clock < WHITE_AT) skipTo = WHITE_AT; }

    // ================================================================== el director

    private void frame(double t, double dt) {
        world.setOpacity(Math.min(1, t / 0.45));
        neonFlicker(t);

        // 1) El Guachimán aparece en la calle (rebote elástico)
        if (t >= SPIDER_AT && !crushed) {
            double u = Math.min(1, (t - SPIDER_AT) / 0.35);
            double s = u < 1 ? backOut(u) : 1;
            spider.setScaleX(s); spider.setScaleY(s);
        }
        if (t >= ENGINE_AT && !engine) { engine = true; ctx.audio().play(SoundEffect.MOTO_ENGINE); }

        // 2) entra la moto, 3) derrapa y frena
        double lastX = bikeX;
        if (t >= ENTER_AT && t < DRIFT_AT) {
            double u = (t - ENTER_AT) / (DRIFT_AT - ENTER_AT);
            bikeX = ENTER_FROM + (DRIFT_FROM - ENTER_FROM) * u;
            riderView.setImage(riderFrames[(int) (t * 18) % 2]);       // el motor lo sacude
        } else if (t >= DRIFT_AT && t < STOP_AT) {
            if (!drift) { drift = true; ctx.audio().play(SoundEffect.MOTO_DRIFT); }
            double u = (t - DRIFT_AT) / (STOP_AT - DRIFT_AT);
            bikeX = DRIFT_FROM + (STOP_X - DRIFT_FROM) * (1 - (1 - u) * (1 - u));
            double k = Math.sin(Math.PI * u);
            lean.setAngle(-9 * k);                                      // se recuesta: la trasera clava y la delantera sube
            squash.setX(1 - 0.2 * k);                                   // se cruza de lado (escorzo)
            skidMark(lastX);
            puff(dt, 140 * k + 20);
        } else if (t >= STOP_AT) {
            bikeX = STOP_X;
            lean.setAngle(0); squash.setX(1);
            bike.setTranslateY(rnd.nextInt(3) == 0 ? -1 : 0);           // ronroneo en neutro
            if (t < HOP_AT) riderView.setImage(riderFrames[(int) (t * 12) % 2]);
            if (t < STOP_AT + 0.6) puff(dt, 30);
        }
        bike.setLayoutX(bikeX);
        int f = (int) Math.floorMod((long) (bikeX / 7), FRAMES);         // las ruedas giran con la distancia
        if (f != frame) { frame = f; bikeView.setImage(bikeFrames[f]); }

        if (!crushed && bikeX >= CRUSH_X) crush();

        // 4) Feid se baja de un salto
        if (t >= HOP_AT && t < ZOOM_AT) {
            riderView.setVisible(false);
            standing.setVisible(true);
            double u = Math.min(1, (t - HOP_AT) / (HOP_END - HOP_AT));
            double sx = STOP_X + (HIP_X - 17) * PX, sy = BIKE_Y + (HIP_Y - 39) * PX;
            standing.setLayoutX(lerp(sx, LAND_X, easeBoth(u)));
            standing.setLayoutY(lerp(sy, FEID_Y, u) - Math.sin(Math.PI * u) * 70);
        }

        stepParticles(dt);
        camera(t);

        // 5) primer plano y gestos
        if (t >= FACE_AT) {
            double u = Math.min(1, (t - FACE_AT) / 0.4);
            world.setOpacity(1 - u);
            face.setOpacity(u);
            double s = 0.92 + 0.08 * easeBoth(u);
            face.setScaleX(s); face.setScaleY(s);
            setExpr(t >= GRIN_AT ? FeidSprite.GRILLS : t >= SMIRK_AT ? FeidSprite.SMIRK : FeidSprite.NEUTRAL);
        }
        // 6) el destello cruza los grills de oro
        if (t >= SHINE_AT && t < WHITE_AT + 0.3) {
            if (shine.getOpacity() == 0) ctx.audio().play(SoundEffect.GRILL_SHINE);
            double u = Math.min(1, (t - SHINE_AT) / 0.5);
            shine.setOpacity(Math.sin(Math.PI * Math.min(1, u * 1.1)) * 0.95 + 0.05);
            shine.setTranslateX(GRILL_SX - 60 + 120 * easeBoth(u));
            shine.setTranslateY(GRILL_SY - 4);
            shine.setRotate(u * 180);
        }
        // 7) el flash ciega la pantalla
        if (t >= WHITE_AT) {
            double u = Math.min(1, (t - WHITE_AT) / (END_AT - WHITE_AT));
            burst.setOpacity(Math.min(1, u * 2));
            double r = 1 + u * u * 160;
            burst.setScaleX(r); burst.setScaleY(r);
            white.setOpacity(Math.max(0, (u - 0.35) / 0.65));
            if (u >= 1 && !finished) { finished = true; white.setOpacity(1); onFinished.run(); }
        }
    }

    // ------------------------------------------------------------------ el aplastón

    private void crush() {
        crushed = true;
        ctx.audio().play(SoundEffect.MOTO_CRUSH);
        spider.setScaleX(1.25);
        spider.setScaleY(0.15);
        spider.setTranslateY(SpiderSprite.H * SPIDER_PX * 0.42);
        spider.glitchFor(450);
        for (int i = 0; i < 70; i++) {                                // nube de humo verde
            double a = Math.PI + rnd.nextDouble() * Math.PI, sp = 90 + rnd.nextDouble() * 330;
            particles.add(new double[]{SPIDER_X, FLOOR_Y - 10, Math.cos(a) * sp, Math.sin(a) * sp * 0.5,
                    0, 0.7 + rnd.nextDouble() * 0.8, 14 + rnd.nextDouble() * 22, 0});
        }
        for (int i = 0; i < 90; i++) {                                // glitch verde (píxeles de la araña)
            double a = rnd.nextDouble() * Math.PI * 2, sp = 80 + rnd.nextDouble() * 260;
            particles.add(new double[]{SPIDER_X + (rnd.nextDouble() - 0.5) * 100, FLOOR_Y - 20 - rnd.nextDouble() * 30,
                    Math.cos(a) * sp, Math.sin(a) * sp - 140, 0, 0.4 + rnd.nextDouble() * 0.5, SPIDER_PX * 2, 1});
        }
    }

    /** Humo verde que sale de la rueda trasera ({@code rate} bocanadas por segundo). */
    private void puff(double dt, double rate) {
        smokeAcc += rate * dt;
        double rx = bikeX + REAR_X * PX, ry = BIKE_Y + CONTACT_Y * PX;
        while (smokeAcc >= 1) {
            smokeAcc -= 1;
            particles.add(new double[]{rx + (rnd.nextDouble() - 0.5) * 30, ry - rnd.nextDouble() * 10,
                    -60 - rnd.nextDouble() * 160, -30 - rnd.nextDouble() * 70, 0, 0.6 + rnd.nextDouble() * 0.7,
                    10 + rnd.nextDouble() * 16, 0});
        }
    }

    /** Marca de llanta: un trazo oscuro bajo la rueda trasera, que se queda pintado en el asfalto. */
    private void skidMark(double fromX) {
        double y = BIKE_Y + CONTACT_Y * PX - 2;
        double x1 = bikeX + REAR_X * PX;
        double x0 = Double.isNaN(lastSkidX) ? x1 : lastSkidX;
        GraphicsContext g = skid.getGraphicsContext2D();
        g.setFill(Color.web("#020303", 0.75));
        g.fillRect(Math.min(x0, x1), y, Math.abs(x1 - x0) + 2, 5);
        g.setFill(Color.web("#00ff66", 0.12));
        g.fillRect(Math.min(x0, x1), y + 5, Math.abs(x1 - x0) + 2, 2);
        lastSkidX = x1;
    }

    /** Humo (crece y se desvanece, con arrastre) y glitch (cae con gravedad): Euler con dt real. */
    private void stepParticles(double dt) {
        GraphicsContext g = smoke.getGraphicsContext2D();
        if (particles.isEmpty()) { smoke.setVisible(false); return; }
        smoke.setVisible(true);
        g.clearRect(0, 0, W, H);
        for (Iterator<double[]> it = particles.iterator(); it.hasNext(); ) {
            double[] p = it.next();
            p[4] += dt;
            if (p[4] >= p[5]) { it.remove(); continue; }
            double life = p[4] / p[5];
            boolean glitch = p[7] == 1;
            double drag = Math.exp(-(glitch ? 0.5 : 2.4) * dt);
            p[2] *= drag;
            p[3] = p[3] * drag + (glitch ? 700 : -40) * dt;
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            if (glitch) {
                g.setGlobalAlpha(1 - life);
                g.setFill(rnd.nextInt(4) == 0 ? Color.WHITE : GREEN);
                g.fillRect(p[0], p[1], p[6], p[6]);
            } else {
                double s = p[6] * (1 + life * 2.5);
                g.setGlobalAlpha(Math.pow(1 - life, 1.5) * 0.45);
                g.setFill(life < 0.35 ? Color.web("#7dffb2") : Color.web("#4f7a62"));
                g.fillOval(p[0] - s / 2, p[1] - s / 2, s, s);
            }
        }
        g.setGlobalAlpha(1);
        if (crushed) spider.setOpacity(Math.max(0, spider.getOpacity() - dt * 2.5));
    }

    // ------------------------------------------------------------------ cámara

    /** Plano general con temblor al aplastar; luego acercamiento con EASE_BOTH hasta la cara de Feid. */
    private void camera(double t) {
        double headX = LAND_X + FeidSprite.HEAD_X * PX, headY = FEID_Y + FeidSprite.HEAD_Y * PX;
        double fx = W / 2, fy = H / 2, s = 1;
        if (t >= ZOOM_AT) {
            double u = easeBoth(Math.min(1, (t - ZOOM_AT) / (FACE_AT - ZOOM_AT)));
            fx = lerp(W / 2, headX, u); fy = lerp(H / 2, headY, u); s = lerp(1, 3.4, u);
        }
        lampCone.setVisible(s < 1.6);                                  // capa ADD a pantalla completa: fuera del zoom
        double shake = 0, since = crushTime(t);
        if (since >= 0 && since < 0.5) shake = 16 * (1 - since / 0.5);
        camScale.setX(s); camScale.setY(s);
        world.setTranslateX(W / 2 - fx * s + (rnd.nextDouble() - 0.5) * 2 * shake);
        world.setTranslateY(H / 2 - fy * s + (rnd.nextDouble() - 0.5) * 2 * shake);
    }

    private double crushAt = -1;

    private double crushTime(double t) {
        if (crushed && crushAt < 0) crushAt = t;
        return crushAt < 0 ? -1 : t - crushAt;
    }

    private void neonFlicker(double t) {
        boolean off = (t > 0.5 && t < 0.58) || (t > 0.66 && t < 0.7) || (t > 2.45 && t < 2.55) || rnd.nextInt(240) == 0;
        if (off != flicker) { flicker = off; neon.setOpacity(off ? 0.25 : 1); }
    }

    private void setExpr(int e) {
        if (e == expr) return;
        expr = e;
        GraphicsContext g = face.getGraphicsContext2D();
        g.clearRect(0, 0, face.getWidth(), face.getHeight());
        PixelArt.draw(g, FeidSprite.face(e), FeidSprite.PALETTE, 0, 0, FACE_PX);
    }

    private void buildShine() {
        Polygon star = new Polygon(0, -34, 6, -6, 34, 0, 6, 6, 0, 34, -6, 6, -34, 0, -6, -6);
        star.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.5, Color.web("#fff1b0")), new Stop(1, Color.web("#f5c542", 0))));
        Rectangle streak = new Rectangle(-160, -2, 320, 4);
        streak.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(0.5, Color.web("#fff6cf", 0.9)), new Stop(1, Color.TRANSPARENT)));
        Circle core = new Circle(0, 0, 14, Color.web("#ffffff", 0.9));
        shine.getChildren().addAll(streak, star, core);
        shine.setEffect(new Glow(0.8));
        shine.setBlendMode(BlendMode.ADD);
        shine.setOpacity(0);
        shine.setMouseTransparent(true);
        shine.setCache(true);
        shine.setCacheHint(CacheHint.SPEED);
        burst.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.7, Color.web("#fffbe6")), new Stop(1, Color.web("#ffffff", 0))));
    }

    // ================================================================== utilidades

    private static double lerp(double a, double b, double u) { return a + (b - a) * u; }

    private static double easeBoth(double u) { u = Math.max(0, Math.min(1, u)); return u * u * (3 - 2 * u); }

    private static double backOut(double u) { double c = 1.7, v = u - 1; return 1 + (c + 1) * v * v * v + c * v * v; }
}
