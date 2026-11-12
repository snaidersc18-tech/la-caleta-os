package app.secret.dave;

import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.Random;

/**
 * Corazón Triste tropical-ciberpunk (Bad Bunny). Un solo ente, no un zombi con accesorios pegados:
 * <ul>
 *   <li>Chasis de araña = duna de arena (textura de granos) con palmeras de neón grabadas.</li>
 *   <li>Patas = troncos de palma segmentados que terminan en tenis blancos; rodillas con aro de neón.</li>
 *   <li>Torso = el corazón triste de un solo ojo, cosido con grapas cibernéticas brillantes y una placa remachada.</li>
 *   <li>Streetwear de verano: camisa floral abierta con mangas cortas, bucket hat estampado y cadena con dije.</li>
 *   <li>Aura: sol retro a franjas y dos palmeras de neón detrás.</li>
 * </ul>
 */
public final class TropicalHeartArt extends Group {
    public static final String HEART = "M 0 120 C -150 20 -125 -112 -52 -102 C -22 -97 0 -72 0 -52 C 0 -72 22 -97 52 -102 C 125 -112 150 20 0 120 Z";

    private static final Color NEON_CYAN = Color.web("#00e5ff"), NEON_PINK = Color.web("#ff4fa3");

    private final SVGPath heart = new SVGPath();
    private final Group bodyAll = new Group();
    private final Group face = new Group();
    private final Path mouthSad, mouthGrin;
    private final Group teeth = new Group();
    private final Group aura = new Group();
    private final Paint heartFill = new LinearGradient(0, -100, 0, 120, false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#ff5a3c")), new Stop(0.55, Color.web("#ff2d55")), new Stop(1, Color.web("#7a0f3a")));

    public TropicalHeartArt() {
        Random r = new Random(11);
        buildAura(r);

        Group chassis = sandChassis(r);
        Group legs = palmLegs();

        heart.setContent(HEART);
        heart.setFill(heartFill);
        heart.setStroke(Color.web("#120008")); heart.setStrokeWidth(6);
        heart.setEffect(new DropShadow(30, Color.web("#ff6a00")));

        Group shirt = floralShirt(r);
        Group cyber = cyberStitches();

        // cara: un solo ojo enorme, ceja triste y boca triste / sonrisa macabra
        Circle eye = new Circle(0, -14, 42, Color.web("#fff3e0")); eye.setStroke(Color.BLACK); eye.setStrokeWidth(5);
        Circle iris = new Circle(0, -8, 22, NEON_CYAN); iris.setEffect(new Glow(1.0));
        Circle pupil = new Circle(0, -8, 9, Color.BLACK);
        Circle glint = new Circle(-8, -18, 5, Color.WHITE);
        Path brow = new Path(new MoveTo(-46, -58), new LineTo(-4, -44), new MoveTo(46, -58), new LineTo(4, -44));
        brow.setStroke(Color.BLACK); brow.setStrokeWidth(6); brow.setStrokeLineCap(StrokeLineCap.ROUND);
        Path tear = new Path(new MoveTo(30, 18), new QuadCurveTo(38, 34, 30, 40), new QuadCurveTo(22, 34, 30, 18));
        tear.setFill(NEON_CYAN); tear.setOpacity(0.85); tear.setEffect(new Glow(0.8));

        mouthSad = new Path(new MoveTo(-40, 78), new QuadCurveTo(0, 46, 40, 78));
        mouthSad.setStroke(Color.BLACK); mouthSad.setStrokeWidth(6); mouthSad.setFill(null); mouthSad.setStrokeLineCap(StrokeLineCap.ROUND);
        mouthGrin = new Path(new MoveTo(-52, 46), new QuadCurveTo(0, 112, 52, 46), new ClosePath());
        mouthGrin.setFill(Color.web("#1a0010")); mouthGrin.setStroke(Color.BLACK); mouthGrin.setStrokeWidth(5);
        for (int i = -2; i <= 2; i++) {
            Rectangle t = new Rectangle(i * 18 - 7, 52 + (2 - Math.abs(i)) * 5, 14, 15);
            t.setFill(Color.web("#fff3e0")); t.setStroke(Color.BLACK); t.setStrokeWidth(2);
            teeth.getChildren().add(t);
        }
        teeth.setVisible(false); mouthGrin.setVisible(false);
        face.getChildren().addAll(cyber, eye, iris, pupil, glint, brow, tear, mouthSad, mouthGrin, teeth);

        Group chain = chain();
        Group hat = bucketHat(r);

        bodyAll.getChildren().addAll(chassis, legs, heart, shirt, face, chain, hat);
        getChildren().addAll(aura, bodyAll);
    }

    // ------------------------------------------------------------------ piezas

    /** Sol retro a franjas (sol − franjas mediante Shape.subtract) y palmeras de neón a los lados. */
    private void buildAura(Random r) {
        Circle sunDisc = new Circle(0, -10, 175);
        Shape stripes = null;
        // Franjas cada vez más gruesas hacia abajo (altura = 4 + 2.5·i, una cada 24 px): el clásico sol synthwave.
        for (int i = 0; i < 6; i++) {
            Rectangle s = new Rectangle(-180, 20 + i * 24, 360, 4 + i * 2.5);
            stripes = stripes == null ? s : Shape.union(stripes, s);
        }
        Shape sun = Shape.subtract(sunDisc, stripes);
        sun.setFill(new LinearGradient(0, -185, 0, 165, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffe066")), new Stop(0.5, Color.web("#ff8a3d")), new Stop(1, Color.web("#ff3d7f"))));
        sun.setOpacity(0.85);
        aura.getChildren().add(sun);
        aura.getChildren().addAll(neonPalm(-200, 190, 1, NEON_CYAN), neonPalm(200, 190, -1, NEON_PINK));
        for (int i = 0; i < 26; i++) {                           // brillitos de arena flotando
            Circle c = new Circle((r.nextDouble() - 0.5) * 460, (r.nextDouble() - 0.6) * 380, 1.5 + r.nextDouble() * 2.5,
                    r.nextBoolean() ? Color.web("#ffe6b0") : NEON_CYAN);
            c.setOpacity(0.4 + r.nextDouble() * 0.5);
            aura.getChildren().add(c);
        }
    }

    /** Palmera de neón: tronco curvo y 5 hojas en arco, solo trazo luminoso. {@code lean} = hacia dónde se inclina. */
    static Group neonPalm(double baseX, double baseY, int lean, Color c) {
        double topX = baseX + lean * 46, topY = baseY - 230;
        QuadCurve trunk = new QuadCurve(baseX, baseY, baseX + lean * 6, baseY - 130, topX, topY);
        trunk.setFill(null); trunk.setStroke(c); trunk.setStrokeWidth(5); trunk.setStrokeLineCap(StrokeLineCap.ROUND);
        Group g = new Group(trunk);
        double[] angles = {-160, -125, -90, -55, -20};
        for (double a : angles) {
            double rad = Math.toRadians(a), len = 78;
            double ex = topX + Math.cos(rad) * len, ey = topY + Math.sin(rad) * len * 0.55 + 34;
            // hoja = arco cuyo control se eleva sobre la punta: la fronda sube y cae
            QuadCurve leaf = new QuadCurve(topX, topY, topX + Math.cos(rad) * len * 0.55, topY - 30, ex, ey);
            leaf.setFill(null); leaf.setStroke(c); leaf.setStrokeWidth(4); leaf.setStrokeLineCap(StrokeLineCap.ROUND);
            g.getChildren().add(leaf);
        }
        g.setEffect(new Glow(1.0));
        g.setOpacity(0.8);
        return g;
    }

    /** Chasis de araña hecho duna: elipse de arena con granos y palmeras de neón grabadas. */
    private Group sandChassis(Random r) {
        Ellipse dune = new Ellipse(0, 112, 128, 56);
        dune.setFill(new LinearGradient(0, 56, 0, 168, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f3d29a")), new Stop(0.6, Color.web("#d9a764")), new Stop(1, Color.web("#a8743e"))));
        dune.setStroke(Color.web("#3a1a10")); dune.setStrokeWidth(4);
        Group grains = new Group();
        for (int i = 0; i < 150; i++) {
            // muestreo uniforme en la elipse: radio = sqrt(u) para que los granos no se amontonen en el centro
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble());
            Circle g = new Circle(Math.cos(a) * d * 120, 112 + Math.sin(a) * d * 50, 0.8 + r.nextDouble() * 1.6,
                    r.nextBoolean() ? Color.web("#8a5a2e") : Color.web("#fff1cc"));
            g.setOpacity(0.55);
            grains.getChildren().add(g);
        }
        Group engraved = new Group(miniPalm(-78, 150, NEON_CYAN), miniPalm(78, 150, NEON_PINK), miniPalm(-30, 162, NEON_PINK), miniPalm(30, 162, NEON_CYAN));
        Ellipse clip = new Ellipse(0, 112, 126, 54);
        Group surface = new Group(grains, engraved);
        surface.setClip(clip);
        return new Group(dune, surface);
    }

    /**
     * Palmera grabada en miniatura: se dibuja con la base en el origen y luego Translate·Scale. Las transformaciones de
     * getTransforms() se aplican respecto al origen local (no al centro de los bounds como setScaleX), así la base de
     * la palmera cae exactamente en (x, y).
     */
    private static Group miniPalm(double x, double y, Color c) {
        Group p = neonPalm(0, 0, x < 0 ? 1 : -1, c);
        p.getTransforms().addAll(new Translate(x, y), new Scale(0.22, 0.22));
        return p;
    }

    /** 6 patas de tronco de palma: trazo marrón + trazo discontinuo claro = anillos del tronco. Tenis al final. */
    private Group palmLegs() {
        Group g = new Group();
        double[][] spec = {{-96, 90, -168, 40, -196, 176}, {-104, 118, -182, 104, -150, 196}, {-80, 138, -112, 176, -96, 206},
                {96, 90, 168, 40, 196, 176}, {104, 118, 182, 104, 150, 196}, {80, 138, 112, 176, 96, 206}};
        for (double[] s : spec) {
            Polyline trunk = new Polyline(s);
            trunk.setStroke(Color.web("#6e4426")); trunk.setStrokeWidth(13);
            trunk.setStrokeLineJoin(StrokeLineJoin.ROUND); trunk.setStrokeLineCap(StrokeLineCap.ROUND);
            Polyline rings = new Polyline(s);
            rings.setStroke(Color.web("#b07a4a")); rings.setStrokeWidth(13);
            rings.getStrokeDashArray().addAll(3.0, 7.0);
            rings.setStrokeLineJoin(StrokeLineJoin.ROUND);
            Circle knee = new Circle(s[2], s[3], 8, Color.web("#2a1020"));
            knee.setStroke(NEON_CYAN); knee.setStrokeWidth(2.5); knee.setEffect(new Glow(0.9));
            g.getChildren().addAll(trunk, rings, knee, sneaker(s[4], s[5], s[4] < 0));
        }
        return g;
    }

    private static Group sneaker(double x, double y, boolean left) {
        double dir = left ? -1 : 1;                              // la puntera mira hacia fuera del cuerpo
        Rectangle upper = new Rectangle(x - 14 + dir * 4, y - 8, 28, 14);
        upper.setArcWidth(12); upper.setArcHeight(12);
        upper.setFill(Color.web("#f7f7f7")); upper.setStroke(Color.BLACK); upper.setStrokeWidth(2);
        Rectangle sole = MechaSpiderArt.rect(x - 16 + dir * 4, y + 4, 32, 5, Color.web("#ff4fa3"));
        sole.setArcWidth(4); sole.setArcHeight(4);
        Line stripe = new Line(x - 6 + dir * 4, y - 2, x + 6 + dir * 4, y - 5);
        stripe.setStroke(NEON_CYAN); stripe.setStrokeWidth(2.5);
        return new Group(upper, sole, stripe);
    }

    /** Camisa floral abierta: dos paneles recortados al contorno del corazón + mangas cortas en los lóbulos. */
    private Group floralShirt(Random r) {
        Paint fabric = new LinearGradient(0, -100, 0, 120, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5fe0d0")), new Stop(1, Color.web("#2a9d9a")));
        Polygon left = new Polygon(-160, -120, -58, -120, -50, -40, -62, 40, -30, 130, -160, 130);
        Polygon right = new Polygon(160, -120, 58, -120, 50, -40, 62, 40, 30, 130, 160, 130);
        Group panels = new Group();
        for (Polygon p : new Polygon[]{left, right}) {
            p.setFill(fabric);
            panels.getChildren().add(p);
        }
        for (int i = 0; i < 16; i++) {                           // hibiscos de 5 pétalos repartidos por los paneles
            double side = i % 2 == 0 ? -1 : 1;
            double fx = side * (75 + r.nextDouble() * 55), fy = -90 + r.nextDouble() * 170;
            panels.getChildren().add(hibiscus(fx, fy, 5 + r.nextDouble() * 3, r.nextBoolean() ? Color.web("#ff6fa8") : Color.web("#fff3e0")));
        }
        SVGPath clip = new SVGPath(); clip.setContent(HEART);
        panels.setClip(clip);

        // solapas: borde blanco del corte de la camisa
        Polyline lapelL = new Polyline(-58, -100, -50, -40, -62, 40, -32, 116);
        Polyline lapelR = new Polyline(58, -100, 50, -40, 62, 40, 32, 116);
        for (Polyline l : new Polyline[]{lapelL, lapelR}) { l.setStroke(Color.web("#fff3e0")); l.setStrokeWidth(3); }

        // mangas cortas y brazos con guantes
        Polygon sleeveL = new Polygon(-118, -40, -150, 6, -126, 30, -102, -6);
        Polygon sleeveR = new Polygon(118, -40, 150, 6, 126, 30, 102, -6);
        Group arms = new Group();
        for (Polygon s : new Polygon[]{sleeveL, sleeveR}) {
            s.setFill(fabric); s.setStroke(Color.web("#0f3d3b")); s.setStrokeWidth(3);
            arms.getChildren().add(s);
        }
        Path armL = new Path(new MoveTo(-138, 20), new LineTo(-166, 66)), armR = new Path(new MoveTo(138, 20), new LineTo(166, 66));
        for (Path a : new Path[]{armL, armR}) { a.setStroke(Color.web("#0a0a0d")); a.setStrokeWidth(6); a.setStrokeLineCap(StrokeLineCap.ROUND); }
        Circle gloveL = new Circle(-166, 70, 11, Color.WHITE), gloveR = new Circle(166, 70, 11, Color.WHITE);
        gloveL.setStroke(Color.BLACK); gloveR.setStroke(Color.BLACK);
        arms.getChildren().addAll(armL, armR, gloveL, gloveR);

        return new Group(arms, panels, lapelL, lapelR);
    }

    private static Group hibiscus(double x, double y, double s, Color petal) {
        Group g = new Group();
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5 - Math.PI / 2;                 // 5 pétalos a 72°
            g.getChildren().add(new Circle(x + Math.cos(a) * s, y + Math.sin(a) * s, s * 0.75, petal));
        }
        g.getChildren().add(new Circle(x, y, s * 0.45, Color.web("#ffd166")));
        return g;
    }

    /** Costura cibernética: cicatriz curva con grapas luminosas perpendiculares y una placa remachada con LED. */
    private Group cyberStitches() {
        CubicCurve scar = new CubicCurve(-92, -62, -60, -30, -78, 30, -46, 86);
        scar.setFill(null); scar.setStroke(Color.web("#3a0016")); scar.setStrokeWidth(4);
        Group g = new Group(scar);
        // grapas: en t = 0.1..0.9 se evalúa la curva y su tangente; cada grapa va sobre la NORMAL a la tangente.
        for (int i = 1; i <= 7; i++) {
            double t = i / 8.0;
            double[] p = cubic(scar, t), d = cubicTangent(scar, t);
            double len = Math.hypot(d[0], d[1]), nx = -d[1] / len * 9, ny = d[0] / len * 9;
            Line staple = new Line(p[0] - nx, p[1] - ny, p[0] + nx, p[1] + ny);
            staple.setStroke(NEON_CYAN); staple.setStrokeWidth(3); staple.setEffect(new Glow(1.0));
            Circle e1 = new Circle(p[0] - nx, p[1] - ny, 2.2, Color.web("#d9d9e6"));
            Circle e2 = new Circle(p[0] + nx, p[1] + ny, 2.2, Color.web("#d9d9e6"));
            g.getChildren().addAll(staple, e1, e2);
        }
        Rectangle plate = new Rectangle(54, -84, 40, 30);
        plate.setArcWidth(6); plate.setArcHeight(6); plate.setRotate(14);
        plate.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#e6e9ee")), new Stop(1, Color.web("#7f8691"))));
        plate.setStroke(Color.web("#2a2a2a")); plate.setStrokeWidth(2);
        Group plateG = new Group(plate);
        for (double[] rv : new double[][]{{60, -78}, {88, -74}, {56, -60}, {84, -55}}) plateG.getChildren().add(new Circle(rv[0], rv[1], 2, Color.web("#2a2a2a")));
        Circle led = new Circle(72, -68, 3.5, NEON_PINK); led.setEffect(new Glow(1.0));
        plateG.getChildren().add(led);
        g.getChildren().add(plateG);
        return g;
    }

    /** Punto de la Bézier cúbica: B(t) = (1−t)³P0 + 3(1−t)²t·P1 + 3(1−t)t²·P2 + t³P3. */
    private static double[] cubic(CubicCurve c, double t) {
        double u = 1 - t, a = u * u * u, b = 3 * u * u * t, d = 3 * u * t * t, e = t * t * t;
        return new double[]{a * c.getStartX() + b * c.getControlX1() + d * c.getControlX2() + e * c.getEndX(),
                a * c.getStartY() + b * c.getControlY1() + d * c.getControlY2() + e * c.getEndY()};
    }

    /** Derivada: B'(t) = 3(1−t)²(P1−P0) + 6(1−t)t(P2−P1) + 3t²(P3−P2). */
    private static double[] cubicTangent(CubicCurve c, double t) {
        double u = 1 - t, a = 3 * u * u, b = 6 * u * t, d = 3 * t * t;
        return new double[]{a * (c.getControlX1() - c.getStartX()) + b * (c.getControlX2() - c.getControlX1()) + d * (c.getEndX() - c.getControlX2()),
                a * (c.getControlY1() - c.getStartY()) + b * (c.getControlY2() - c.getControlY1()) + d * (c.getEndY() - c.getControlY2())};
    }

    private static Group chain() {
        QuadCurve links = new QuadCurve(-74, 72, 0, 128, 74, 72);
        links.setFill(null); links.setStroke(Color.web("#f5c542")); links.setStrokeWidth(4);
        links.getStrokeDashArray().addAll(6.0, 3.0);
        SVGPath pendant = new SVGPath();
        pendant.setContent("M 0 116 C -12 106 -10 94 -4 94 C -1 94 0 97 0 99 C 0 97 1 94 4 94 C 10 94 12 106 0 116 Z");
        pendant.setFill(NEON_PINK); pendant.setStroke(Color.web("#f5c542")); pendant.setStrokeWidth(2);
        pendant.setEffect(new Glow(0.9));
        return new Group(links, pendant);
    }

    /** Bucket hat entre los lóbulos: copa trapezoidal + ala elíptica, tela rosa con lunares de palmera. */
    private static Group bucketHat(Random r) {
        Ellipse brim = new Ellipse(0, -100, 96, 18);
        brim.setFill(Color.web("#ff7eb6")); brim.setStroke(Color.web("#3a0a20")); brim.setStrokeWidth(4);
        Polygon crown = new Polygon(-58, -102, 58, -102, 44, -150, -44, -150);
        crown.setFill(Color.web("#ff9cc8")); crown.setStroke(Color.web("#3a0a20")); crown.setStrokeWidth(4);
        Rectangle band = MechaSpiderArt.rect(-56, -118, 112, 10, Color.web("#2a9d9a"));
        Group dots = new Group();
        for (int i = 0; i < 9; i++) {
            Circle d = new Circle(-36 + r.nextDouble() * 72, -146 + r.nextDouble() * 26, 2.4, Color.web("#fff3e0"));
            dots.getChildren().add(d);
        }
        Polygon clip = new Polygon(-58, -102, 58, -102, 44, -150, -44, -150);
        dots.setClip(clip);
        Group g = new Group(brim, crown, band, dots);
        g.setRotate(-6);
        return g;
    }

    // ------------------------------------------------------------------ estados

    /**
     * Silueta negra a contraluz (para el spotlight): ColorAdjust(brillo −1) apaga todos los colores del cuerpo y el
     * DropShadow encadenado dibuja el borde de luz naranja alrededor de esa silueta.
     */
    public void silhouette(boolean on) {
        if (on) {
            DropShadow rim = new DropShadow(18, Color.web("#ff9f1c"));
            rim.setSpread(0.35);
            rim.setInput(new ColorAdjust(0, 0, -1, 0));
            bodyAll.setEffect(rim);
        } else bodyAll.setEffect(null);
        aura.setVisible(!on);
    }

    public void grin(boolean on) {
        mouthSad.setVisible(!on); mouthGrin.setVisible(on); teeth.setVisible(on);
    }

    public Node faceOnly() { return face; }

    /** Copia del contorno para el efecto RGB-split del glitch. */
    public static Node splitCopy(Color c) {
        SVGPath s = new SVGPath(); s.setContent(HEART);
        s.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.85)); s.setBlendMode(BlendMode.ADD);
        return s;
    }

    public Animation auraDrift() {
        Timeline t = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(aura.rotateProperty(), -3, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1800), new KeyValue(aura.rotateProperty(), 3, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true); t.setCycleCount(Animation.INDEFINITE);
        return t;
    }
}
