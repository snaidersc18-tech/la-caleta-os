package app.ui;

/**
 * Sprite pixel-art de la "Araña Zombi", generado proceduralmente (sin assets).
 * Grid 60x56. Valores: 0 vacío, 1 hueso, 2 tinta, 3 gris (telaraña/grietas), 4 blanco puro (ojos).
 * Sin dependencias de JavaFX para poder probarlo/renderizarlo en cualquier lado.
 */
public final class SpiderSprite {

    public static final int W = 60, H = 56;
    public static final int BONE = 1, INK = 2, GRAY = 3, WHITE = 4;

    /** Centros de las órbitas (para superponer corazones en modo amor). */
    public static final int[] EYE_L = {23, 21};
    public static final int[] EYE_R = {38, 21};

    private SpiderSprite() {}

    /** Hombros del par de patas delanteras (izquierdo; el derecho es su espejo {@code W − 1 − x}). */
    public static final int[] FRONT_SHOULDER = {14, 19};

    /** phase 0/1 mueve ligeramente las patas (animación idle). */
    public static int[][] generate(int phase) { return generate(phase, true); }

    /**
     * @param frontLegs false = sin el par delantero, para que otra escena lo dibuje como "brazos" (p. ej. sosteniendo
     *                  algo) con {@link #drawLimb}.
     */
    public static int[][] generate(int phase, boolean frontLegs) {
        int[][] g = new int[H][W];

        // ---- patas (4 por lado): contorno tinta grueso + núcleo hueso
        int[][][] all = {
                {{14, 19}, {6, 15}, {2, 26}},
                {{13, 26}, {4, 30}, {2, 42}},
                {{15, 31}, {8, 40}, {8, 51}},
                {{20, 35}, {15, 45}, {17, 54}}
        };
        int[][][] legs = frontLegs ? all : java.util.Arrays.copyOfRange(all, 1, all.length);
        for (int[][] leg : legs) {
            int[][] a = wiggle(leg, phase, false);
            int[][] b = wiggle(leg, phase, true);
            drawPolyline(g, a, INK, 2);
            drawPolyline(g, b, INK, 2);
        }
        for (int[][] leg : legs) {
            drawPolyline(g, wiggle(leg, phase, false), BONE, 1);
            drawPolyline(g, wiggle(leg, phase, true), BONE, 1);
        }

        // ---- cráneo
        fillEllipse(g, 30, 21, 19, 17, INK);
        fillEllipse(g, 30, 21, 18, 16, BONE);

        // ---- mandíbula redondeada
        fillEllipse(g, 30, 34, 13, 10, INK);
        fillEllipse(g, 30, 34, 12, 9, BONE);
        fillRect(g, 22, 33, 16, 7, INK);           // boca oscura
        for (int x = 23; x < 37; x += 3) {         // dientes superiores
            int hh = 2 + ((x / 3) % 2);
            fillRect(g, x, 33, 2, hh, BONE);
        }
        for (int x = 24; x < 36; x += 3) {         // dientes inferiores irregulares
            int hh = 1 + ((x / 3 + 1) % 3 == 0 ? 2 : 1);
            fillRect(g, x, 40 - hh, 2, hh, BONE);
        }

        // ---- ojo izquierdo: globo grande con pupila
        fillEllipse(g, EYE_L[0], EYE_L[1], 7, 7, INK);
        fillEllipse(g, EYE_L[0], EYE_L[1], 5, 5, WHITE);
        fillEllipse(g, EYE_L[0] + 1, EYE_L[1] + 1, 2, 2, INK);
        set(g, EYE_L[0], EYE_L[1], WHITE);

        // ---- ojo derecho: entrecerrado/guiño
        fillEllipse(g, EYE_R[0], EYE_R[1], 8, 7, INK);
        fillEllipse(g, EYE_R[0], EYE_R[1], 6, 5, WHITE);
        for (int y = EYE_R[1] - 6; y <= EYE_R[1] - 1; y++)
            for (int x = EYE_R[0] - 8; x <= EYE_R[0] + 8; x++)
                if (inEllipse(x, y, EYE_R[0], EYE_R[1], 7, 6)) set(g, x, y, INK);
        fillEllipse(g, EYE_R[0] - 1, EYE_R[1] + 2, 2, 2, INK);

        // ---- nariz
        fillRect(g, 28, 27, 5, 1, INK);
        fillRect(g, 29, 28, 3, 1, INK);
        fillRect(g, 30, 29, 1, 1, INK);

        // ---- grieta y telaraña (solo sobre hueso)
        drawWeb(g);
        drawLineIfBone(g, 38, 5, 35, 10);
        drawLineIfBone(g, 35, 10, 39, 14);
        return g;
    }

    // ------------------------------------------------------------------ helpers

    /** Una pata con el mismo trazo que las del sprite (contorno tinta de radio 2 + núcleo hueso), en cualquier rejilla. */
    public static void drawLimb(int[][] g, int[][] pts) {
        drawPolyline(g, pts, INK, 2);
        drawPolyline(g, pts, BONE, 1);
    }

    /** Elipse rellena con el criterio del sprite (para retocar la cara en otra escena). */
    public static void paintEllipse(int[][] g, int cx, int cy, int rx, int ry, int v) { fillEllipse(g, cx, cy, rx, ry, v); }

    private static int[][] wiggle(int[][] leg, int phase, boolean mirror) {
        int[][] out = new int[leg.length][2];
        for (int i = 0; i < leg.length; i++) {
            int dy = (i == leg.length - 1) ? phase : (i == 1 ? phase : 0);
            int x = mirror ? W - 1 - leg[i][0] : leg[i][0];
            int y = leg[i][1] + (mirror ? (1 - phase) * (i >= 1 ? 1 : 0) : dy);
            out[i][0] = x;
            out[i][1] = Math.min(H - 1, y);
        }
        return out;
    }

    private static void drawWeb(int[][] g) {
        double cx = 30, cy = 5;
        double[] ang = {Math.toRadians(25), Math.toRadians(55), Math.toRadians(90),
                Math.toRadians(125), Math.toRadians(155)};
        double[] radii = {5, 10, 15};
        for (double a : ang)
            drawLineIfBone(g, (int) cx, (int) cy, (int) Math.round(cx + Math.cos(a) * 16), (int) Math.round(cy + Math.sin(a) * 16));
        for (double r : radii)
            for (int i = 0; i < ang.length - 1; i++) {
                int x0 = (int) Math.round(cx + Math.cos(ang[i]) * r), y0 = (int) Math.round(cy + Math.sin(ang[i]) * r);
                int x1 = (int) Math.round(cx + Math.cos(ang[i + 1]) * r), y1 = (int) Math.round(cy + Math.sin(ang[i + 1]) * r);
                drawLineIfBone(g, x0, y0, x1, y1);
            }
    }

    private static void drawLineIfBone(int[][] g, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            if (in(g, x0, y0) && g[y0][x0] == BONE) g[y0][x0] = GRAY;
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private static void drawPolyline(int[][] g, int[][] pts, int v, int radius) {
        for (int i = 0; i < pts.length - 1; i++) brushLine(g, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], v, radius);
    }

    private static void brushLine(int[][] g, int x0, int y0, int x1, int y1, int v, int r) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            for (int yy = -r; yy <= r; yy++)
                for (int xx = -r; xx <= r; xx++) set(g, x0 + xx, y0 + yy, v);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private static boolean inEllipse(int x, int y, int cx, int cy, double rx, double ry) {
        double nx = (x - cx) / rx, ny = (y - cy) / ry;
        return nx * nx + ny * ny <= 1.0;
    }

    private static void fillEllipse(int[][] g, int cx, int cy, int rx, int ry, int v) {
        for (int y = cy - ry; y <= cy + ry; y++)
            for (int x = cx - rx; x <= cx + rx; x++)
                if (inEllipse(x, y, cx, cy, rx + 0.35, ry + 0.35)) set(g, x, y, v);
    }

    private static void fillRect(int[][] g, int x, int y, int w, int h, int v) {
        for (int yy = y; yy < y + h; yy++) for (int xx = x; xx < x + w; xx++) set(g, xx, yy, v);
    }

    private static boolean in(int[][] g, int x, int y) { return y >= 0 && y < g.length && x >= 0 && x < g[y].length; }

    private static void set(int[][] g, int x, int y, int v) { if (in(g, x, y)) g[y][x] = v; }
}
