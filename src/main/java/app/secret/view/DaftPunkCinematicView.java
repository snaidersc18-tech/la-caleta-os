package app.secret.view;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.BeatSync;
import javafx.animation.AnimationTimer;
import app.secret.ui.Avatars;
import app.secret.ui.Fx;
import app.secret.ui.LightShow;
import app.secret.ui.PixelArt;
import app.secret.ui.PixelBackdrop;
import app.secret.ui.VocalHud;
import app.secret.audio.MusicTrack;
import javafx.animation.Animation;
import javafx.animation.FillTransition;
import javafx.animation.Interpolator;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.shape.Polygon;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * CINEMÁTICA DE ENTRADA "HUMAN AFTER ALL" (orquestada con SequentialTransition / ParallelTransition / Timeline):
 *   1. Fundido a negro (la UI base ya quedó oculta en la fase TAKEOVER).
 *   2. Fade-in del escenario de la pirámide (andamios, pantallas triangulares de LED) y arranque del show de luces.
 *   3. Aparecen los dos músicos; cabecean y centellean al ritmo.
 *   4. ZOOM-IN en cámara lenta (ScaleTransition + TranslateTransition en paralelo) hacia los cascos.
 *   5. Diálogo RPG con efecto máquina de escribir y vocoder. Clic = avanzar / completar, ESC = saltar.
 * El zoom combina ScaleTransition y TranslateTransition: para acercar la cámara al punto P con factor Z, el mundo
 * se escala Z (pivote en su centro C) y se traslada −(P − C)·Z, de modo que P termina en el centro de la pantalla.
 */
public final class DaftPunkCinematicView extends TakeoverView implements Cinematic {

    // Las palabras HARDER/BETTER/FASTER/STRONGER las pone VocalHud cuando el vocoder las canta; el diálogo ya no las
    // teclea a destiempo.
    private static final String LINE = ">>> HUMAN... AFTER... ALL... ENCALETADO";
    private static final double HELMET_X = 640, HELMET_Y = 336, ZOOM = 2.2;

    private enum State { CINEMA, TYPING, WAITING, DONE }

    private final SecretContext ctx;
    private Runnable onFinished = () -> { };
    private final Rectangle black = new Rectangle(W, H, Color.BLACK);
    private final Pane world = new Pane();
    private final PixelBackdrop stage = PixelBackdrop.pyramid(160, 90, 8);
    private final LightShow lights = new LightShow(W, H);
    // Concierto en vivo: cada músico en su propio Canvas para que cabeceen por separado; las sombras no se mueven.
    private final Canvas shadows = new Canvas(W, H);
    private final Canvas avatarL = new Canvas(W, H);
    private final Canvas avatarR = new Canvas(W, H);
    private final Group avatars = new Group(shadows, avatarL, avatarR);
    private final Group strobes = new Group();
    /** Copias de las pantallas triangulares que destellan con el color de cada palabra cantada (las mueve VocalHud). */
    private final Group vocalStrobes = new Group();
    private final VocalHud vocals = new VocalHud(W, H);
    /** Beat MEDIDO de "Harder, Better, Faster, Stronger" (123.48 BPM = 485.9 ms). */
    private static final double BEAT_MS = MusicTrack.DAFT_PUNK_THEME.beatMs();
    private final BeatSync sync;
    private AnimationTimer typer;
    private double typeFrom = Double.NaN;
    private long wallStart;
    private static final Color[] STROBE = {Color.web("#00f5ff"), Color.web("#ffe94a"), Color.web("#ff2bd6")};
    private final Pane dialog = new Pane();
    private final Label text = Fx.label("", 26, "#ffffff");
    private final Label prompt = Fx.label("▼  CLIC / ESPACIO", 14, "#ffe94a");
    private final Rectangle flash = new Rectangle(W, H, Color.web("#00f5ff"));

    private State state = State.CINEMA;
    private SequentialTransition seq;
    private Timeline avatarLoop;
    private PauseTransition wait;
    private int tick;

    public DaftPunkCinematicView(SecretContext ctx) {
        this.ctx = ctx;
        this.sync = new BeatSync(ctx.audio());
        lights.syncTo(ctx.audio());

        world.setPrefSize(W, H); world.setMinSize(W, H); world.setMaxSize(W, H);
        buildStrobes();
        world.getChildren().addAll(stage, strobes, vocalStrobes, lights, avatars);
        vocals.strobeAlso(vocalStrobes.getChildren().stream().map(n -> (javafx.scene.shape.Shape) n)
                .toArray(javafx.scene.shape.Shape[]::new)).syncTo(ctx.audio());

        Rectangle vignette = new Rectangle(W, H);
        vignette.setMouseTransparent(true);
        vignette.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.color(0, 0, 0, 0)), new Stop(1, Color.color(0, 0, 0, 0.7))));

        buildDialog();
        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        getChildren().addAll(black, world, vignette, vocals, dialog, flash);

        setOnMouseClicked(e -> advance());
        setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) skip(); else if (e.getCode() == KeyCode.SPACE) advance(); });
        onDispose(() -> { stage.stop(); lights.stop(); sync.stop(); vocals.stop(); if (typer != null) typer.stop(); });
    }

    /**
     * Pantallas triangulares del escenario como Polygon reales (mismas coordenadas que PyramidStage, en px del lienzo
     * 160x90 × 8) por ENCIMA de los LED pixelados, en modo ADD: cuando un FillTransition las enciende, suman luz.
     */
    private void buildStrobes() {
        double[][] tris = {
                {0.17, 0.62, 0.30, 0.20, 0.30, 0.62}, {0.83, 0.62, 0.70, 0.20, 0.70, 0.62},
                {0.12, 0.17, 0.26, 0.17, 0.19, 0.38}, {0.88, 0.17, 0.74, 0.17, 0.81, 0.38},
                {0.36, 0.62, 0.40, 0.46, 0.44, 0.62}, {0.64, 0.62, 0.60, 0.46, 0.56, 0.62}};
        for (double[] t : tris) {
            double cx = (t[0] + t[2] + t[4]) / 3, cy = (t[1] + t[3] + t[5]) / 3, k = 0.88;
            Polygon p = new Polygon();
            for (int i = 0; i < 6; i += 2) p.getPoints().addAll((cx + (t[i] - cx) * k) * W, (cy + (t[i + 1] - cy) * k) * H);
            p.setFill(Color.TRANSPARENT);
            p.setBlendMode(BlendMode.ADD);
            p.setOpacity(0.75);
            p.setMouseTransparent(true);
            strobes.getChildren().add(p);
            Polygon v = new Polygon();
            v.getPoints().setAll(p.getPoints());
            v.setFill(Color.WHITE);
            v.setBlendMode(BlendMode.ADD);
            v.setOpacity(0);
            v.setMouseTransparent(true);
            vocalStrobes.getChildren().add(v);
        }
    }

    /** Beats de la canción (currentTime real); sin audio, un reloj de pared al mismo tempo para que nada se congele. */
    private double beatNow() {
        double b = ctx.audio().beats();
        if (!Double.isNaN(b)) return b;
        if (wallStart == 0) wallStart = System.nanoTime();
        return (System.nanoTime() - wallStart) / 1e6 / BEAT_MS;
    }

    /**
     * Arranca el "en vivo" enganchado a "Harder, Better, Faster, Stronger".
     * <ul>
     *   <li>Cabeceo de los robots: una TranslateTransition en el eje Y de EXACTAMENTE un beat (485.9 ms) con un
     *       interpolador de ida y vuelta ({@link BeatSync#NOD}): la cabeza baja en el golpe y sube antes del siguiente.</li>
     *   <li>Estroboscopio: cada triángulo destella un color por beat (ciclo de 3 beats); los impares, medio beat corridos.</li>
     * </ul>
     * {@link BeatSync} lee el currentTime del MediaPlayer cada frame y las recoloca (jumpTo) si se desvían más de 18 ms:
     * el tiempo 0 de cada ciclo cae SIEMPRE sobre un bombo de la canción.
     */
    private void startConcert() {
        double[] nod = {9, 6};
        Canvas[] heads = {avatarL, avatarR};
        for (int i = 0; i < 2; i++) {
            TranslateTransition bob = track(new TranslateTransition(Duration.millis(BEAT_MS), heads[i]));
            bob.setFromY(0);
            bob.setToY(nod[i]);
            bob.setCycleCount(Animation.INDEFINITE);
            bob.setInterpolator(BeatSync.NOD);
            sync.lock(bob, 1, 0);
            bob.play();
        }

        // Estroboscopio: cada triángulo destella un color por beat (cian → amarillo → magenta) con caída rápida;
        // los impares van medio beat corridos para que el escenario "ping-ponguee".
        for (int i = 0; i < strobes.getChildren().size(); i++) {
            Polygon p = (Polygon) strobes.getChildren().get(i);
            SequentialTransition seqStrobe = new SequentialTransition();
            for (int k = 0; k < STROBE.length; k++) {
                Color c = STROBE[(i + k) % STROBE.length];
                Color off = Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0);
                FillTransition hit = new FillTransition(Duration.millis(40), p, off, c);
                FillTransition decay = new FillTransition(Duration.millis(BEAT_MS - 40), p, c, off);
                decay.setInterpolator(Interpolator.EASE_OUT);
                seqStrobe.getChildren().addAll(hit, decay);
            }
            seqStrobe.setCycleCount(Animation.INDEFINITE);
            sync.lock(seqStrobe, STROBE.length, i % 2 == 1 ? 0.5 : 0);
            track(seqStrobe).play();
        }
        sync.start();
    }

    private void buildDialog() {
        dialog.setPrefSize(W, H); dialog.setMouseTransparent(true);
        Rectangle box = new Rectangle(170, 548, 940, 132);
        box.setArcWidth(14); box.setArcHeight(14);
        box.setFill(Color.web("#05060f", 0.9));
        box.setStroke(Color.web("#00f5ff"));
        box.setStrokeWidth(3);
        Label tag = Fx.label(" DAFT-BOTS ", 15, "#05060f");
        tag.setStyle("-fx-background-color: #ffe94a; -fx-padding: 2 8 2 8;");
        tag.relocate(196, 534);
        text.relocate(196, 584);
        text.setPrefWidth(890);
        prompt.relocate(900, 648);
        prompt.setOpacity(0);
        dialog.getChildren().addAll(box, tag, text, prompt);
        dialog.setOpacity(0);
    }

    @Override public Node node() { return this; }

    /** La UI base ya está oculta (fase TAKEOVER): aquí solo se monta el escenario. */
    @Override public void play(Runnable onFinished) {
        this.onFinished = onFinished;
        requestFocus();
        black.setOpacity(0); world.setOpacity(0); avatars.setOpacity(0);

        // 1-2) base -> negro -> escenario + show de luces
        Timeline startShow = Fx.cue(() -> {
            stage.start();
            lights.start();
            ctx.audio().playMusic(MusicTrack.DAFT_PUNK_THEME);       // el concierto suena; la consola sigue la misma pista
        });
        // 3) los músicos entran desde abajo
        Timeline startAvatars = Fx.cue(() -> {
            avatarLoop = track(new Timeline(new KeyFrame(Duration.millis(150), e -> { tick++; drawAvatars(); })));
            avatarLoop.setCycleCount(Animation.INDEFINITE);
            avatarLoop.play();
            drawShadows();
            startConcert();
        });
        avatars.setTranslateY(70);
        TranslateTransition rise = new TranslateTransition(Duration.millis(900), avatars);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition avatarsIn = new ParallelTransition(Fx.fade(avatars, 900, 0, 1), rise);

        // 4) zoom lento hacia los cascos: escala + traslación simultáneas (la cámara se acerca al punto medio)
        ScaleTransition scale = new ScaleTransition(Duration.millis(5200), world);
        scale.setToX(ZOOM); scale.setToY(ZOOM);
        TranslateTransition pan = new TranslateTransition(Duration.millis(5200), world);
        pan.setToX(-(HELMET_X - W / 2) * ZOOM);
        pan.setToY(-(HELMET_Y - H / 2) * ZOOM);
        ParallelTransition zoom = new ParallelTransition(scale, pan);
        zoom.setInterpolator(Interpolator.EASE_BOTH);

        seq = track(new SequentialTransition(
                Fx.fade(black, 700, 0, 1),
                startShow, Fx.fade(world, 1100, 0, 1),
                new PauseTransition(Duration.millis(1100)),
                startAvatars, avatarsIn,
                new PauseTransition(Duration.millis(900)),
                zoom,
                new PauseTransition(Duration.millis(300))));
        seq.setOnFinished(e -> startDialog());
        seq.play();
    }

    private void drawAvatars() {
        GraphicsContext l = avatarL.getGraphicsContext2D(), r = avatarR.getGraphicsContext2D();
        l.clearRect(0, 0, W, H);
        r.clearRect(0, 0, W, H);
        PixelArt.draw(l, Avatars.generate(true, tick), Avatars.PALETTE, 380, 262, 6);
        PixelArt.draw(r, Avatars.generate(false, tick), Avatars.PALETTE, 660, 262, 6);
    }

    private void drawShadows() {
        GraphicsContext g = shadows.getGraphicsContext2D();
        g.clearRect(0, 0, W, H);
        g.setFill(Color.color(0, 0, 0, 0.45));
        g.fillOval(360, 672, 280, 26); g.fillOval(640, 672, 280, 26);
    }

    // ------------------------------------------------------------------ diálogo RPG

    /** Fin del prefijo ">>> " y de cada palabra dentro de LINE: cada palabra entra en su propio golpe. */
    private static final int[] WORD_END = wordEnds(LINE);

    /** Fin de cada palabra (">>> HUMAN..." cuenta como la primera; cada espacio abre la siguiente). */
    private static int[] wordEnds(String s) {
        java.util.List<Integer> ends = new java.util.ArrayList<>();
        for (int i = 4; i < s.length(); i++) if (s.charAt(i) == ' ') ends.add(i);
        ends.add(s.length());
        return ends.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * La terminal teclea EN los golpes: arranca en el siguiente beat par y cada palabra ("HARDER...", "BETTER...",
     * "FASTER...", "ENCALETADO") aparece en un beat, tecleada en sus primeros 150 ms. Lo mueve un AnimationTimer que lee
     * la posición real de la canción en cada frame (no un Timeline): imposible que se adelante o se atrase.
     */
    private void startDialog() {
        if (state != State.CINEMA) return;
        state = State.TYPING;
        Fx.fade(dialog, 400, 0, 1).play();
        double b = beatNow();
        typeFrom = Math.ceil(b + 0.6);                              // margen para el fundido del cuadro de diálogo
        if (((long) typeFrom) % 2 != 0) typeFrom += 1;
        typer = new AnimationTimer() {
            @Override public void handle(long now) {
                double rel = beatNow() - typeFrom;
                if (rel < 0) return;
                int word = (int) Math.floor(rel);
                if (word >= WORD_END.length) { text.setText(LINE); stop(); waitForClick(); return; }
                int start = word == 0 ? 0 : WORD_END[word - 1];
                double u = Math.min(1, (rel - word) * BEAT_MS / 150);
                text.setText(LINE.substring(0, start + (int) Math.round((WORD_END[word] - start) * u)));
            }
        };
        typer.start();
    }

    private void waitForClick() {
        if (state != State.TYPING) return;
        state = State.WAITING;
        Timeline blink = track(new Timeline(new KeyFrame(Duration.millis(450), e -> prompt.setOpacity(prompt.getOpacity() > 0.5 ? 0.15 : 1))));
        blink.setCycleCount(Animation.INDEFINITE);
        blink.play();
        wait = track(new PauseTransition(Duration.seconds(6)));
        wait.setOnFinished(e -> finish());
        wait.play();
    }

    /** Clic o ESPACIO: acelera la cinemática, completa el texto o continúa, según el momento. */
    private void advance() {
        switch (state) {
            case CINEMA -> { if (seq != null) seq.jumpTo(seq.getTotalDuration()); }
            case TYPING -> { if (typer != null) typer.stop(); text.setText(LINE); waitForClick(); }
            case WAITING -> { if (wait != null) wait.stop(); finish(); }
            default -> { }
        }
    }

    private void skip() {
        if (state == State.DONE) return;
        if (seq != null) seq.stop();
        if (typer != null) typer.stop();
        if (wait != null) wait.stop();
        state = State.WAITING;
        finish();
    }

    /** Fin: destello cian que tapa la pantalla y aviso al orquestador (que monta la consola DEBAJO de esta vista). */
    private void finish() {
        if (state == State.DONE) return;
        state = State.DONE;
        flash.setOpacity(0.95);
        onFinished.run();
    }

    /** El destello se apaga (500 ms) mientras toda la vista se desvanece (700 ms) revelando la consola. */
    @Override public void dissolve() {
        setMouseTransparent(true);
        ParallelTransition out = new ParallelTransition(Fx.fade(flash, 500, 0.95, 0), Fx.fade(this, 700, 1, 0));
        out.setOnFinished(e -> disposeAndRemove());
        out.play();
    }

    @Override public void stop() {
        state = State.DONE;
        disposeAndRemove();
    }
}
