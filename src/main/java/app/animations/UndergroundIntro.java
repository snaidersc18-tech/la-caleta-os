package app.animations;

import app.secret.audio.AudioManager;
import app.secret.audio.MusicTrack;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cinemática de arranque "HACKER meets UNDERGROUND COLOMBIANO", dibujada sobre el telón negro que Main pone encima de
 * la interfaz desde el FRAME 0.
 * <pre>
 *   FASE 1 (reloj de pared)
 *   0.0 s   terminal hacker: lluvia de código binario verde + una consola que arranca el sistema
 *   1.7 s   "[!!] alguien se metió a la consola": entra una lata de aerosol agitándose (AudioManager.playIntro)
 *   2.95 s  el graffiti VANDALIZA la terminal: "MINISTERIO" / "DEL REBUSQUE" en tres trazos con niebla de pintura
 *   FASE 2  5.39 s, el último trazo: TODO se congela y en ese mismo frame suena Alcolirykoz (AudioManager.playStreetTheme)
 *   FASE 3 (reloj = currentTime REAL de la canción): la intro hablada de Jaime Garzón. El fondo se oscurece, la lluvia
 *           sigue en cámara lenta, el tag palpita con el nivel de la voz (AudioSpectrumListener), chorrea la pintura y la
 *           terminal inferior transcribe el homenaje bloque por bloque de voz
 *   FASE 4  currentTime ≥ MusicTrack.CALLE_THEME.dropMs (14.060 s): cae el boom-bap
 *   FASE 5  FLASH verde-blanco → onReveal: Main disipa el telón y la araña se "sella" en su marco
 * </pre>
 * Sin audio (falta el MP3 o no hay módulo de medios) la fase 3 corre con el reloj de pared desde el congelado: la
 * cinemática nunca se queda colgada esperando una canción que no va a sonar.
 *
 * <h3>Rendimiento (60 FPS)</h3>
 * Medido con {@code -Drebusque.fps=true}: la versión anterior corría a 3-4 FPS. La causa NO era el hilo de
 * animación sino el raster: seis Text de 150 px con trazos OUTSIDE de 15 px y DropShadow se re-rasterizaban en CPU
 * (Marlin) en CADA frame, porque la lluvia ensuciaba toda la pantalla (aunque el clip del tag tuviera ancho 0).
 * Ahora:
 * <ul>
 *   <li><b>Un solo {@link AnimationTimer}</b> ("director") mueve todo con el tiempo real transcurrido: lluvia,
 *       tecleo de la terminal, disparo de fases, trazos del aerosol, mano/lata, niebla y pulso del beat. Nada de
 *       Timelines con cientos de KeyFrames ni intervalos fijos: si un frame llega tarde, la animación no se retrasa.</li>
 *   <li><b>El tag se rasteriza UNA vez</b> (snapshot → {@link ImageView}). Pintarlo es mover el ancho de un clip sobre
 *       una textura, y el "latido" anima la opacidad de una capa de brillo pre-desenfocada, no el radio de un efecto.</li>
 *   <li><b>La lluvia usa un atlas de glifos</b>: cada carácter es un drawImage de una textura pre-renderizada, no un
 *       fillText (que maqueta texto en cada llamada).</li>
 *   <li>La terminal y la corona quedan en caché de GPU (setCache + CacheHint.SPEED) cuando ya no cambian.</li>
 * </ul>
 * ESPACIO / ENTER / ESC o un clic la saltan (directo al flash).
 */
public final class UndergroundIntro {

    private static final double W = 1280, H = 720;
    private static final Color NEON = Color.web("#39ff14");
    private static final Color NEON_DEEP = Color.web("#0f9d2c");
    private static final Color MATRIX = Color.web("#00ff41");
    // guion de la fase 1 (segundos desde el frame 0)
    private static final double VANDAL_AT = 1.7;
    private static final double PAINT_AT = VANDAL_AT + AudioManager.INTRO_PAINT_AT_MS / 1000.0;
    private static final double PAINT_S = 2.44;                  // los tres trazos de paintStrokes() terminan aquí
    private static final double FREEZE_AT = PAINT_AT + PAINT_S;
    private static final double TYPE_CPS = 140;                   // caracteres por segundo de la terminal
    // fases 3-5: milisegundos DENTRO de la canción (medidos sobre el MP3, ver MusicTrack.CALLE_THEME)
    private static final double DROP_MS = MusicTrack.CALLE_THEME.dropMs;
    private static final double RAIN_SLOWMO = 0.18;              // la lluvia durante la voz: cámara lenta

    /**
     * Terminal inferior: una línea por bloque de voz de la intro hablada (inicio y fin de cada bloque medidos sobre
     * el MP3: 0.0–5.9 s y 6.3–14.0 s). Cada línea se teclea a lo largo de su bloque, así que el tecleo va y para con
     * la voz. Es un homenaje (no una transcripción): para mostrar las palabras exactas basta cambiar los textos.
     */
    private record Caption(double fromMs, double toMs, String text) { }
    private static final Caption[] GARZON = {
            new Caption(350, 5_900, "> SEÑAL ENTRANTE · archivo de voz recuperado: JAIME GARZÓN (1960-1999)"),
            new Caption(6_280, 13_700, "> en memoria del humorista que le habló de frente al poder."),
    };

    private static final String[] BOOT = {
            "C:\\MINISTERIO> boot rebusque.sys",
            "[ OK ] núcleo AES-256-GCM ............ cargado",
            "[ OK ] esteganografía LSB ............ lista",
            "[ OK ] guachiman.exe ................. vigilando",
            "[ !! ] ALERTA: alguien se metió a la consola..."
    };

    // lluvia: atlas de glifos (columnas = carácter, filas = cabeza blanca / verde brillante / verde medio)
    private static final String GLYPHS = "0101010101ABCDEF#$%&";
    private static final double CW = 16, CH = 18;

    private final Pane host;
    private final AudioManager audio;
    private final Runnable onReveal;
    private final Pane root = new Pane();
    private final Random rnd = new Random();

    private final Canvas rain = new Canvas(W, H);
    private Image atlas;
    private final Group terminal = new Group();
    private final Text[] bootLines = new Text[BOOT.length];
    private final Group tagGroup = new Group();
    private final Canvas mist = new Canvas(W, H);
    private final Group can = new Group();
    private final Rotate canTilt = new Rotate(0, 0, 80);
    private final Circle cone = new Circle(30);
    private final Rectangle flash = new Rectangle(W, H, Color.WHITE);
    private TagLine line1, line2;
    private Polygon crown;

    private AnimationTimer director;
    private Scene scene;
    private EventHandler<KeyEvent> skipKeys;
    private EventHandler<MouseEvent> skipMouse;
    private boolean vandalized, painting, finished;
    private int typedLines;

    // fases 2-5: el reloj es la canción
    private final Rectangle dim = new Rectangle(W, H, Color.BLACK);
    private final Group captionBox = new Group();
    private final Text[] captionLines = new Text[GARZON.length];
    private double freezeT = -1;                                  // t (pared) del congelado; -1 = aún en la fase 1
    private double audioMs = -1;                                  // currentTime REAL de la canción; -1 = aún no suena
    private boolean noAudio;                                      // sin canción: la fase 3 corre con el reloj de pared
    private double rawVoice, voice, voicePeak = 0.2;              // nivel de voz (0..1) crudo, suavizado y su pico
    private Runnable untapTime, untapVoice;

    // estado de la lluvia
    private final int cols = (int) (W / CW);
    private final double[] dropY = new double[cols], dropSpeed = new double[cols];
    private final int[] dropRow = new int[cols];

    /** Una línea del tag: la textura ya rasterizada, su capa de brillo y la máscara de "pintado". */
    private static final class TagLine {
        Text hit;                       // solo para geometría (dónde hay pintura → chorreones); no está en la escena
        ImageView paint, glow;
        final Rectangle reveal = new Rectangle();
        double left, top, w, h, ox, oy;  // ox/oy: margen de la textura alrededor de la letra (trazo + sombra)
    }

    private UndergroundIntro(Pane host, AudioManager audio, Runnable onReveal) {
        this.host = host;
        this.audio = audio;
        this.onReveal = onReveal;
    }

    /**
     * Monta la cinemática en {@code host} (lienzo lógico de 1280x720) y la arranca YA, en este mismo pulso.
     * {@code onReveal} se llama en el flash final (o al saltarla): ahí Main desvanece el telón.
     */
    public static void play(Pane host, AudioManager audio, Runnable onReveal) {
        new UndergroundIntro(host, audio, onReveal).start();
    }

    // ------------------------------------------------------------------ montaje

    private void start() {
        root.setMinSize(W, H); root.setPrefSize(W, H); root.setMaxSize(W, H);
        root.setClip(new Rectangle(W, H));
        root.setStyle("-fx-background-color: black;");

        atlas = buildAtlas();
        buildTerminal();
        line1 = tagLine("MINISTERIO", 150, 150);
        line2 = tagLine("DEL REBUSQUE", 116, line1.top + line1.h + 34);
        cone.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(57, 255, 20, 0.55)), new Stop(1, Color.rgb(57, 255, 20, 0))));
        cone.setVisible(false);
        buildCan();
        crown = crown();
        tagGroup.getChildren().addAll(mist, line1.glow, line2.glow, line1.paint, line2.paint, crown, can);
        planDrips(line1, 6);                                         // la geometría de los chorreones se calcula aquí,
        planDrips(line2, 6);                                         // no en el golpe del beat (evita un tirón de ~130 ms)
        tagGroup.getTransforms().add(new Rotate(-5, W / 2, (line1.top + line2.top + line2.h) / 2));   // los tags nunca van derechos

        Rectangle vignette = new Rectangle(W, H);
        vignette.setMouseTransparent(true);
        vignette.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0.5, Color.TRANSPARENT), new Stop(1, Color.rgb(0, 0, 0, 0.75))));
        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        dim.setOpacity(0);
        dim.setMouseTransparent(true);
        buildCaptions();
        root.getChildren().addAll(rain, terminal, dim, tagGroup, captionBox, vignette, new CrtOverlay(W, H), flash);
        host.getChildren().add(root);
        installSkip();

        GraphicsContext g = rain.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < cols; i++) {                             // gotas repartidas por toda la altura desde el frame 0
            dropY[i] = rnd.nextDouble() * (H / CH) - rnd.nextDouble() * 12;
            dropSpeed[i] = 16 + rnd.nextDouble() * 24;
            dropRow[i] = -1;
        }
        director = new AnimationTimer() {
            private long t0, last;
            @Override public void handle(long now) {
                if (t0 == 0) { t0 = now; last = now; }
                double t = (now - t0) / 1e9, dt = Math.min(0.25, (now - last) / 1e9);
                last = now;
                frame(t, dt);
            }
        };
        director.start();
        FpsProbe.scene("intro");
    }

    // ------------------------------------------------------------------ el director: un frame = una llamada

    private void frame(double t, double dt) {
        if (finished) return;
        if (freezeT < 0) {                                           // ---- FASE 1: reloj de pared
            drawRain(dt, 1);
            typeTerminal(t);
            if (!vandalized && t >= VANDAL_AT) vandalize();
            if (vandalized && !painting) {                           // la lata se agita (≈4.5 sacudidas/s) mientras entra
                double s = t - VANDAL_AT;
                canTilt.setAngle(16 * Math.sin(2 * Math.PI * 4.5 * s));
                can.setTranslateY(line1.top + line1.h / 2 + 10 * Math.sin(2 * Math.PI * 4.5 * s));
            }
            if (!painting && t >= PAINT_AT) { painting = true; canTilt.setAngle(-10); }
            if (painting) paintStrokes(t - PAINT_AT);
            if (t >= FREEZE_AT) freeze(t);
            return;
        }

        // ---- FASES 3-4: el reloj es el currentTime de la canción (lo alimenta el listener de currentTimeProperty)
        if (noAudio) audioMs = (t - freezeT) * 1000;
        boolean speaking = audioMs >= 0;
        drawRain(dt, speaking ? RAIN_SLOWMO : 0);                    // congelada hasta que la voz suena de verdad
        if (!speaking) return;
        double s = audioMs / 1000;
        dim.setOpacity(0.5 * smooth(s / 1.2));                       // el fondo se oscurece mientras habla Garzón
        captionBox.setOpacity(smooth((s - 0.15) / 0.6));
        typeCaptions(audioMs);
        voiceBeat(dt);
        for (Runnable r : dripMotion) r.run();
        if (audioMs >= DROP_MS) reveal();                            // red de seguridad (el listener ya lo dispara)
    }

    private static double smooth(double u) {
        u = Math.max(0, Math.min(1, u));
        return u * u * (3 - 2 * u);
    }

    // ------------------------------------------------------------------ fase 1: terminal hacker

    /** Atlas: los 20 glifos × 3 tonos, rasterizados una sola vez. */
    private Image buildAtlas() {
        Canvas c = new Canvas(GLYPHS.length() * CW, 3 * CH);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        Color[] tones = {Color.web("#e4ffdc"), MATRIX, MATRIX.deriveColor(0, 1, 0.72, 1)};
        for (int r = 0; r < 3; r++) {
            g.setFill(tones[r]);
            for (int i = 0; i < GLYPHS.length(); i++) g.fillText(String.valueOf(GLYPHS.charAt(i)), i * CW + 2, r * CH + 15);
        }
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }

    /**
     * Lluvia binaria: una gota por columna de 16 px, cada una a su velocidad (filas/s × dt real). En vez de borrar el
     * lienzo, cada frame se le pinta encima un negro casi transparente: lo ya dibujado se apaga solo y deja estela.
     */
    private void drawRain(double dt, double speed) {
        if (speed <= 0) return;                                      // congelada: ni cae ni se apaga la estela
        dt *= speed;                                                 // cámara lenta: el tiempo de la lluvia se estira
        GraphicsContext g = rain.getGraphicsContext2D();
        g.setFill(Color.rgb(0, 0, 0, Math.min(1, 5.5 * dt)));         // estela independiente de los FPS
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < cols; i++) {
            dropY[i] += dropSpeed[i] * dt;
            int row = (int) dropY[i];
            if (row != dropRow[i] && row >= 0) {
                if (dropRow[i] >= 0) glyph(g, 1 + rnd.nextInt(2), i * CW, dropRow[i] * CH);   // la cabeza anterior pasa a verde
                glyph(g, 0, i * CW, row * CH);
                dropRow[i] = row;
            }
            if (row * CH > H && rnd.nextDouble() < 0.03) { dropY[i] = -rnd.nextDouble() * 10; dropRow[i] = -1; }
        }
    }

    private void glyph(GraphicsContext g, int tone, double x, double y) {
        int k = rnd.nextInt(GLYPHS.length());
        g.drawImage(atlas, k * CW, tone * CH, CW, CH, x, y, CW, CH);
    }

    /** Ventana de consola que arranca el sistema. */
    private void buildTerminal() {
        Rectangle panel = new Rectangle(28, 28, 640, 196);
        panel.setFill(Color.rgb(0, 12, 2, 0.82));
        panel.setStroke(MATRIX);
        panel.setStrokeWidth(2);
        panel.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 255, 65, 0.5), 18, 0.1, 0, 0));
        panel.setCache(true);                                        // el resplandor se calcula una vez
        Rectangle bar = rect(28, 28, 640, 24, MATRIX);
        Text title = new Text(38, 45, "MINISTERIO.EXE  —  terminal de arranque");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        title.setFill(Color.BLACK);
        terminal.getChildren().addAll(panel, bar, title);
        Font f = Font.font("Monospaced", FontWeight.BOLD, 17);
        for (int l = 0; l < BOOT.length; l++) {
            Text line = new Text(44, 82 + l * 30, "");
            line.setFont(f);
            line.setFill(l == BOOT.length - 1 ? Color.web("#ffd400") : MATRIX);
            bootLines[l] = line;
            terminal.getChildren().add(line);
        }
    }

    /** El tecleo sale del tiempo transcurrido: carácter n = t · cps (con 0.11 s de respiro entre líneas). */
    private void typeTerminal(double t) {
        if (typedLines >= BOOT.length) return;
        double at = 0.12;
        for (int l = 0; l < BOOT.length; l++) {
            String s = BOOT[l];
            double end = at + s.length() / TYPE_CPS;
            if (t < at) break;
            int n = (int) Math.min(s.length(), (t - at) * TYPE_CPS);
            String shown = n >= s.length() ? s : s.substring(0, n) + "█";
            if (!shown.equals(bootLines[l].getText())) bootLines[l].setText(shown);
            if (t >= end && l == typedLines) {
                typedLines++;
                if (typedLines == BOOT.length) { terminal.setCache(true); terminal.setCacheHint(CacheHint.SPEED); }
            }
            at = end + 0.11;
        }
    }

    // ------------------------------------------------------------------ fase 2: el graffiti vandaliza la consola

    private void vandalize() {
        vandalized = true;
        if (audio != null) audio.playIntro();                        // lata agitándose YA; siseo y beat se encadenan
        ScreenShake.shake(terminal, 7, 380);
        TranslateTransition in = new TranslateTransition(Duration.millis(700), can);
        in.setDelay(Duration.millis(200));
        in.setToX(line1.left - 40);
        in.setInterpolator(Interpolator.SPLINE(0.15, 0.85, 0.3, 1));
        in.play();
    }

    /**
     * Rasteriza una línea del tag UNA vez: volumen 3D + contorno negro + relleno neón con brillo interior.
     * Además guarda una capa de brillo desenfocada aparte, cuya opacidad late con el bombo.
     */
    private TagLine tagLine(String text, double size, double top) {
        TagLine t = new TagLine();
        Font font = Font.font(tagFamily(), FontWeight.BLACK, size);
        t.hit = tagText(text, font);
        Bounds b = t.hit.getLayoutBounds();
        t.w = b.getWidth(); t.h = b.getHeight();
        t.left = (W - t.w) / 2; t.top = top;
        t.hit.setLayoutX(t.left - b.getMinX());
        t.hit.setLayoutY(t.top - b.getMinY());

        Text shadow = styled(text, font, Color.web("#062b10"), Color.web("#062b10"), 15, StrokeType.OUTSIDE);
        shadow.setTranslateX(8); shadow.setTranslateY(8);
        Text outline = styled(text, font, Color.BLACK, Color.BLACK, 14, StrokeType.OUTSIDE);
        Text fill = styled(text, font, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#c8ffb0")), new Stop(0.32, NEON), new Stop(1, NEON_DEEP)), Color.web("#f0ffe6"), 2.5, StrokeType.INSIDE);
        fill.setEffect(new DropShadow(BlurType.GAUSSIAN, NEON, 24, 0.22, 0, 0));
        Group art = new Group(shadow, outline, fill);
        Bounds ab = art.getBoundsInParent();                          // incluye trazo, sombra 3D y resplandor
        t.paint = raster(art, ab, 2);                                // a 2x: nítido aunque la ventana escale
        // la letra empieza (b.min − ab.min) px dentro de la textura: así su borde visual cae en (left, top)
        t.ox = b.getMinX() - ab.getMinX();
        t.oy = b.getMinY() - ab.getMinY();
        t.paint.setLayoutX(t.left - t.ox);
        t.paint.setLayoutY(t.top - t.oy);

        Text halo = styled(text, font, NEON, NEON, 10, StrokeType.OUTSIDE);
        Group haloG = new Group(halo);
        haloG.setEffect(new GaussianBlur(28));
        Bounds hb = haloG.getBoundsInParent();
        t.glow = raster(haloG, hb, 1);                               // borroso por diseño: 1x basta
        t.glow.setLayoutX(t.left - (b.getMinX() - hb.getMinX()));
        t.glow.setLayoutY(t.top - (b.getMinY() - hb.getMinY()));
        t.glow.setOpacity(0);

        t.reveal.setX(t.ox - 22);                                    // coordenadas locales del ImageView
        t.reveal.setY(0);
        t.reveal.setWidth(0);
        t.reveal.setHeight(ab.getHeight());
        t.paint.setClip(t.reveal);
        return t;
    }

    /** Snapshot de {@code n} a escala {@code k}, mostrado a su tamaño lógico (fitWidth) con suavizado. */
    private static ImageView raster(Node n, Bounds b, double k) {
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        sp.setTransform(new javafx.scene.transform.Scale(k, k));
        ImageView v = new ImageView(n.snapshot(sp, null));
        v.setFitWidth(b.getWidth());
        v.setFitHeight(b.getHeight());
        v.setSmooth(true);
        return v;
    }

    private static Text styled(String s, Font font, javafx.scene.paint.Paint fill, Color stroke, double sw, StrokeType type) {
        Text t = tagText(s, font);
        t.setFill(fill);
        t.setStroke(stroke);
        t.setStrokeWidth(sw);
        t.setStrokeType(type);
        t.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return t;
    }

    private static Text tagText(String s, Font font) {
        Text t = new Text(s);
        t.setFont(font);
        t.setBoundsType(TextBoundsType.VISUAL);
        t.setTextOrigin(VPos.TOP);
        return t;
    }

    private void buildCan() {
        Rectangle body = new Rectangle(-24, 26, 48, 122);
        body.setArcWidth(10); body.setArcHeight(10);
        body.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a1a1a")), new Stop(0.35, Color.web("#9a9a9a")), new Stop(0.6, Color.web("#454545")), new Stop(1, Color.web("#111"))));
        Rectangle shoulder = new Rectangle(-20, 16, 40, 16);
        shoulder.setArcWidth(14); shoulder.setArcHeight(14);
        shoulder.setFill(Color.web("#5a5a5a"));
        Rectangle valve = rect(-6, 4, 12, 14, Color.web("#dcdcdc"));
        valve.setStroke(Color.web("#2a2a2a"));
        Group canBody = new Group(body, rect(-24, 62, 48, 50, NEON_DEEP), rect(-24, 80, 48, 9, Color.web("#0a0a0a")),
                shoulder, valve, rect(-2.5, 0, 5, 6, Color.web("#0a0a0a")));
        canBody.getTransforms().add(canTilt);
        canBody.setCache(true);
        canBody.setCacheHint(CacheHint.ROTATE);
        can.getChildren().addAll(cone, canBody);
        can.setTranslateX(W + 120);
        can.setTranslateY(line1.top + line1.h / 2);
    }

    /**
     * Tres trazos (coinciden con los tres "psst" del siseo): "MINIST" · "ERIO" · "DEL REBUSQUE". El ancho de cada
     * máscara es una función lineal por tramos del tiempo; la mano (lata) sigue el borde que crece.
     */
    private void paintStrokes(double s) {
        double f1 = line1.w + 46, f2 = line2.w + 46;
        double w1 = s < 0.72 ? f1 * 0.56 * s / 0.72 : s < 0.80 ? f1 * 0.56 : s < 1.58 ? f1 * (0.56 + 0.44 * (s - 0.80) / 0.78) : f1;
        double w2 = s < 1.66 ? 0 : s < 2.44 ? f2 * (s - 1.66) / 0.78 : f2;
        boolean on1 = w1 > line1.reveal.getWidth() + 0.05, on2 = w2 > line2.reveal.getWidth() + 0.05;
        line1.reveal.setWidth(w1);
        line2.reveal.setWidth(w2);

        TagLine l = on2 || w1 >= f1 ? line2 : line1;
        double x = l.left - 22 + l.reveal.getWidth();
        double y = l.top + l.h * (0.5 + 0.44 * Math.sin(2 * Math.PI * 3.4 * s));   // la mano sube y baja
        can.setTranslateX(x);
        can.setTranslateY(y);
        boolean spraying = on1 || on2;
        cone.setVisible(spraying);
        if (!spraying) return;
        cone.setRadius(24 + rnd.nextDouble() * 10);
        GraphicsContext g = mist.getGraphicsContext2D();
        for (int i = 0; i < 34; i++) {                               // niebla de pintura alrededor del trazo
            double r = Math.abs(rnd.nextGaussian()) * (i < 26 ? 16 : 42), ang = rnd.nextDouble() * Math.PI * 2;
            double d = 1 + rnd.nextDouble() * 2.4;
            g.setFill(Color.rgb(57, 255, 20, 0.04 + rnd.nextDouble() * (i < 26 ? 0.16 : 0.08)));
            g.fillOval(x + Math.cos(ang) * r - d / 2, y + Math.sin(ang) * r - d / 2, d, d);
        }
    }

    // ------------------------------------------------------------------ fase 2: el congelado y la canción

    /**
     * Último trazo del tag: la animación se congela (la lluvia se detiene, la lata suelta el gatillo) y en este MISMO
     * frame suena Alcolirykoz. A partir de aquí el reloj de la cinemática es el currentTime del MediaPlayer.
     */
    private void freeze(double t) {
        freezeT = t;
        cone.setVisible(false);
        line1.reveal.setWidth(line1.w + 46);
        line2.reveal.setWidth(line2.w + 46);
        canTilt.setAngle(0);
        FadeTransition canOut = new FadeTransition(Duration.millis(1100), can);   // la mano se retira en la penumbra
        canOut.setDelay(Duration.millis(500));
        canOut.setToValue(0);
        canOut.setOnFinished(e -> can.setVisible(false));
        canOut.play();
        terminal.setOpacity(0.35);                                   // la consola vandalizada queda medio muerta
        slap(crown);                                                 // la firma: corona de tag
        dripsStart = System.nanoTime();
        for (Node n : drips) n.setVisible(true);

        if (Boolean.getBoolean("rebusque.debug")) System.err.printf("[intro] congelado en t=%.3f s → play()%n", t);
        if (audio == null) { noAudio = true; return; }
        audio.playStreetTheme();
        untapTime = audio.onMusicTime(MusicTrack.CALLE_THEME, ms -> {
            audioMs = ms;
            if (ms >= DROP_MS) reveal();                             // FASE 4: el milisegundo del drop
        });
        untapVoice = audio.onMusicVoiceLevel(MusicTrack.CALLE_THEME, v -> rawVoice = v);
        audio.whenMusicPlaying(ms -> { if (ms < 0 && !finished) noAudio = true; });
    }

    // ------------------------------------------------------------------ fase 3: la voz de Garzón

    /**
     * El tag palpita con la VOZ: nivel del espectro suavizado (ataque rápido, caída lenta) y normalizado contra su
     * propio pico reciente, así reacciona igual en una sílaba suave que en una fuerte. Solo escala y opacidad de
     * texturas ya rasterizadas: la GPU lo hace casi gratis.
     */
    private void voiceBeat(double dt) {
        voice += (rawVoice - voice) * (rawVoice > voice ? 0.55 : Math.min(1, dt * 6));
        voicePeak = Math.max(voice, voicePeak * (1 - 0.25 * dt));
        double lvl = Math.max(0, Math.min(1, (voice - 0.35 * voicePeak) / (0.65 * Math.max(0.05, voicePeak))));
        double s = 1 + 0.03 * lvl;
        tagGroup.setScaleX(s);
        tagGroup.setScaleY(s);
        line1.glow.setOpacity(0.2 + 0.7 * lvl);
        line2.glow.setOpacity(0.2 + 0.7 * lvl);
    }

    /** Terminal inferior del homenaje (aparece al empezar la voz). */
    private void buildCaptions() {
        double x = 150, y = H - 140, w = W - 300;
        Rectangle panel = new Rectangle(x, y, w, 100);
        panel.setFill(Color.rgb(0, 10, 2, 0.8));
        panel.setStroke(MATRIX);
        panel.setStrokeWidth(1.5);
        Rectangle bar = rect(x, y, w, 20, MATRIX);
        Text title = new Text(x + 10, y + 15, "MINISTERIO.EXE  —  transmisión entrante");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        title.setFill(Color.BLACK);
        captionBox.getChildren().addAll(panel, bar, title);
        Font f = Font.font("Monospaced", FontWeight.BOLD, 18);
        for (int i = 0; i < GARZON.length; i++) {
            Text line = new Text(x + 16, y + 50 + i * 30, "");
            line.setFont(f);
            line.setFill(i == 0 ? Color.web("#ffd400") : MATRIX);
            captionLines[i] = line;
            captionBox.getChildren().add(line);
        }
        captionBox.setOpacity(0);
        captionBox.setMouseTransparent(true);
    }

    /** Cada línea se teclea dentro de su bloque de voz (termina al 85 % del bloque), leyendo el ms real del audio. */
    private void typeCaptions(double ms) {
        for (int i = 0; i < GARZON.length; i++) {
            Caption c = GARZON[i];
            double u = (ms - c.fromMs()) / ((c.toMs() - c.fromMs()) * 0.85);
            int len = c.text().length(), n = (int) Math.max(0, Math.min(len, u * len));
            String shown = n == 0 ? "" : n >= len ? c.text() : c.text().substring(0, n) + "█";
            if (!shown.equals(captionLines[i].getText())) captionLines[i].setText(shown);
        }
    }

    private final List<Node> drips = new ArrayList<>();
    private long dripsStart;

    /**
     * La pintura chorrea: el borde inferior real de cada letra se busca con Text.contains sobre la geometría. Se planea
     * al montar (nodos ocultos) y cada gota cae acelerando (ease-in cuadrático) a partir del congelado.
     */
    private void planDrips(TagLine l, int count) {
        int made = 0;
        for (int tries = 0; tries < 70 && made < count; tries++) {
            double x = l.left + 8 + rnd.nextDouble() * (l.w - 16);
            Double yStart = null;
            for (double y = l.top + l.h; y > l.top + l.h * 0.4; y -= 2) {
                Point2D p = l.hit.parentToLocal(x, y);
                if (l.hit.contains(p)) { yStart = y; break; }
            }
            if (yStart == null) continue;
            made++;
            double len = 22 + rnd.nextDouble() * (l == line1 ? 26 : 70), w = 3.5 + rnd.nextDouble() * 3;
            Rectangle trail = new Rectangle(x - w / 2, yStart - 3, w, 0);
            trail.setArcWidth(w); trail.setArcHeight(w);
            trail.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, NEON), new Stop(1, NEON_DEEP)));
            Circle drop = new Circle(x, yStart, w * 0.85, NEON_DEEP);
            trail.setVisible(false);
            drop.setVisible(false);
            tagGroup.getChildren().addAll(trail, drop);
            drips.add(trail);
            drips.add(drop);
            double dur = 2.6 + rnd.nextDouble() * 2.6, delay = rnd.nextDouble() * 1.5, y0 = yStart;   // lentas: bajo la voz
            dripMotion.add(() -> {                                   // lo ejecuta el director en cada frame
                if (dripsStart == 0) return;
                double u = Math.max(0, Math.min(1, ((System.nanoTime() - dripsStart) / 1e9 - delay) / dur));
                trail.setHeight(len * u * u);
                drop.setCenterY(y0 + len * u * u);
            });
        }
    }

    private final List<Runnable> dripMotion = new ArrayList<>();

    /** Corona de tres puntas sobre el final de "MINISTERIO": firma clásica del graffiti de calle. */
    private Polygon crown() {
        Polygon p = new Polygon(-38, 0, -32, -36, -13, -13, 0, -44, 13, -13, 32, -36, 38, 0);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#f4f4f4"));
        p.setStrokeWidth(5);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        p.setLayoutX(line1.left + line1.w - 70);
        p.setLayoutY(line1.top - 16);
        p.setEffect(new DropShadow(10, Color.web("#ffffff66")));
        p.setCache(true);
        p.setCacheHint(CacheHint.SCALE);
        p.setScaleX(0); p.setScaleY(0);
        return p;
    }

    private void slap(Polygon n) {
        n.setRotate(8);
        ScaleTransition s = new ScaleTransition(Duration.millis(220), n);
        s.setFromX(1.5); s.setFromY(1.5);
        s.setToX(1); s.setToY(1);
        s.setInterpolator(BACK_OUT);
        s.play();
    }

    /** "Back-out": se pasa un poco del destino y vuelve (SPLINE no admite puntos de control > 1). */
    private static final Interpolator BACK_OUT = new Interpolator() {
        @Override protected double curve(double t) {
            double c = 1.9, u = t - 1;
            return 1 + (c + 1) * u * u * u + c * u * u;
        }
    };

    // ------------------------------------------------------------------ fase 5: flash y revelado

    /**
     * Cae el beat: flash verde-blanco intenso (el telón aún tapa todo) y aviso a Main, que disipa el telón y sella la
     * araña. Si la saltaron antes del drop, la canción salta al beat en el mismo instante (sin la intro hablada).
     */
    private void reveal() {
        if (finished) return;
        finished = true;
        if (Boolean.getBoolean("rebusque.debug"))
            System.err.printf("[intro] revelado con la canción en %.0f ms (drop %.0f ms)%s%n", audioMs, DROP_MS, noAudio ? " · sin audio" : "");
        if (audio != null && audioMs < DROP_MS) audio.playStreetThemeFromDrop();   // saltada (en la fase 1, 2 o 3)
        if (untapTime != null) untapTime.run();
        if (untapVoice != null) untapVoice.run();
        removeSkip();
        flash.setFill(Color.web("#e6ffd8"));
        flash.setOpacity(1);
        tagGroup.setScaleX(1.08);
        tagGroup.setScaleY(1.08);
        new AnimationTimer() {                                       // el flash se apaga en 650 ms; luego se detiene todo
            private long t0;
            @Override public void handle(long now) {
                if (t0 == 0) t0 = now;
                double u = Math.min(1, (now - t0) / 0.65e9);
                flash.setOpacity((1 - u) * (1 - u));
                if (u >= 1) { stop(); director.stop(); }
            }
        }.start();
        onReveal.run();
    }

    private void installSkip() {
        scene = host.getScene();
        if (scene == null) return;
        skipKeys = e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                reveal();
            }
        };
        skipMouse = e -> { e.consume(); reveal(); };
        scene.addEventFilter(KeyEvent.KEY_PRESSED, skipKeys);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, skipMouse);
    }

    private void removeSkip() {
        if (scene == null) return;
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, skipKeys);
        scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, skipMouse);
    }

    // ------------------------------------------------------------------ utilidades

    private static Rectangle rect(double x, double y, double w, double h, javafx.scene.paint.Paint p) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setFill(p);
        return r;
    }

    /** Fuente gruesa de cartel: Impact si existe (Windows), si no la más pesada disponible. */
    private static String tagFamily() {
        List<String> fams = Font.getFamilies();
        for (String f : new String[]{"Impact", "Arial Black", "Franklin Gothic Heavy", "Segoe UI Black"}) if (fams.contains(f)) return f;
        return Font.getDefault().getFamily();
    }
}
