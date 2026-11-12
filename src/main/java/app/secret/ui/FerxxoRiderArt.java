package app.secret.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.Random;

import static app.secret.ui.FeidSprite.*;

/**
 * Arte de "FERXXO RIDER", todo generado por código (pixel art original, sin imágenes externas).
 * <ul>
 *   <li>{@link #bike(int)}: moto de motocross 60x64 de perfil mirando a la derecha (plásticos verde neón, ruedas de
 *       tacos con 4 cuadros de giro).</li>
 *   <li>{@link #rider(int)}: Feid sentado en la moto, inclinado hacia el manubrio, en el MISMO marco de 60x64 (se pinta
 *       encima de la moto y se puede separar para la cinemática de bajarse).</li>
 *   <li>{@link #cone()}, {@link #barrier()}, {@link #pothole()}: obstáculos del juego; {@link #cafexito(int)}: el
 *       power-up.</li>
 *   <li>{@link #neonA()} / {@link #neonB()}: letreros de neón de la capa media (dos juegos que parpadean distinto);
 *       {@link #metro()}: el tren del Metro que cruza por el viaducto de vez en cuando.</li>
 *   <li>Fondos a 320x180 (se muestran x4 sin suavizado): el callejón de la cinemática y las tres capas parallax de
 *       Medellín de noche (montañas con las luces de las comunas, edificios con el Metrocable, y la calle). Las capas
 *       del juego son PERIÓDICAS en 320: dos copias seguidas empalman sin costura.</li>
 * </ul>
 * La paleta de la moto y del piloto es la de {@link FeidSprite#PALETTE} (verde neón #00FF66).
 */
public final class FerxxoRiderArt {

    private FerxxoRiderArt() {}

    // ================================================================== moto + piloto (marco común 60x64)

    public static final int RW = 60, RH = 64, FRAMES = 4;
    public static final int REAR_X = 13, FRONT_X = 46, AXLE_Y = 55, WHEEL_R = 8;
    /** Fila del contacto de las ruedas con el piso (borde inferior del marco). */
    public static final int CONTACT_Y = AXLE_Y + WHEEL_R;
    /** Cadera del piloto sentado y centro de su cabeza (en celdas del marco). */
    public static final int HIP_X = 22, HIP_Y = 37, HEAD_X = 33, HEAD_Y = 14;

    /** Moto con las ruedas en el cuadro de giro {@code frame} (0..3). */
    public static PixelGrid bike(int frame) {
        PixelGrid p = new PixelGrid(RW, RH);
        p.line(REAR_X, AXLE_Y, 27, 48, CHAIN, 1);                       // basculante
        p.line(22, 46, 7, 39, CHAIN, 1);                                 // escape alto de motocross
        p.rect(3, 37, 5, 3, VEST_HI);
        wheel(p, REAR_X, AXLE_Y);
        wheel(p, FRONT_X, AXLE_Y);
        p.line(FRONT_X, AXLE_Y, 40, 34, CHAIN, 1);                      // barras de la horquilla
        p.line(40, 34, 38, 30, VEST_HI, 1);                             // pipa de dirección
        p.line(35, 30, 42, 28, CAP, 0);                                 // manubrio
        p.rect(41, 27, 2, 2, CAP);
        p.rect(23, 44, 11, 7, PANTS_HI);                                // motor
        p.rect(24, 45, 9, 1, CAP_HI);
        for (int x = 24; x < 33; x += 2) p.set(x, 48, CAP);              // aletas del cilindro
        p.line(38, 35, 31, 45, VEST, 1);                                // tubo del chasis
        p.poly(new double[]{3, 16, 26, 25, 12}, new double[]{34, 37, 39, 44, 43}, NEON);      // guardabarros trasero
        p.poly(new double[]{10, 30, 31, 12}, new double[]{35, 36, 39, 38}, CAP);              // sillín
        p.poly(new double[]{26, 37, 39, 31, 25}, new double[]{38, 33, 38, 45, 44}, NEON);     // tanque / tapas
        p.line(28, 41, 35, 36, TEE, 0);                                 // franja del tanque
        p.rect(15, 39, 7, 4, FRAME);                                    // placa lateral con el número
        p.rect(17, 40, 1, 2, CAP); p.rect(19, 40, 2, 1, CAP); p.rect(20, 41, 1, 1, CAP);
        p.poly(new double[]{40, 44, 45, 41}, new double[]{29, 30, 37, 36}, FRAME);            // placa frontal
        p.line(41, 31, 44, 32, NEON, 0);
        p.poly(new double[]{41, 53, 56, 46}, new double[]{43, 42, 44, 45}, NEON);             // guardabarros delantero
        p.rect(27, 50, 3, 1, CHAIN);                                    // estribo
        p.outline(OUT);
        spokes(p, REAR_X, AXLE_Y, frame);                               // después del contorno: rayos finos
        spokes(p, FRONT_X, AXLE_Y, frame);
        return p;
    }

    private static void wheel(PixelGrid p, int cx, int cy) {
        p.ellipse(cx, cy, WHEEL_R, WHEEL_R, CAP);                       // llanta de tacos
        p.ellipse(cx, cy, 5.6, 5.6, CHAIN);                             // rin
        p.ellipse(cx, cy, 4.4, 4.4, 0);                                 // hueco (se ve el fondo)
    }

    private static void spokes(PixelGrid p, int cx, int cy, int frame) {
        double a0 = frame * Math.PI / 16;                               // 4 cuadros = 1/8 de vuelta (los rayos repiten cada 45°)
        for (int k = 0; k < 4; k++) {
            double a = a0 + k * Math.PI / 4;
            int dx = (int) Math.round(Math.cos(a) * 4), dy = (int) Math.round(Math.sin(a) * 4);
            p.line(cx - dx, cy - dy, cx + dx, cy + dy, CHAIN, 0);
        }
        for (int k = 0; k < 12; k++) {                                  // tacos de la llanta (giran con la rueda)
            double a = frame * Math.PI / 24 + k * Math.PI / 6;
            p.set(cx + (int) Math.round(Math.cos(a) * 8.6), cy + (int) Math.round(Math.sin(a) * 8.6), OUT);
        }
        p.set(cx, cy, NEON);                                            // buje
    }

    /** Feid sentado en la moto, agachado hacia el manubrio. {@code bob} (0/1) lo sacude con el motor. */
    public static PixelGrid rider(int bob) {
        PixelGrid p = new PixelGrid(RW, RH);
        int y = bob;
        // pierna (pantalón) de la cadera a la rodilla y al estribo; tenis blanco de suela verde
        p.line(HIP_X - 1, HIP_Y + y, 30, 41 + y, PANTS_HI, 1);
        p.line(HIP_X - 1, HIP_Y + 1 + y, 30, 42 + y, PANTS, 0);       // pliegue en sombra bajo el muslo
        p.line(30, 41 + y, 28, 48, PANTS_HI, 1);
        p.line(29, 42 + y, 27, 48, PANTS, 0);
        p.set(31, 40 + y, VEST_HI);                                     // brillo en la rodilla
        p.rect(26, 48, 6, 2, SHOE);
        p.rect(26, 48, 2, 1, FRAME_SH);
        p.rect(26, 50, 7, 1, SOLE);
        p.set(29, 48, NEON);
        // torso inclinado: camiseta verde + chaleco táctico con correa neón y luz en la espalda
        p.poly(new double[]{17, 25, 34, 27}, new double[]{38 + y, 38 + y, 24 + y, 21 + y}, TEE);
        p.line(25, 37 + y, 33, 25 + y, TEE_D, 0);
        p.poly(new double[]{17, 22, 30, 27}, new double[]{38 + y, 38 + y, 23 + y, 21 + y}, VEST);
        p.line(18, 37 + y, 27, 22 + y, VEST_HI, 0);                     // luz sobre la espalda curvada
        p.line(20, 36 + y, 28, 23 + y, NEON, 0);
        p.poly(new double[]{17, 13, 16}, new double[]{27 + y, 31 + y, 34 + y}, VEST);    // el chaleco al viento
        p.set(14, 31 + y, VEST_HI);
        // cuello, cadena y cabeza
        p.rect(30, 18 + y, 4, 5, SKIN_D);
        p.rect(30, 18 + y, 1, 5, SKIN_DD);
        p.line(28, 22 + y, 34, 21 + y, CHAIN, 0);
        p.set(31, 22 + y, STUD);
        head(p, HEAD_X - 19, HEAD_Y - 12 + y);
        // brazo hacia el puño del manubrio (manga tatuada en bandas + luz de borde)
        p.line(30, 25 + y, 33, 31 + y, SKIN, 1);
        p.line(33, 31 + y, 39, 29, SKIN, 1);
        p.line(34, 30 + y, 38, 28, SKIN_L, 0);
        p.rect(29, 24 + y, 3, 3, TEE);
        p.set(29, 26 + y, TEE_D);
        p.set(32, 28 + y, INK); p.set(31, 29 + y, INK); p.set(35, 31 + y, INK); p.set(36, 30, INK); p.set(35, 30, INK);
        p.rect(38, 28, 3, 3, SKIN);
        p.set(40, 28, SKIN_D);
        p.set(39, 30, GOLD);                                            // anillo sobre el puño
        p.outline(OUT);
        return p;
    }

    /** La cabeza de perfil de {@link FeidSprite} (la misma del cuerpo de pie), desplazada. */
    private static void head(PixelGrid p, int dx, int dy) { FeidSprite.headProfile(p, dx, dy); }

    // ================================================================== obstáculos

    public static final int O_OUT = 1, O_ORANGE = 2, O_ORANGE_D = 3, O_WHITE = 4, O_GRAY = 5, O_HOLE = 6, O_RIM = 7,
            O_NEON = 8, O_YELLOW = 9, O_GRAY_L = 10;
    public static final int[] OBST_PALETTE = {0x000000, 0x0b0b0b, 0xff6a00, 0xa84300, 0xf2f2f2, 0x3a3a3a, 0x030303,
            0x26292c, 0x00ff66, 0xffd400, 0x5a5f64};

    /** Cono de tránsito 16x22 con bandas reflectivas. */
    public static PixelGrid cone() {
        PixelGrid p = new PixelGrid(16, 22);
        p.poly(new double[]{6, 10, 14, 2}, new double[]{1, 1, 19, 19}, O_ORANGE);
        for (int y = 6; y < 19; y++) {
            boolean band = (y >= 6 && y <= 8) || (y >= 12 && y <= 14);
            for (int x = 0; x < 16; x++) {
                if (p.get(x, y) != O_ORANGE) continue;
                if (band) p.set(x, y, O_WHITE);
                else if (x <= 5) p.set(x, y, O_ORANGE_D);                // sombra del lado izquierdo
            }
        }
        p.rect(0, 19, 16, 3, O_ORANGE_D);
        p.set(8, 1, O_NEON);
        p.outline(O_OUT);
        return p;
    }

    /** Valla de tránsito 34x24: tablero a rayas naranja/blanco, patas en A y luz amarilla. */
    public static PixelGrid barrier() {
        PixelGrid p = new PixelGrid(34, 24);
        p.line(5, 11, 2, 23, O_GRAY, 0); p.line(5, 11, 8, 23, O_GRAY, 0);
        p.line(28, 11, 25, 23, O_GRAY, 0); p.line(28, 11, 31, 23, O_GRAY, 0);
        p.rect(1, 23, 9, 1, O_GRAY_L); p.rect(24, 23, 9, 1, O_GRAY_L);
        for (int y = 3; y < 11; y++)
            for (int x = 1; x < 33; x++) p.set(x, y, ((x + y) / 4) % 2 == 0 ? O_ORANGE : O_WHITE);
        p.rect(1, 15, 32, 2, O_GRAY_L);                                 // travesaño inferior
        p.rect(15, 0, 4, 3, O_YELLOW);
        p.outline(O_OUT);
        return p;
    }

    /** Hueco en la calle 40x8 (se ve desde un ángulo bajo) con agua que refleja el neón. */
    public static PixelGrid pothole() {
        PixelGrid p = new PixelGrid(40, 8);
        p.ellipse(20, 4, 19, 3.4, O_RIM);
        p.ellipse(20, 4.6, 16, 2.4, O_HOLE);
        for (int x = 9; x < 30; x += 4) p.set(x, 5, O_NEON);
        p.rect(13, 4, 5, 1, O_NEON);
        return p;
    }

    // ================================================================== power-up: "El Cafexito"

    public static final int C_OUT = 1, C_CUP = 2, C_CUP_D = 3, C_SLEEVE = 4, C_SLEEVE_D = 5, C_LID = 6, C_LID_HI = 7,
            C_STEAM = 8, C_NEON = 9, C_HOLE = 10;
    public static final int[] CAFE_PALETTE = {0x000000, 0x0b0b0b, 0xf7f3e8, 0xc9c2b0, 0xb07a45, 0x7a4f2a, 0x00c752,
            0x9effc6, 0xe8f5ee, 0x00ff66, 0x05140c};
    public static final int CAFE_W = 16, CAFE_H = 22;

    /**
     * "El Cafexito": vaso de tinto para llevar 16x22, tapa verde Ferxxo, manga de cartón kraft con un corazón neón y el
     * vapor que se enrosca ({@code frame} 0..1 alterna la voluta).
     */
    public static PixelGrid cafexito(int frame) {
        PixelGrid p = new PixelGrid(CAFE_W, CAFE_H);
        // vaso cónico: blanco con sombra a la izquierda
        p.poly(new double[]{3, 13, 11, 5}, new double[]{9, 9, 21, 21}, C_CUP);
        p.poly(new double[]{3, 6, 6.5, 5}, new double[]{9, 9, 21, 21}, C_CUP_D);
        // manga de cartón con corazón neón
        p.poly(new double[]{3.5, 12.5, 11.8, 4.2}, new double[]{12, 12, 17, 17}, C_SLEEVE);
        p.rect(4, 16, 8, 1, C_SLEEVE_D);
        p.rect(4, 12, 2, 5, C_SLEEVE_D);
        p.set(8, 13, C_NEON); p.set(10, 13, C_NEON);
        p.rect(8, 14, 3, 1, C_NEON);
        p.set(9, 15, C_NEON);
        // tapa con borde y orificio para sorber
        p.rect(2, 7, 12, 2, C_LID);
        p.rect(4, 6, 8, 1, C_LID);
        p.rect(3, 7, 10, 1, C_LID_HI);
        p.rect(9, 6, 2, 1, C_HOLE);
        // vapor
        int[][] steam = frame == 0
                ? new int[][]{{9, 4}, {8, 3}, {8, 2}, {9, 1}, {10, 0}, {6, 4}, {6, 3}, {7, 2}}
                : new int[][]{{9, 4}, {10, 3}, {10, 2}, {9, 1}, {8, 0}, {6, 4}, {5, 3}, {5, 2}};
        for (int[] s : steam) p.set(s[0], s[1], C_STEAM);
        p.outline(C_OUT);
        return p;
    }

    // ================================================================== Metro de Medellín (tren de 3 vagones)

    public static final int METRO_W = 146, METRO_H = 13;

    /** Tren elevado de 3 vagones (146x13 en la escala 320x180): plateado, franja verde, ventanas encendidas. */
    public static Image metro() {
        Canvas320 c = new Canvas320();
        for (int car = 0; car < 3; car++) {
            int x0 = car * 49, w = 47;
            boolean head = car == 0, tail = car == 2;
            for (int y = 1; y < 12; y++)
                for (int x = x0; x < x0 + w; x++) {
                    boolean nose = (head && x - x0 < 4 && y < 4 - (x - x0)) || (tail && x0 + w - 1 - x < 4 && y < 4 - (x0 + w - 1 - x));
                    if (nose) continue;
                    int col = y < 3 ? 0xdfe5e8 : y < 8 ? 0xc3ccd1 : 0x8c979e;
                    c.set(x, y, col);
                }
            for (int x = x0 + 4; x < x0 + w - 4; x++) {                     // ventanas encendidas con parantes
                boolean post = (x - x0) % 7 == 3;
                c.set(x, 4, post ? 0x5c666c : 0xfff1c4);
                c.set(x, 5, post ? 0x5c666c : 0xf0d78f);
            }
            for (int x = x0 + 1; x < x0 + w - 1; x++) c.set(x, 8, 0x00a86b);   // franja verde
            for (int k = 0; k < 2; k++) {                                       // puertas
                int dx = x0 + 14 + k * 18;
                for (int y = 3; y < 11; y++) { c.set(dx, y, 0x6d777d); c.set(dx + 3, y, 0x6d777d); }
            }
            for (int x : new int[]{x0 + 6, x0 + 10, x0 + w - 11, x0 + w - 7}) {   // ruedas / bogies
                c.set(x, 12, 0x1a1d20); c.set(x + 1, 12, 0x1a1d20);
            }
            c.set(x0 + w / 2, 0, 0x5c666c);                                     // pantógrafo
            c.set(x0 + w / 2 - 1, 0, 0x5c666c);
            if (car < 2) { c.set(x0 + w, 9, 0x1a1d20); c.set(x0 + w + 1, 9, 0x1a1d20); }   // acople
        }
        c.set(1, 6, 0xffffcc); c.set(1, 7, 0xffffcc);                           // faros en las dos puntas
        c.set(METRO_W - 2, 6, 0xff3b3b); c.set(METRO_W - 2, 7, 0xff3b3b);
        // recorta a 146x13 (Canvas320 es de 320x180)
        WritableImage img = new WritableImage(METRO_W, METRO_H);
        img.getPixelWriter().setPixels(0, 0, METRO_W, METRO_H, javafx.scene.image.PixelFormat.getIntArgbInstance(), c.px, 0, LW);
        return img;
    }

    /** Fila (escala 320x180) del tablero del viaducto del Metro: el tren apoya sus ruedas encima. */
    public static final int VIADUCT_Y = 101;

    // ================================================================== letreros de neón (capa media, parpadean)

    /** Fuente de píxeles 3x5 para los letreros (solo las letras que se usan). */
    private static final java.util.Map<Character, String> FONT = java.util.Map.ofEntries(
            java.util.Map.entry('A', "010101111101101"), java.util.Map.entry('B', "110101110101110"),
            java.util.Map.entry('C', "011100100100011"), java.util.Map.entry('E', "111100110100111"),
            java.util.Map.entry('F', "111100110100100"), java.util.Map.entry('H', "101101111101101"),
            java.util.Map.entry('I', "111010010010111"), java.util.Map.entry('M', "101111111101101"),
            java.util.Map.entry('N', "110101101101101"), java.util.Map.entry('O', "111101101101111"),
            java.util.Map.entry('P', "110101110100100"), java.util.Map.entry('R', "110101110101101"),
            java.util.Map.entry('T', "111010010010010"), java.util.Map.entry('X', "101101010101101"),
            java.util.Map.entry('2', "110001010100111"), java.util.Map.entry('4', "101101111001001"));

    private static volatile Image neonA, neonB;

    /** Letreros que zumban (casi siempre encendidos, con caídas breves). */
    public static Image neonA() { Image i = neonA; return i != null ? i : (neonA = paintNeon(new Object[][]{
            {12, 90, "BAR", 0xff2e88}, {124, 66, "FERXXO", 0x00ff66}, {236, 94, "24H", 0x00c3ff}})); }

    /** Letreros que parpadean on/off. */
    public static Image neonB() { Image i = neonB; return i != null ? i : (neonB = paintNeon(new Object[][]{
            {62, 80, "TINTO", 0xffd400}, {166, 74, "PARCE", 0xff2e88}, {284, 86, "MOR", 0x00ff66}})); }

    /** Cada letrero: tablero oscuro, marco neón tenue, letras brillantes y un halo suave alrededor. Periódica en 320. */
    private static Image paintNeon(Object[][] signs) {
        Canvas320 c = new Canvas320();
        for (Object[] s : signs) {
            int x0 = (Integer) s[0], y0 = (Integer) s[1], col = (Integer) s[3];
            String text = (String) s[2];
            int w = text.length() * 4 + 3, h = 9;
            for (int y = y0 - 5; y < y0 + h + 5; y++)                       // halo
                for (int x = x0 - 5; x < x0 + w + 5; x++) {
                    double dx = Math.max(0, Math.max(x0 - x, x - (x0 + w - 1))), dy = Math.max(0, Math.max(y0 - y, y - (y0 + h - 1)));
                    double d = Math.hypot(dx, dy);
                    if (d > 0) c.blend(x, y, col, 0.32 * Math.exp(-d / 2.2));
                }
            c.rect(x0, y0, w, h, 0x07090b);                                  // tablero
            for (int x = x0; x < x0 + w; x++) { c.wrap(x, y0, lerp(0x07090b, col, 0.45)); c.wrap(x, y0 + h - 1, lerp(0x07090b, col, 0.45)); }
            for (int y = y0; y < y0 + h; y++) { c.wrap(x0, y, lerp(0x07090b, col, 0.45)); c.wrap(x0 + w - 1, y, lerp(0x07090b, col, 0.45)); }
            for (int i = 0; i < text.length(); i++) {
                String g = FONT.get(text.charAt(i));
                if (g == null) continue;
                for (int r = 0; r < 5; r++)
                    for (int k = 0; k < 3; k++)
                        if (g.charAt(r * 3 + k) == '1') c.wrap(x0 + 2 + i * 4 + k, y0 + 2 + r, lerp(col, 0xffffff, 0.35));
            }
            c.rect(x0 + w / 2, y0 + h, 1, 6, 0x1d2428);                      // soporte
        }
        return c.image();
    }

    /** Grid indexado → imagen con transparencia (el índice 0 es transparente). */
    public static WritableImage image(PixelGrid p, int[] palette) {
        WritableImage img = new WritableImage(p.w, p.h);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < p.h; y++)
            for (int x = 0; x < p.w; x++) {
                int v = p.g[y][x];
                pw.setArgb(x, y, v == 0 ? 0 : 0xff000000 | palette[Math.min(v, palette.length - 1)]);
            }
        return img;
    }

    // ================================================================== fondos 320x180 (x4 en pantalla)

    public static final int LW = 320, LH = 180;
    /** Fila (en el lienzo 320x180) donde las ruedas tocan la calle en el juego. */
    public static final int ROAD_Y = 157;

    private static volatile Image alley, sky, city, street;

    /** Pinta el callejón y las tres capas del juego en un hilo de fondo tras la intro (WritableImage admite cualquier hilo). */
    public static void prepareInBackground() {
        Thread t = new Thread(() -> { alley(); sky(); city(); street(); neonA(); neonB(); }, "rebusque-ferxxo-rider");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public static Image alley() { Image i = alley; return i != null ? i : (alley = paintAlley()); }
    public static Image sky() { Image i = sky; return i != null ? i : (sky = paintSky()); }
    public static Image city() { Image i = city; return i != null ? i : (city = paintCity()); }
    public static Image street() { Image i = street; return i != null ? i : (street = paintStreet()); }

    /** Lienzo ARGB con mezcla manual (todo se pinta en un int[] y se vuelca una sola vez). */
    private static final class Canvas320 {
        final int[] px = new int[LW * LH];
        void set(int x, int y, int rgb) { if (x >= 0 && y >= 0 && x < LW && y < LH) px[y * LW + x] = 0xff000000 | rgb; }
        void wrap(int x, int y, int rgb) { set(Math.floorMod(x, LW), y, rgb); }
        void rect(int x, int y, int w, int h, int rgb) { for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) wrap(i, j, rgb); }
        /** Mezcla {@code rgb} encima con opacidad {@code a} (si el píxel está vacío, queda semitransparente). */
        void blend(int x, int y, int rgb, double a) {
            x = Math.floorMod(x, LW);
            if (y < 0 || y >= LH || a <= 0) return;
            int under = px[y * LW + x];
            int ua = under >>> 24;
            if (ua == 0) { px[y * LW + x] = ((int) Math.round(Math.min(1, a) * 255) << 24) | rgb; return; }
            px[y * LW + x] = 0xff000000 | lerp(under & 0xffffff, rgb, Math.min(1, a));
        }
        Image image() {
            WritableImage img = new WritableImage(LW, LH);
            PixelWriter pw = img.getPixelWriter();
            pw.setPixels(0, 0, LW, LH, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, LW);
            return img;
        }
    }

    private static final int[][] BAYER = {{0, 2}, {3, 1}};

    private static boolean dither(int x, int y, double t) { return t > (BAYER[y & 1][x & 1] + 0.5) / 4.0; }

    static int lerp(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * El callejón de la cinemática: pared de ladrillo bañada por la luz verde del letrero (x=160, y=46), ventana con
     * rejas, aire acondicionado, el portón del taller, bajante, contenedor de basura, grafitis, cables y el asfalto
     * mojado con el reflejo verde del neón. El piso (bordillo) está en la fila 140.
     */
    private static Image paintAlley() {
        Canvas320 c = new Canvas320();
        Random r = new Random(4040);
        for (int y = 0; y < LH; y++)
            for (int x = 0; x < LW; x++) {
                int col;
                if (y < 14) {
                    col = dither(x, y, y / 14.0 * 0.6) ? 0x0a1a12 : 0x04070a;
                } else if (y < 140) {
                    int row = (y - 14) / 5, off = (row % 2) * 5;
                    boolean mortar = (y - 14) % 5 == 4 || (x + off) % 10 == 9;
                    int base = mortar ? 0x0b0909 : (((x + off) / 10 + row) % 3 == 0 ? 0x221917 : 0x1b1513);
                    double d = Math.hypot(x - 160, (y - 46) * 1.3);
                    double wash = Math.exp(-d / 70) * 0.75 + Math.exp(-Math.hypot(x - 52, y - 30) / 40) * 0.25;
                    col = lerp(base, 0x0f5a31, wash * 0.8);
                } else {
                    int base = r.nextInt(6) == 0 ? 0x121518 : 0x0b0d0f;
                    double refl = Math.exp(-Math.abs(x - 160) / 26.0) * (1 - (y - 140) / 48.0);
                    if (dither(x, y, refl * 0.9) && (x % 3 != 0)) base = lerp(base, 0x00ff66, 0.35 + refl * 0.3);
                    double pool = Math.exp(-Math.hypot((x - 52) / 2.0, y - 146) / 14.0);       // charco bajo el farol
                    col = lerp(base, 0x9effc6, pool * 0.25);
                }
                c.set(x, y, col);
            }
        for (int x = 0; x < LW; x++) { c.set(x, 140, 0x2b2e31); c.set(x, 141, 0x1c1f22); }
        // farol de pared (su cono de luz es un nodo aparte en la cinemática)
        c.rect(56, 20, 2, 9, 0x262b2f);
        c.rect(44, 26, 14, 3, 0x3a4046);
        c.rect(46, 29, 10, 1, 0xeafff2);
        // ventana con rejas (luz verde por dentro)
        c.rect(22, 40, 38, 32, 0x2a2f33);
        c.rect(24, 42, 34, 28, 0x08261a);
        for (int x = 26; x < 58; x += 5) c.rect(x, 42, 1, 28, 0x2a2f33);
        c.rect(24, 55, 34, 1, 0x2a2f33);
        // aire acondicionado
        c.rect(28, 78, 24, 14, 0x3a3f44);
        for (int y = 80; y < 90; y += 2) c.rect(30, y, 14, 1, 0x24282c);
        c.rect(46, 81, 4, 4, 0x24282c);
        c.rect(50, 92, 1, 30, 0x1a1d20);                                    // goteo del aire
        // portón del taller de motos
        c.rect(196, 66, 60, 74, 0x23272b);
        c.rect(199, 70, 54, 70, 0x15191c);
        for (int y = 72; y < 140; y += 3) c.rect(199, y, 54, 1, 0x0f1214);
        c.rect(199, 70, 54, 2, 0x00ff66);                                   // tira de neón sobre el portón
        // bajante y contenedor
        c.rect(268, 14, 3, 126, 0x2a2f33);
        for (int y = 30; y < 140; y += 22) c.rect(267, y, 5, 2, 0x3a3f44);
        c.rect(282, 110, 36, 30, 0x0f2a1a);
        c.rect(280, 106, 40, 5, 0x123824);
        c.rect(286, 116, 28, 1, 0x0a1f13);
        // grafitis (tags garabateados)
        int[] tagCols = {0xff2e88, 0x00ff66, 0xffd400, 0x00c3ff};
        for (int k = 0; k < 5; k++) {
            int x0 = 70 + k * 24 + r.nextInt(8), y0 = 100 + r.nextInt(20), col = tagCols[k % tagCols.length];
            int x = x0, y = y0;
            for (int i = 0; i < 26; i++) {
                x += r.nextInt(3) - (i % 6 < 3 ? 0 : 1);
                y += r.nextInt(5) - 2;
                y = Math.max(y0 - 8, Math.min(y0 + 8, y));
                c.blend(x, y, col, 0.55);
                c.blend(x + 1, y, col, 0.35);
            }
        }
        // cables que cruzan el callejón
        for (int x = 0; x < LW; x++) {
            c.set(x, 6 + (int) Math.round(5 * Math.sin(Math.PI * x / LW)), 0x1a1d20);
            c.set(x, 10 + (int) Math.round(3 * Math.sin(Math.PI * x / LW + 0.4)), 0x15181b);
        }
        return c.image();
    }

    /**
     * Capa lejana: cielo nocturno con bruma verde, luna, estrellas y las montañas del valle de Aburrá llenas de luces
     * (las comunas trepando las laderas). Periódica en 320.
     */
    private static Image paintSky() {
        Canvas320 c = new Canvas320();
        Random r = new Random(5005);
        double tau = Math.PI * 2 / LW;
        int[] far = new int[LW], near = new int[LW];
        for (int x = 0; x < LW; x++) {
            far[x] = (int) Math.round(88 + 10 * Math.sin(tau * x) + 6 * Math.sin(tau * 3 * x + 1.3) + 3 * Math.sin(tau * 7 * x + 0.4));
            near[x] = (int) Math.round(112 + 8 * Math.sin(tau * 2 * x + 2) + 4 * Math.sin(tau * 5 * x));
        }
        for (int y = 0; y < LH; y++)
            for (int x = 0; x < LW; x++) {
                double t = Math.pow(y / 120.0, 1.6);
                int col = lerp(0x020509, 0x0b2a1a, t);
                if (dither(x, y, (t * 8) % 1) && t < 1) col = lerp(0x020509, 0x0b2a1a, t + 0.06);
                if (y >= far[x]) col = lerp(0x061410, 0x0b2a1a, Math.max(0, (y - 150) / 30.0));
                if (y >= near[x]) col = lerp(0x040c09, 0x0d3320, Math.max(0, (y - 150) / 40.0));
                c.set(x, y, col);
            }
        for (int i = 0; i < 70; i++) c.set(r.nextInt(LW), r.nextInt(70), r.nextInt(5) == 0 ? 0xeafff2 : 0x5f7a6c);
        for (int y = -9; y <= 9; y++)                                       // luna
            for (int x = -9; x <= 9; x++) {
                if (x * x + y * y > 81) continue;
                int col = (x + 3) * (x + 3) + (y - 2) * (y - 2) < 6 || (x - 4) * (x - 4) + (y + 3) * (y + 3) < 3 ? 0xb7d9c4 : 0xd8ffe8;
                c.set(250 + x, 28 + y, col);
            }
        for (int i = 0; i < 1100; i++) {                                    // luces de las comunas en las laderas
            int x = r.nextInt(LW);
            double u = Math.sqrt(r.nextDouble());
            int y = far[x] + 2 + (int) ((LH - far[x] - 2) * u);
            if (y >= LH) continue;
            boolean onNear = y >= near[x] + 1;
            if (!onNear && y >= near[x] - 1) continue;
            double k = r.nextDouble();
            int col = k < 0.62 ? 0xffc766 : k < 0.8 ? 0xcfe8ff : 0x00ff66;
            c.set(x, y, lerp(0x061410, col, onNear ? 0.95 : 0.55 + 0.4 * u));
        }
        return c.image();
    }

    /**
     * Capa media (transparente arriba): edificios con ventanas encendidas, letreros verdes en las azoteas y el
     * Metrocable (torres cada 160 px, cable con catenaria y dos cabinas iluminadas). Periódica en 320.
     */
    private static Image paintCity() {
        Canvas320 c = new Canvas320();
        Random r = new Random(6006);
        int x = 0;
        while (x < LW) {
            int w = 12 + r.nextInt(22);
            if (LW - (x + w) < 10) w = LW - x;                              // el último cierra exacto en 320
            int top = 76 + r.nextInt(48);                                   // bajos: detrás se ven las comunas
            int body = (x / 7) % 2 == 0 ? 0x0a1013 : 0x0c1316;
            c.rect(x, top, w, 150 - top, body);
            for (int wy = top + 4; wy < 146; wy += 5)
                for (int wx = x + 2; wx < x + w - 2; wx += 4) {
                    double k = r.nextDouble();
                    int col = k < 0.62 ? 0x10181c : k < 0.88 ? 0xe8b85a : k < 0.95 ? 0x00ff66 : 0xeafff2;
                    if (col != 0x10181c) col = lerp(body, col, 0.55 + r.nextDouble() * 0.4);
                    c.rect(wx, wy, 2, 2, col);
                }
            if (r.nextInt(5) == 0) c.rect(x + 2, top - 4, Math.min(w - 4, 14), 2, 0x00ff66);
            if (r.nextInt(3) == 0) c.rect(x + w / 2, top - 7, 1, 7, 0x1d2428);
            x += w;
        }
        // viaducto del Metro (el tren pasa por encima, ver metro()): tablero de concreto + pilas cada 64 px
        for (int px0 = 20; px0 < LW; px0 += 64)
            for (int y = VIADUCT_Y + 4; y < 150; y++) {
                c.wrap(px0, y, 0x1f262b); c.wrap(px0 + 1, y, 0x262e34); c.wrap(px0 + 2, y, 0x262e34); c.wrap(px0 + 3, y, 0x181e22);
            }
        for (int xx = 0; xx < LW; xx++) {
            c.set(xx, VIADUCT_Y, 0x4a545b);
            c.set(xx, VIADUCT_Y + 1, 0x2f383e);
            c.set(xx, VIADUCT_Y + 2, 0x2a3136);
            c.set(xx, VIADUCT_Y + 3, xx % 8 == 0 ? 0x00ff66 : 0x161b1f);          // luces de seguridad del borde
        }
        for (int tx : new int[]{40, 200}) {                                 // torres del Metrocable
            for (int y = 34; y < 150; y++) {
                c.wrap(tx, y, 0x1d2428); c.wrap(tx + 3, y, 0x1d2428);
                if ((y - 34) % 6 < 3) c.wrap(tx + 1 + (y % 2), y, 0x161c1f);
            }
            c.rect(tx - 3, 33, 10, 2, 0x2c3439);
        }
        int[] cable = new int[LW];
        for (int i = 0; i < LW; i++) {
            cable[i] = 34 + (int) Math.round(9 * Math.sin(Math.PI * Math.floorMod(i - 40, 160) / 160.0));
            c.set(i, cable[i], 0x2c3439);
        }
        for (int cx : new int[]{104, 262}) {                                // cabinas
            int cy = cable[cx];
            c.rect(cx, cy, 1, 4, 0x2c3439);
            c.rect(cx - 5, cy + 4, 11, 8, 0x1b2a22);
            c.rect(cx - 4, cy + 6, 9, 3, 0x9effc6);
            c.rect(cx - 5, cy + 4, 11, 1, 0x00ff66);
        }
        return c.image();
    }

    /**
     * Capa cercana: muro con grafitis, faroles con halo verde, andén, bordillo y la calle de asfalto mojado con la
     * línea discontinua y los reflejos de los faroles. Las ruedas tocan la fila {@link #ROAD_Y}. Periódica en 320.
     */
    private static Image paintStreet() {
        Canvas320 c = new Canvas320();
        Random r = new Random(7007);
        for (int y = 112; y < 138; y++)                                     // muro de bloques
            for (int x = 0; x < LW; x++) {
                boolean joint = (y - 112) % 7 == 6 || (x + ((y - 112) / 7 % 2) * 8) % 16 == 15;
                c.set(x, y, joint ? 0x0f1113 : (r.nextInt(9) == 0 ? 0x1b1f22 : 0x16191c));
            }
        for (int x = 0; x < LW; x++) c.set(x, 111, 0x2a2f33);
        int[] tagCols = {0x00ff66, 0xff2e88, 0xffd400, 0x00c3ff};
        for (int k = 0; k < 6; k++) {                                       // grafitis del muro
            int x0 = k * 53 + 6 + r.nextInt(14), y0 = 118 + r.nextInt(10), col = tagCols[k % tagCols.length];
            int x = x0, y = y0;
            for (int i = 0; i < 34; i++) {
                x += r.nextInt(3) - (i % 8 < 4 ? 0 : 1);
                y = Math.max(114, Math.min(135, y + r.nextInt(5) - 2));
                c.blend(x, y, col, 0.7);
                c.blend(x, y + 1, col, 0.45);
            }
        }
        for (int y = 138; y < 146; y++)                                     // andén
            for (int x = 0; x < LW; x++) c.set(x, y, x % 16 == 0 ? 0x181b1e : 0x22262a);
        for (int x = 0; x < LW; x++) { c.set(x, 146, 0x6b737a); c.set(x, 147, 0x4a5056); }
        for (int y = 148; y < LH; y++)                                      // calle
            for (int x = 0; x < LW; x++) {
                int col = r.nextInt(7) == 0 ? 0x15181b : 0x0e1012;
                if (y >= 167 && y <= 168 && x % 40 < 20) col = 0xbfc2b0;
                c.set(x, y, col);
            }
        for (int lx : new int[]{70, 230}) {                                 // faroles
            c.rect(lx, 34, 2, 104, 0x262b2f);
            for (int i = 0; i < 12; i++) c.wrap(lx + i, 34, 0x262b2f);
            c.rect(lx + 9, 33, 9, 3, 0x3a4046);
            c.rect(lx + 10, 36, 7, 1, 0xeafff2);
            for (int y = 20; y < 70; y++)                                   // halo
                for (int x = lx - 12; x < lx + 40; x++) {
                    double d = Math.hypot(x - (lx + 13), (y - 38) * 1.2);
                    if (d < 22 && !(x >= lx && x < lx + 2)) c.blend(x, y, 0x9effc6, 0.35 * Math.exp(-d / 7));
                }
            for (int y = 149; y < LH; y++)                                  // reflejo en el asfalto mojado
                for (int x = lx + 4; x < lx + 24; x++) {
                    double t = Math.exp(-Math.abs(x - (lx + 13)) / 4.0) * (1 - (y - 149) / 40.0);
                    if (dither(x, y, t) && y % 2 == 0) c.blend(x, y, 0x00ff66, 0.35);
                }
        }
        return c.image();
    }
}
