package app.secret.dave;

import javafx.animation.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Araña-mecha de escenario (Daft Punk). No es una araña con casco encima: todo el cuerpo es equipo de concierto.
 * <ul>
 *   <li>Cefalotórax = casco: mitad plata (visor rectangular) y mitad oro (visor largo). La unión es un panel de PCB a
 *       la vista con pistas, vías y una tira de LED que corre al ritmo.</li>
 *   <li>Fémur = tramo de andamio (truss) de escenario; tibia = cable de audio grueso que cuelga y termina en un plug.</li>
 *   <li>Rodillas = mini altavoces; abdomen = caja de subwoofer con el cono latiendo.</li>
 * </ul>
 */
public final class MechaSpiderArt extends Group {

    private static final String CHASSIS = "M -112 -6 C -112 -86 -62 -124 0 -124 C 62 -124 112 -86 112 -6 L 92 52 L 42 74 L -42 74 L -92 52 Z";
    private static final Color SILVER = Color.web("#d6dbe3"), GOLD = Color.web("#f0c040");
    private static final Color LED_CYAN = Color.web("#00e5ff"), LED_PINK = Color.web("#ff2bd6");

    private final List<Group> legs = new ArrayList<>();
    private final Group body = new Group();
    private final List<Rectangle> leds = new ArrayList<>();
    private final Circle cone = new Circle(0, 118, 40);
    private final List<Animation> loops = new ArrayList<>();

    public MechaSpiderArt() {
        Group sub = subwoofer();

        for (int i = 0; i < 8; i++) {
            boolean left = i < 4;
            int k = i % 4;
            double sx = left ? -1 : 1;
            double y0 = -5 + k * 24;
            Point2D hip = new Point2D(sx * 70, y0);
            Point2D knee = new Point2D(sx * (150 + k * 12 - Math.abs(k - 1.5) * 8), y0 - 70 + k * 20);
            Point2D foot = new Point2D(sx * (215 + k * 8), 120 + k * 22);
            Group leg = new Group(truss(hip, knee, 16, left ? SILVER : GOLD, 2.6),
                    cable(knee, foot, 9), jackPlug(foot, left), speakerJoint(knee, 9));
            leg.setUserData(new double[]{hip.getX(), hip.getY()});
            legs.add(leg);
        }

        body.getChildren().add(sub);
        body.getChildren().addAll(legs);
        body.getChildren().addAll(helmet());
        getChildren().add(body);
    }

    // ------------------------------------------------------------------ piezas

    /** Casco plata→oro con la costura convertida en panel de circuitos y tira de LED. */
    private List<Node> helmet() {
        SVGPath chassis = new SVGPath();
        chassis.setContent(CHASSIS);
        // Degradado horizontal en coordenadas absolutas (-112..112): 0–0.42 plata, 0.42–0.58 costura oscura, 0.58–1 oro.
        chassis.setFill(new LinearGradient(-112, 0, 112, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#8e97a4")), new Stop(0.14, Color.web("#f4f6f9")), new Stop(0.34, Color.web("#c3c9d2")),
                new Stop(0.42, Color.web("#9aa2ad")), new Stop(0.43, Color.web("#0b1a14")), new Stop(0.57, Color.web("#0b1a14")),
                new Stop(0.58, Color.web("#e0b23a")), new Stop(0.70, Color.web("#f5c542")), new Stop(0.88, Color.web("#c9921a")),
                new Stop(1.00, Color.web("#7a5410"))));
        chassis.setStroke(Color.web("#0b0c10")); chassis.setStrokeWidth(4);
        chassis.setEffect(new DropShadow(22, Color.color(0, 0, 0, 0.7)));

        // Circuitos: pistas en ángulo recto que salen de la costura hacia ambas mitades, con vías en los extremos.
        Group pcb = new Group();
        double[][] traces = {
                {-10, -110, -10, -90, -34, -90, -34, -70}, {10, -104, 10, -84, 40, -84, 40, -66},
                {-12, -30, -40, -30, -40, -12, -58, -12}, {12, -22, 44, -22, 44, 0, 70, 0},
                {-10, 30, -28, 30, -28, 54}, {10, 40, 30, 40, 30, 62}, {-6, -60, -6, 60}, {6, -60, 6, 60}};
        for (double[] t : traces) {
            Polyline p = new Polyline(t);
            boolean leftSide = t[2] < 0 || t[0] < 0;
            p.setStroke(leftSide ? LED_CYAN : Color.web("#ffb000"));
            p.setStrokeWidth(2); p.setOpacity(0.85);
            p.setEffect(new Glow(0.6));
            Circle via = new Circle(t[t.length - 2], t[t.length - 1], 3.2, Color.web("#0b0c10"));
            via.setStroke(p.getStroke()); via.setStrokeWidth(1.6);
            pcb.getChildren().addAll(p, via);
        }
        // chip central sobre la costura
        Rectangle chip = rect(-14, -8, 28, 22, Color.web("#151515"));
        chip.setStroke(Color.web("#3a3a3a")); chip.setArcWidth(4); chip.setArcHeight(4);
        for (int i = 0; i < 4; i++) {
            pcb.getChildren().addAll(rect(-18, -6 + i * 5, 4, 2, SILVER), rect(14, -6 + i * 5, 4, 2, GOLD));
        }
        pcb.getChildren().add(chip);
        pcb.setClip(chassisShape());

        // Tira de LED integrada a lo largo de la costura (se anima en idle()).
        Group strip = new Group();
        for (int i = 0; i < 9; i++) {
            Rectangle led = new Rectangle(-4, -116 + i * 20, 8, 12);
            led.setArcWidth(4); led.setArcHeight(4);
            led.setFill(i % 2 == 0 ? LED_CYAN : LED_PINK);
            led.setEffect(new Glow(1.0));
            leds.add(led);
            strip.getChildren().add(led);
        }
        strip.setClip(chassisShape());

        Rectangle visorL = new Rectangle(-92, -44, 70, 40);
        visorL.setArcWidth(10); visorL.setArcHeight(10);
        visorL.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#2b3350")), new Stop(0.5, Color.web("#06070b")), new Stop(1, Color.web("#000"))));
        visorL.setStroke(Color.web("#10121a")); visorL.setStrokeWidth(3);
        Polygon glintL = new Polygon(-84, -40, -60, -40, -72, -8, -90, -8);
        glintL.setFill(Color.color(1, 1, 1, 0.22));

        Rectangle visorR = new Rectangle(22, -38, 80, 26);
        visorR.setArcWidth(26); visorR.setArcHeight(26);
        visorR.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1c2236")), new Stop(1, Color.web("#000"))));
        visorR.setStroke(Color.web("#10121a")); visorR.setStrokeWidth(3);
        Polygon glintR = new Polygon(32, -34, 64, -34, 54, -16, 26, -16);
        glintR.setFill(Color.color(1, 1, 1, 0.2));

        // remates de neón inferiores con LEDs puntuales
        Path trimL = new Path(new MoveTo(-100, 40), new LineTo(-60, 62), new LineTo(-16, 66));
        trimL.setStroke(LED_CYAN); trimL.setStrokeWidth(3); trimL.setEffect(new Glow(0.9));
        Path trimR = new Path(new MoveTo(16, 66), new LineTo(60, 62), new LineTo(100, 40));
        trimR.setStroke(LED_PINK); trimR.setStrokeWidth(3); trimR.setEffect(new Glow(0.9));

        return List.of(chassis, pcb, strip, visorL, glintL, visorR, glintR, trimL, trimR);
    }

    static Rectangle rect(double x, double y, double w, double h, Paint fill) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setFill(fill);
        return r;
    }

    /** Un nodo no puede ser clip de dos nodos: cada recorte necesita su propia copia del contorno. */
    private static SVGPath chassisShape() {
        SVGPath s = new SVGPath();
        s.setContent(CHASSIS);
        return s;
    }

    /** Caja de subwoofer con cono, anillos y aro de neón. */
    private Group subwoofer() {
        Rectangle box = new Rectangle(-80, 64, 160, 112);
        box.setArcWidth(22); box.setArcHeight(22);
        box.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#2a2e37")), new Stop(1, Color.web("#0d0e12"))));
        box.setStroke(LED_CYAN); box.setStrokeWidth(3); box.setEffect(new Glow(0.5));
        cone.setFill(new RadialGradient(0, 0, 0, 118, 40, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5a606c")), new Stop(0.35, Color.web("#1b1d22")), new Stop(0.8, Color.web("#30343c")), new Stop(1, Color.web("#0b0c10"))));
        cone.setStroke(LED_PINK); cone.setStrokeWidth(3); cone.setEffect(new Glow(0.9));
        Circle cap = new Circle(0, 118, 12, Color.web("#0b0c10"));
        cap.setStroke(Color.web("#8e97a4")); cap.setStrokeWidth(2);
        Group g = new Group(box, cone, cap);
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 0; i < 3; i++) g.getChildren().add(new Circle(s * 66, 80 + i * 38, 3, Color.web("#8e97a4")));
        }
        return g;
    }

    /**
     * Tramo de andamio: dos largueros paralelos y arriostramiento en zigzag. Los largueros salen de desplazar la recta
     * a lo largo de su NORMAL unitaria n = (-dy, dx)/|d| a ±w/2; el zigzag alterna entre ambos cada ~14 px.
     */
    static Group truss(Point2D a, Point2D b, double w, Color rail, double railWidth) {
        Point2D d = b.subtract(a);
        double len = d.magnitude();
        Point2D n = new Point2D(-d.getY() / len, d.getX() / len).multiply(w / 2);
        Point2D a1 = a.add(n), a2 = a.subtract(n), b1 = b.add(n), b2 = b.subtract(n);
        Group g = new Group();
        Line shadow = new Line(a.getX(), a.getY(), b.getX(), b.getY());
        shadow.setStroke(Color.web("#0d0e12")); shadow.setStrokeWidth(w + 4); shadow.setOpacity(0.55);
        g.getChildren().add(shadow);
        int steps = Math.max(2, (int) (len / 14));
        Polyline zig = new Polyline();
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Point2D p = (i % 2 == 0 ? a1.add(b1.subtract(a1).multiply(t)) : a2.add(b2.subtract(a2).multiply(t)));
            zig.getPoints().addAll(p.getX(), p.getY());
        }
        zig.setStroke(rail.darker()); zig.setStrokeWidth(railWidth * 0.6);
        Line r1 = new Line(a1.getX(), a1.getY(), b1.getX(), b1.getY());
        Line r2 = new Line(a2.getX(), a2.getY(), b2.getX(), b2.getY());
        for (Line r : new Line[]{r1, r2}) { r.setStroke(rail); r.setStrokeWidth(railWidth); r.setStrokeLineCap(StrokeLineCap.ROUND); }
        g.getChildren().addAll(zig, r1, r2);
        return g;
    }

    /**
     * Cable de audio grueso que cuelga por gravedad: curva cuadrática con el punto de control bajo el punto medio
     * (comba = 18 % de la longitud), cuerpo negro + brillo fino desplazado para dar volumen de goma.
     */
    static Group cable(Point2D a, Point2D b, double thick) {
        Point2D mid = a.midpoint(b);
        double sag = a.distance(b) * 0.18;
        QuadCurve body = new QuadCurve(a.getX(), a.getY(), mid.getX(), mid.getY() + sag, b.getX(), b.getY());
        body.setFill(null); body.setStroke(Color.web("#121216")); body.setStrokeWidth(thick); body.setStrokeLineCap(StrokeLineCap.ROUND);
        QuadCurve hi = new QuadCurve(a.getX(), a.getY() - thick * 0.25, mid.getX(), mid.getY() + sag - thick * 0.25, b.getX(), b.getY() - thick * 0.25);
        hi.setFill(null); hi.setStroke(Color.color(1, 1, 1, 0.28)); hi.setStrokeWidth(Math.max(1.5, thick * 0.22)); hi.setStrokeLineCap(StrokeLineCap.ROUND);
        return new Group(body, hi);
    }

    /** Pie = plug de 6.35 mm: funda metálica, anillo aislante y punta. */
    static Group jackPlug(Point2D p, boolean silver) {
        Rectangle sleeve = new Rectangle(p.getX() - 7, p.getY() - 4, 14, 16);
        sleeve.setArcWidth(4); sleeve.setArcHeight(4);
        sleeve.setFill(silver ? SILVER : GOLD); sleeve.setStroke(Color.BLACK); sleeve.setStrokeWidth(1.5);
        Rectangle ring = rect(p.getX() - 4, p.getY() + 12, 8, 3, Color.BLACK);
        Polygon tip = new Polygon(p.getX() - 4, p.getY() + 15, p.getX() + 4, p.getY() + 15, p.getX(), p.getY() + 22);
        tip.setFill(silver ? SILVER : GOLD); tip.setStroke(Color.BLACK);
        return new Group(sleeve, ring, tip);
    }

    /** Rodilla = mini altavoz con aro de neón. */
    static Group speakerJoint(Point2D p, double r) {
        Circle frame = new Circle(p.getX(), p.getY(), r, Color.web("#0d0e12"));
        frame.setStroke(LED_CYAN); frame.setStrokeWidth(2); frame.setEffect(new Glow(0.9));
        Circle c = new Circle(p.getX(), p.getY(), r * 0.45, Color.web("#5a606c"));
        return new Group(frame, c);
    }

    // ------------------------------------------------------------------ animación

    public Group leg(int i) { return legs.get(i); }

    /** Respiración/ritmo: rebote del cuerpo, LEDs en persecución y el cono del subwoofer latiendo. */
    public void idle() {
        stop();
        Timeline bounce = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(body.translateYProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(260), new KeyValue(body.translateYProperty(), -9, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(520), new KeyValue(body.translateYProperty(), 0, Interpolator.EASE_BOTH)));
        bounce.setCycleCount(Animation.INDEFINITE);

        // Persecución: en cada paso k, el LED i se enciende si (i + k) mod 3 == 0 → una ola que sube por la costura.
        int[] k = {0};
        Timeline chase = new Timeline(new KeyFrame(Duration.millis(90), e -> {
            k[0]++;
            for (int i = 0; i < leds.size(); i++) leds.get(i).setOpacity((i + k[0]) % 3 == 0 ? 1.0 : 0.25);
        }));
        chase.setCycleCount(Animation.INDEFINITE);

        ScaleTransition pump = new ScaleTransition(Duration.millis(130), cone);
        pump.setFromX(1); pump.setFromY(1); pump.setToX(1.12); pump.setToY(1.12);
        pump.setAutoReverse(true); pump.setCycleCount(2);
        SequentialTransition beat = new SequentialTransition(pump, new PauseTransition(Duration.millis(260)));
        beat.setCycleCount(Animation.INDEFINITE);

        loops.addAll(List.of(bounce, chase, beat));
        loops.forEach(Animation::play);
    }

    public void stop() { loops.forEach(Animation::stop); loops.clear(); }

    /** Pose de "drop" del DJ: se agacha y luego se estira. */
    public Animation drop() {
        return new SequentialTransition(Cine.scale(this, 90, 1, 0.86, Cine.SLAM), Cine.scale(this, 260, 0.86, 1.0, Cine.OVERSHOOT));
    }

    /** Una pata enorme para el panel de la pisada: andamio + cable + plug. */
    public static Group bigLeg() {
        Point2D hip = new Point2D(0, 0), knee = new Point2D(130, 190), foot = new Point2D(70, 400);
        Group t = truss(hip, knee, 58, Color.web("#f4f6f9"), 9);
        Group c = cable(knee, foot, 34);
        Group plug = jackPlug(foot, true);
        plug.setScaleX(3.2); plug.setScaleY(3.2);
        Circle kneeC = new Circle(knee.getX(), knee.getY(), 36, Color.web("#0d0e12"));
        kneeC.setStroke(LED_CYAN); kneeC.setStrokeWidth(6); kneeC.setEffect(new Glow(0.9));
        Circle coneC = new Circle(knee.getX(), knee.getY(), 18, Color.web("#5a606c"));
        return new Group(t, c, plug, kneeC, coneC);
    }
}
