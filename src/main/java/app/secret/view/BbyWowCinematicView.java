package app.secret.view;

import app.animations.FpsProbe;
import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.AudioManager;
import app.secret.audio.MusicTrack;
import app.secret.audio.SongCues;
import app.secret.ui.LyricFireworks;
import app.secret.ui.MusicReactor;
import app.secret.ui.SpectrumWave;
import app.secret.ui.WigSpider;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BBYWOW — EL CONCIERTO. Una sola escena anclada al currentTime REAL de "BbY WOW" (MusicTrack.BBYWOW_THEME): los
 * momentos grandes y cada frase son cues de {@link SongCues} ({@code currentTimeProperty().addListener} vía
 * AudioManager.onMusicTime) y todo lo continuo (descenso, baile, público, láseres, fuegos) lo mueve UN
 * {@link AnimationTimer} leyendo esa misma posición.
 * <pre>
 *    0.0 s   apagón total (la interfaz ya desapareció en el takeover): 1 s de negro
 *    1.0 s   se enciende un cañón de luz y El Guachimán "bichota" (coletas azules, {@link WigSpider}) baja muy despacio
 *   28.9 s   se cortan los hi-hats (el "ouh ah ah ah"): el descenso se detiene; tiembla colgado y empieza a cantar
 *   31.86 s  EL DROP (la mezcla calla 60 ms y revienta): FLASHAZO blanco que se disipa revelando el estadio
 *   31.86–69.6 s  CONCIERTO: canta frente al micrófono (la boca sigue la voz real del espectro), público saltando a
 *            tempo, mini arañas con coletas bailando, láseres en mezcla ADD que parpadean en cada golpe, pantallas
 *            gigantes con el espectro en vivo y FUEGOS QUE ESCRIBEN ({@link LyricFireworks}): por cada frase sale un
 *            cohete del público que explota justo en el milisegundo de la frase y la revela en el cielo
 *   69.6 s   (≈1:09) el bajo se apaga de golpe: APAGÓN, la música se corta y se pide la calle (El Mordisco + péndulo)
 * </pre>
 * <h3>Frases</h3>
 * Si existe {@code src/main/resources/lyrics/bbywow.lrc} ({@code [mm:ss.cc] texto} por línea), cada línea es un
 * cohete que explota en su tiempo exacto. Sin ese archivo suenan las dos marcas dadas para la escena ("ouh ah ah ah"
 * en el quiebre y "me gustas demasiao" al cierre) y, entre ellas, frases del show en las barras de la canción.
 *
 * <h3>Rendimiento (≥ 60 FPS medidos)</h3>
 * Fondo pintado UNA vez; láseres = rectángulos que solo giran (quads en GPU); público = ~120 drawImage de texturas
 * pre-dibujadas; chispas en un Canvas con alfa global; mini arañas = texturas; ningún efecto que se recalcule por frame.
 */
public final class BbyWowCinematicView implements Cinematic {

    private static final double W = TakeoverView.W, H = TakeoverView.H;
    private static final MusicTrack SONG = MusicTrack.BBYWOW_THEME;
    private static final double BEAT = SONG.beatMs(), FIRST = SONG.firstBeatMs, BAR = 4 * BEAT;

    // ---- guion (ms dentro de la canción, medidos sobre el MP3)
    private static final double LIGHT_ON = 1_000;                 // 1 s de negro
    private static final double BREAK = 28_900;                   // se cortan los hi-hats: fin del descenso
    private static final double DROP = SONG.dropMs;               // 31.860 s
    private static final double[][] BASS_OFF = {{45_900, 47_900}, {61_900, 62_900}};
    private static final double DROP2 = 47_900, DROP3 = 62_900;
    private static final double END = 69_600;                     // ≈1:09: el bajo se apaga de golpe
    private static final double FLIGHT_MS = 1_000;                // lo que tarda un cohete en llegar a su punto máximo

    // ---- posiciones
    private static final double SPIDER_PX = 4, SW = WigSpider.GW * SPIDER_PX, SH = WigSpider.GH * SPIDER_PX;
    private static final double SPIDER_X = W / 2 - SW / 2, HANG_Y = 190, STAGE_Y = 290, FLOOR = 540;
    private static final Color[] LASER = {Color.web("#00e5ff"), Color.web("#ff2bd6"), Color.web("#2b6bff"), Color.web("#39ff14")};
    private static final double[][] EMIT = {{360, FLOOR - 20}, {920, FLOOR - 20}, {W / 2, 78}};

    private final SecretContext ctx;
    private final AudioManager audio;
    private final Random rnd = new Random(11);

    private final Pane root = new Pane();
    private final Group concert = new Group();
    private final Canvas dust = new Canvas(W, H);
    private final Canvas crowd = new Canvas(W, 200);
    private final LyricFireworks fireworks = new LyricFireworks(W, H);
    private final Group lasers = new Group();
    private final Rectangle[] beams = new Rectangle[10];
    private final Rotate[] beamAngle = new Rotate[10];
    private final Circle backdrop = new Circle(W / 2, 300, 220);
    private final SpectrumWave screenL = screen(), screenR = screen();
    private final Polygon spot = new Polygon(588, -10, 692, -10, 880, H, 400, H);
    private final Line thread = new Line(W / 2, -10, W / 2, -10);
    private final WigSpider spider = new WigSpider(SPIDER_PX);
    private final Group mic = new Group();
    private final List<ImageView> minis = new ArrayList<>();
    private final Image[] miniFrames = {WigSpider.image(1.5, 0, false), WigSpider.image(1.5, 1, true)};
    private final Image[][] fans = bakeFans();
    private final double[][] people = new double[120][6];        // x, fila, tamaño, desfase, brazos(0/1), celular(0/1)
    private final double[][] motes = new double[70][3];
    private final Rectangle flash = new Rectangle(W, H, Color.WHITE);
    private final Rectangle blackout = new Rectangle(W, H, Color.BLACK);
    private final FadeTransition laserBlink = new FadeTransition(Duration.millis(420), lasers);

    private double songMs = -1, voice, voiceFloor = 0.2;
    private boolean wallClock, revealed, ended, stopped;
    private long wallT0;
    private int lastBeat = Integer.MIN_VALUE, phraseTurn;
    private Runnable untapTime, untapVoice, stopCues;
    private MusicReactor reactor;
    private AnimationTimer director;

    public BbyWowCinematicView(SecretContext ctx) {
        this.ctx = ctx;
        this.audio = ctx.audio();
        build();
    }

    @Override public Node node() { return root; }

    // ------------------------------------------------------------------ montaje

    private void build() {
        root.setMinSize(W, H); root.setPrefSize(W, H); root.setMaxSize(W, H);
        root.setClip(new Rectangle(W, H));
        root.setStyle("-fx-background-color: black;");
        root.setPickOnBounds(true);

        Canvas stadium = new Canvas(W, H);
        paintStadium(stadium.getGraphicsContext2D());
        backdrop.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1fb8c8", 0.55)), new Stop(0.6, Color.web("#ff2bd6", 0.18)), new Stop(1, Color.TRANSPARENT)));
        // Haz de láser = rectángulo largo y fino que SOLO gira (Rotate con pivote en el emisor): la GPU lo dibuja como
        // un quad transformado. Un Polygon que cambia de puntos en cada frame obliga a re-rasterizarlo en CPU (medido:
        // los 10 haces costaban ~30 FPS).
        for (int i = 0; i < beams.length; i++) {
            double[] e = EMIT[i % EMIT.length];
            Color c = LASER[i % LASER.length];
            beams[i] = new Rectangle(1500, 5);
            beams[i].setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, c.deriveColor(0, 1, 1, 0.95)), new Stop(1, c.deriveColor(0, 1, 1, 0))));
            beams[i].setLayoutX(e[0]);
            beams[i].setLayoutY(e[1] - 2.5);
            beamAngle[i] = new Rotate(0, 0, 2.5);
            beams[i].getTransforms().add(beamAngle[i]);
            lasers.getChildren().add(beams[i]);
        }
        lasers.setBlendMode(BlendMode.ADD);
        lasers.setMouseTransparent(true);
        laserBlink.setFromValue(1);
        laserBlink.setToValue(0.4);
        screenL.relocate(40, 150);
        screenR.relocate(980, 150);
        buildMinis();
        buildMic();
        concert.getChildren().addAll(stadium, backdrop, screenL, screenR, lasers);
        concert.getChildren().addAll(minis);
        concert.setOpacity(0);

        spot.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#d8f6ff", 0.30)), new Stop(1, Color.web("#1fb8c8", 0.02))));
        spot.setOpacity(0);
        thread.setStroke(Color.web("#e8f6ff", 0.8));
        thread.setStrokeWidth(2);
        spider.relocate(SPIDER_X, -SH - 20);
        mic.setOpacity(0);
        crowd.setLayoutY(H - 200);
        crowd.setOpacity(0);
        flash.setOpacity(0);
        blackout.setOpacity(0);
        for (Node n : new Node[]{dust, crowd, flash, blackout, spot, thread, mic}) n.setMouseTransparent(true);

        root.getChildren().addAll(concert, spot, dust, thread, spider, mic, crowd, fireworks, flash, blackout);
        seedCrowd();
        for (double[] d : motes) { d[0] = 420 + rnd.nextDouble() * 440; d[1] = rnd.nextDouble() * H; d[2] = 0.4 + rnd.nextDouble(); }
    }

    @Override public void play(Runnable onFinished) {
        // onFinished no se usa: el concierto termina pidiendo la calle (ctx.requestStreet) en el apagón final
        FpsProbe.scene("bbywow");
        reactor = new MusicReactor(audio, SONG).heartbeat(backdrop, 0.08).wave(screenL).wave(screenR);
        reactor.start();
        stopCues = cues().start(audio, SONG, 0);
        if (audio != null) {
            untapTime = audio.onMusicTime(SONG, ms -> songMs = ms);
            untapVoice = audio.onMusicVoiceLevel(SONG, v -> voice += (v - voice) * 0.5);
            audio.whenMusicPlaying(pos -> { if (pos < 0) useWallClock(); });
        } else {
            useWallClock();
        }
        director = new AnimationTimer() {
            private long last;
            @Override public void handle(long now) {
                double dt = last == 0 ? 1 / 60.0 : Math.min(0.1, (now - last) / 1e9);
                last = now;
                frame(now, dt);
            }
        };
        director.start();
        root.requestFocus();
    }

    private void useWallClock() { if (!wallClock) { wallClock = true; wallT0 = System.nanoTime(); } }

    /** Los momentos grandes y las frases, cada uno en su milisegundo de la canción. */
    private SongCues cues() {
        SongCues c = new SongCues()
                .at(LIGHT_ON, () -> fade(spot, 1500, 1))
                .at(DROP, this::reveal)
                .at(DROP2, () -> { salvo(7); punch(); })
                .at(DROP3, () -> { salvo(8); punch(); })
                .at(END, this::lightsOut);
        // mini arañas: aparecen de a una en las barras tras el drop (las de los parlantes, en el re-drop)
        double[] appear = {DROP + BAR / 2, DROP + BAR, DROP + 3 * BAR / 2, DROP + 2 * BAR, DROP2, DROP2 + BAR / 2};
        for (int i = 0; i < minis.size(); i++) {
            ImageView m = minis.get(i);
            c.at(appear[i], () -> popIn(m));
        }
        // fuegos que escriben: el cohete sale FLIGHT_MS antes y explota en el milisegundo de la frase
        List<double[]> times = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        loadPhrases(times, lines);
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            double at = times.get(i)[0], launch = Math.max(0, at - FLIGHT_MS), flight = at - launch;
            c.at(launch, () -> phraseRocket(s, flight));
        }
        return c;
    }

    /**
     * Frases de la escena. Con {@code /lyrics/bbywow.lrc} (letra que pone el usuario) se usan sus líneas tal cual;
     * sin él, las dos marcas dadas para la escena y frases del show en las barras de la canción.
     */
    private static void loadPhrases(List<double[]> times, List<String> lines) {
        try (InputStream in = BbyWowCinematicView.class.getResourceAsStream("/lyrics/bbywow.lrc")) {
            if (in != null) {
                Pattern p = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]\\s*(.*)");
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String l; (l = r.readLine()) != null; ) {
                    Matcher m = p.matcher(l.trim());
                    if (!m.matches() || m.group(3).isBlank()) continue;
                    double ms = (Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2))) * 1000;
                    if (ms < END) { times.add(new double[]{ms}); lines.add(m.group(3).trim()); }
                }
                if (!lines.isEmpty()) return;
            }
        } catch (IOException | RuntimeException ignored) { /* sin archivo: frases por defecto */ }
        Object[][] fallback = {
                {29_400.0, "ouh ah ah ah"},                                // el quiebre (hi-hats fuera)
                {DROP, "BBY WOW"}, {DROP + BAR, "¡WOW!"}, {DROP + 2 * BAR, "EL GUACHIMÁN"}, {DROP + 3 * BAR, "EN VIVO"},
                {DROP2, "BBY WOW"}, {DROP2 + BAR, "BICHOTA"}, {DROP2 + 2 * BAR, "¡QUÉ NIVEL!"}, {DROP2 + 3 * BAR, "MINISTERIO LIVE"},
                {DROP3, "¡WOW!"}, {DROP3 + BAR, "PELUCA AZUL"},
                {68_900.0, "me gustas demasiao"}};                        // el cierre, justo antes del apagón
        for (Object[] f : fallback) { times.add(new double[]{(Double) f[0]}); lines.add((String) f[1]); }
    }

    /** Un cohete sale del público y explota en el cielo del escenario revelando la frase (alterna izquierda/derecha). */
    private void phraseRocket(String text, double flightMs) {
        double[] xs = {640, 400, 880, 520, 760};
        double x = xs[phraseTurn % xs.length], y = 150 + (phraseTurn % 3) * 40;
        phraseTurn++;
        fireworks.launchPhrase(text, x + (rnd.nextDouble() - 0.5) * 160, H - 70, x, y, flightMs);
    }

    private void salvo(int n) {
        for (int i = 0; i < n; i++) {
            double x = 140 + (W - 280) * (i + 0.5) / n;
            fireworks.launchSparkle(x + (rnd.nextDouble() - 0.5) * 60, H - 60, x, 110 + rnd.nextDouble() * 140, 700 + rnd.nextDouble() * 250);
        }
    }

    // ------------------------------------------------------------------ el director

    private void frame(long now, double dt) {
        double ms = wallClock ? (now - wallT0) / 1e6 : songMs;
        if (ms < 0 || stopped) return;
        double beats = (ms - FIRST) / BEAT, frac = beats - Math.floor(beats);
        int beat = (int) Math.floor(beats);
        boolean calm = inBassBreak(ms);
        voiceFloor += (voice - voiceFloor) * Math.min(1, dt * 0.8);
        boolean singing = ms >= BREAK && voice > voiceFloor + 0.045;
        fireworks.step(dt);

        if (!revealed) {
            // ---- INTRO: el descenso (y, desde el quiebre, cuelga temblando y canta)
            double u = Math.max(0, Math.min(1, (ms - LIGHT_ON) / (BREAK - LIGHT_ON)));
            double ease = 0.5 - 0.5 * Math.cos(Math.PI * u);
            double y = -SH - 20 + (HANG_Y + SH + 20) * ease;
            double tremble = ms >= BREAK ? 2.2 * Math.sin(ms / 23.0) : 0;
            spider.setLayoutY(y);
            spider.setTranslateX(tremble);
            spider.setRotate(3 * Math.sin(ms / 900.0));
            double zoom = ms >= BREAK ? 1 + 0.08 * Math.min(1, (ms - BREAK) / (DROP - BREAK)) : 1;
            spider.setScaleX(zoom); spider.setScaleY(zoom);
            thread.setEndY(y + 8 * SPIDER_PX);
            thread.setStartX(W / 2 + tremble * 0.3);
            spider.pose((int) (ms / 600) % 2, singing);
            drawDust(dt, ms);
            return;
        }

        // ---- CONCIERTO
        if (beat != lastBeat) {                                          // en cada golpe parpadean los láseres
            lastBeat = beat;
            if (!calm) laserBlink.playFromStart();
        }
        double bounce = Math.abs(Math.sin(Math.PI * frac));
        spider.setLayoutY(STAGE_Y - (calm ? 2 : 6) * bounce);
        spider.setRotate(4 * Math.sin(Math.PI * beats / 2));
        spider.pose(beat % 2 == 0 ? 0 : 1, singing);
        for (int i = 0; i < minis.size(); i++) {
            ImageView m = minis.get(i);
            double side = i % 2 == 0 ? 1 : -1, f = frac + (i % 2) * 0.5;
            m.setTranslateY(-12 * Math.abs(Math.sin(Math.PI * f)));
            m.setRotate(side * 9 * Math.sin(Math.PI * (beats + i * 0.5)));
            m.setImage(miniFrames[Math.floorMod(beat + i, 2)]);
        }
        aimLasers(ms / 1000, calm);
        drawCrowd(ms, beats, calm);
    }

    private static boolean inBassBreak(double ms) {
        for (double[] b : BASS_OFF) if (ms >= b[0] && ms < b[1]) return true;
        return false;
    }

    // ------------------------------------------------------------------ fases

    /** EL DROP: flashazo blanco que se disipa y deja ver el estadio con El Guachimán ya frente al micrófono. */
    private void reveal() {
        revealed = true;
        flash.setOpacity(1);
        FadeTransition f = new FadeTransition(Duration.millis(900), flash);
        f.setToValue(0);
        f.setInterpolator(Interpolator.EASE_OUT);
        f.play();
        concert.setOpacity(1);
        crowd.setOpacity(1);
        mic.setOpacity(1);
        spot.setVisible(false);
        thread.setVisible(false);
        dust.setVisible(false);
        spider.relocate(SPIDER_X, STAGE_Y);
        spider.setTranslateX(0);
        spider.setScaleX(1); spider.setScaleY(1);
        salvo(8);
    }

    /** Re-drop: el estadio da un golpe de escala. */
    private void punch() {
        ScaleTransition s = new ScaleTransition(Duration.millis(320), concert);
        s.setFromX(1.04); s.setFromY(1.04);
        s.setToX(1); s.setToY(1);
        s.setInterpolator(Interpolator.EASE_OUT);
        s.play();
    }

    /** ≈1:09: apagón (luces fuera y la música con ellas) y de vuelta a la calle con El Mordisco + péndulo. */
    private void lightsOut() {
        if (ended) return;
        ended = true;
        FadeTransition black = new FadeTransition(Duration.millis(120), blackout);
        black.setToValue(1);
        black.play();
        if (audio != null) audio.stopMusic(150);
        PauseTransition hold = new PauseTransition(Duration.millis(550));
        hold.setOnFinished(e -> { if (!stopped) ctx.requestStreet(); });
        hold.play();
    }

    // ------------------------------------------------------------------ escenario (se pinta una vez)

    private static void paintStadium(GraphicsContext g) {
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#04010d")), new Stop(0.55, Color.web("#170a33")), new Stop(1, Color.web("#2a0f45"))));
        g.fillRect(0, 0, W, H);
        // gradas del estadio con luces de celulares
        g.setFill(Color.web("#0d0820"));
        g.fillPolygon(new double[]{0, 360, 330, 0}, new double[]{250, 380, 560, 560}, 4);
        g.fillPolygon(new double[]{W, W - 360, W - 330, W}, new double[]{250, 380, 560, 560}, 4);
        Color[] lights = {Color.web("#00e5ff"), Color.web("#ff2bd6"), Color.web("#ffe600"), Color.WHITE};
        Random r = new Random(4);
        for (int i = 0; i < 520; i++) {
            boolean left = i % 2 == 0;
            double u = r.nextDouble(), v = r.nextDouble();
            double x = left ? u * 340 : W - u * 340;
            double top = 250 + 130 * u, y = top + v * (560 - top);
            g.setFill(lights[r.nextInt(lights.length)].deriveColor(0, 1, 1, 0.25 + r.nextDouble() * 0.5));
            g.fillOval(x, y, 2.2, 2.2);
        }
        // estructura (truss): torres y viga superior en zigzag
        g.setStroke(Color.web("#3a3a4e"));
        g.setLineWidth(3);
        for (double tx : new double[]{318, 942}) {
            g.strokeLine(tx, 56, tx, FLOOR);
            g.strokeLine(tx + 20, 56, tx + 20, FLOOR);
            for (double y = 56; y < FLOOR; y += 24) g.strokeLine(tx, y, tx + 20, y + 24);
        }
        g.strokeLine(318, 56, 962, 56);
        g.strokeLine(318, 78, 962, 78);
        for (double x = 318; x < 962; x += 24) g.strokeLine(x, 56, x + 24, 78);
        for (double x = 360; x < 940; x += 72) {                              // focos colgados de la viga
            g.setFill(Color.web("#1d1d28"));
            g.fillRect(x, 78, 22, 16);
            g.setFill(Color.web("#fff3c4", 0.9));
            g.fillOval(x + 6, 88, 10, 8);
        }
        for (double sx : new double[]{40, 980}) {                             // marcos de las pantallas gigantes
            g.setFill(Color.web("#2a2a36"));
            g.fillRect(sx - 8, 142, 276, 166);
            g.setFill(Color.web("#07071a"));
            g.fillRect(sx, 150, 260, 150);
        }
        for (double px : new double[]{150, 1030}) {                           // parlantes
            g.setFill(Color.web("#111018"));
            g.fillRect(px, 440, 100, 100);
            g.setFill(Color.web("#24222e"));
            g.fillOval(px + 22, 452, 56, 56);
            g.fillOval(px + 36, 512, 28, 24);
        }
        g.setFill(Color.web("#120c1c"));                                      // tarima con borde de luz
        g.fillPolygon(new double[]{280, 1000, 1080, 200}, new double[]{FLOOR - 20, FLOOR - 20, H, H}, 4);
        g.setStroke(Color.web("#1fb8c8"));
        g.setLineWidth(3);
        g.strokeLine(280, FLOOR - 20, 1000, FLOOR - 20);
        g.setStroke(Color.web("#ff2bd6", 0.6));
        g.setLineWidth(2);
        g.strokeLine(268, FLOOR - 14, 1012, FLOOR - 14);
    }

    /** Pantalla gigante: el espectro de la canción en vivo (sin texto). */
    private static SpectrumWave screen() {
        return new SpectrumWave(260, 150, Color.web("#2b6bff"), Color.web("#1fb8c8"), Color.web("#ff2bd6"));
    }

    private void buildMic() {
        double mouthY = STAGE_Y + 37 * SPIDER_PX;                             // la boca del sprite (fila 37)
        Line stand = new Line(688, FLOOR - 20, 662, mouthY + 8);
        stand.setStroke(Color.web("#bfc3cc"));
        stand.setStrokeWidth(4);
        stand.setStrokeLineCap(StrokeLineCap.ROUND);
        Line foot1 = new Line(688, FLOOR - 20, 664, FLOOR - 6), foot2 = new Line(688, FLOOR - 20, 712, FLOOR - 6);
        for (Line l : new Line[]{foot1, foot2}) { l.setStroke(Color.web("#8a8f99")); l.setStrokeWidth(4); }
        Circle head = new Circle(658, mouthY, 9, Color.web("#2b2b33"));
        head.setStroke(Color.web("#d7dbe3"));
        head.setStrokeWidth(2);
        mic.getChildren().addAll(stand, foot1, foot2, head);
    }

    private void buildMinis() {
        double[][] at = {{380, FLOOR - 20}, {900, FLOOR - 20}, {470, FLOOR - 20}, {810, FLOOR - 20}, {200, 440}, {1080, 440}};
        for (double[] p : at) {
            ImageView v = new ImageView(miniFrames[0]);
            v.setLayoutX(p[0] - miniFrames[0].getWidth() / 2);
            v.setLayoutY(p[1] - miniFrames[0].getHeight());
            v.setScaleX(0); v.setScaleY(0);
            minis.add(v);
        }
    }

    private static void popIn(Node n) {
        ScaleTransition s = new ScaleTransition(Duration.millis(380), n);
        s.setFromX(0); s.setFromY(0);
        s.setToX(1); s.setToY(1);
        s.setInterpolator(Interpolator.EASE_OUT);
        s.play();
    }

    private void aimLasers(double t, boolean calm) {
        for (int i = 0; i < beams.length; i++) {
            double base = EMIT[i % EMIT.length][1] < 200 ? 90 : -90;          // de arriba hacia abajo / del piso al cielo
            beamAngle[i].setAngle(base + (calm ? 20 : 52) * Math.sin(t * (0.9 + i * 0.13) + i * 1.7));
        }
        if (calm) lasers.setOpacity(0.35);
    }

    /** Polvo flotando dentro del cañón de luz durante el descenso. */
    private void drawDust(double dt, double ms) {
        GraphicsContext g = dust.getGraphicsContext2D();
        g.clearRect(0, 0, W, H);
        if (ms < LIGHT_ON) return;
        g.setFill(Color.web("#d8f6ff"));
        double a = Math.min(1, (ms - LIGHT_ON) / 2000);
        for (double[] d : motes) {
            d[1] -= 9 * d[2] * dt;
            d[0] += 4 * Math.sin(ms / 1300.0 + d[2] * 5) * dt;
            if (d[1] < -4) d[1] = H + 4;
            double halfCone = 52 + (d[1] / H) * 188;                          // solo dentro del cono de luz
            if (Math.abs(d[0] - W / 2) > halfCone) continue;
            g.setGlobalAlpha(0.35 * a * d[2] / 1.4);
            g.fillRect(d[0], d[1], 2.2 * d[2], 2.2 * d[2]);
        }
        g.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------ público

    private void seedCrowd() {
        for (int i = 0; i < people.length; i++) {
            double[] p = people[i];
            p[1] = i % 3;
            p[0] = (i / 3) * (W / 40.0) + rnd.nextDouble() * 18 - 9 + p[1] * 11;
            p[2] = 0.8 + p[1] * 0.18 + rnd.nextDouble() * 0.15;
            p[3] = rnd.nextDouble() * 0.22;
            p[4] = rnd.nextDouble() < 0.35 ? 1 : 0;
            p[5] = rnd.nextDouble() < 0.15 ? 1 : 0;
        }
    }

    /**
     * Público: cada persona es una TEXTURA pre-dibujada (silueta + contraluz del color del láser; brazos abajo, arriba,
     * o arriba con celular): ~120 drawImage por frame en vez de ~600 trazos vectoriales rasterizados en CPU (medido: el
     * público vectorial costaba ~30 FPS). Saltan a tempo, cada uno con su pequeño desfase.
     */
    private void drawCrowd(double ms, double beats, boolean calm) {
        GraphicsContext g = crowd.getGraphicsContext2D();
        g.clearRect(0, 0, W, 200);
        int tone = Math.floorMod((int) Math.floor(beats / 2), LASER.length);
        for (int row = 0; row < 3; row++) {
            for (double[] p : people) {
                if (p[1] != row) continue;
                double f = beats + p[3];
                double jump = calm ? 3 * Math.sin(ms / 700.0 + p[0]) : (10 + row * 6) * Math.abs(Math.sin(Math.PI * (f - Math.floor(f))));
                double s = p[2], x = p[0] + (calm ? 5 * Math.sin(ms / 520.0 + p[0]) : 0), y = 70 + row * 46 - jump;
                int pose = calm || p[4] > 0 ? (p[5] > 0 ? 2 : 1) : 0;             // en el corte, todos con los brazos arriba
                g.drawImage(fans[tone][pose], x - FAN_CX * s, y - FAN_CY * s, FAN_W * s, FAN_H * s);
            }
        }
    }

    private static final double FAN_W = 64, FAN_H = 124, FAN_CX = 32, FAN_CY = 44;

    /** Las 12 texturas del público: 4 colores de contraluz × (brazos abajo / arriba / arriba con celular). */
    private static Image[][] bakeFans() {
        Image[][] out = new Image[LASER.length][3];
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        Color body = Color.web("#0b0714");
        for (int c = 0; c < LASER.length; c++)
            for (int pose = 0; pose < 3; pose++) {
                Canvas cv = new Canvas(FAN_W, FAN_H);
                GraphicsContext g = cv.getGraphicsContext2D();
                if (pose > 0) {
                    g.setStroke(body);
                    g.setLineWidth(5);
                    g.setLineCap(StrokeLineCap.ROUND);
                    g.strokeLine(FAN_CX - 12, FAN_CY + 18, FAN_CX - 20, FAN_CY - 26);
                    g.strokeLine(FAN_CX + 12, FAN_CY + 18, FAN_CX + 20, FAN_CY - 26);
                    if (pose == 2) {
                        g.setFill(Color.web("#e9fbff", 0.9));
                        g.fillRect(FAN_CX + 17, FAN_CY - 40, 7, 11);
                    }
                }
                g.setFill(body);
                g.fillOval(FAN_CX - 22, FAN_CY + 14, 44, 60);                    // hombros
                g.fillOval(FAN_CX - 11, FAN_CY - 7, 22, 22);                     // cabeza
                g.setStroke(LASER[c].deriveColor(0, 1, 1, 0.6));
                g.setLineWidth(1.8);
                g.strokeArc(FAN_CX - 11, FAN_CY - 7, 22, 22, 30, 120, ArcType.OPEN);
                out[c][pose] = cv.snapshot(sp, null);
            }
        return out;
    }

    // ------------------------------------------------------------------ Cinematic

    @Override public void dissolve() { stop(); }

    @Override public void stop() {
        if (stopped) return;
        stopped = true;
        if (director != null) director.stop();
        if (reactor != null) reactor.stop();
        if (stopCues != null) stopCues.run();
        if (untapTime != null) untapTime.run();
        if (untapVoice != null) untapVoice.run();
        laserBlink.stop();
        fireworks.clear();
        if (root.getParent() instanceof Pane p) p.getChildren().remove(root);
    }

    private static void fade(Node n, double ms, double to) {
        FadeTransition f = new FadeTransition(Duration.millis(ms), n);
        f.setToValue(to);
        f.play();
    }
}
