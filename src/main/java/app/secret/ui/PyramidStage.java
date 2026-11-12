package app.secret.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Escenario de concierto con pirámide central iluminada, andamios laterales, pantallas triangulares
 * de LED que parpadean (azul, rojo, verde, amarillo), haces de luz y público en silueta.
 * {@link #build} genera la estructura estática; {@link #ledColor} da el color de cada celda LED por tick.
 */
public final class PyramidStage {

    public static final int SKY0 = 1, SKY1 = 2, STAR = 3, TRUSS = 4, TRUSS_HI = 5, PYR_L = 6, PYR_R = 7, PYR_EDGE = 8,
            PYR_CORE = 9, FLOOR = 10, FLOOR_HI = 11, CROWD = 12, BEAM = 13, SCREEN_FRAME = 14, GLOW = 15;

    public static final int[] PALETTE = {0x000000,
            0x070a1e, 0x1b1550, 0xdfe8ff, 0x3a3f58, 0x8f98b8,
            0xcfe9ff, 0x7fb4f0, 0xffffff, 0xf4fbff, 0x14152a, 0x6b73c9, 0x05050c, 0x35408f, 0x0d0f22, 0x2b3a8c};

    /** Colores LED: azul, rojo, verde, amarillo (y sus tonos apagados para dar textura). */
    public static final int[] LED_ON = {0x2f6bff, 0xff2f3f, 0x2fe05a, 0xffd92f};
    public static final int[] LED_DIM = {0x16328f, 0x8f1722, 0x16722d, 0x8f7a16};

    public static final class Result {
        public final PixelGrid base;
        /** Celdas LED agrupadas por pantalla: cada int[] = {x, y}. */
        public final List<List<int[]>> screens = new ArrayList<>();
        Result(PixelGrid base) { this.base = base; }
    }

    private PyramidStage() {}

    public static Result build(int w, int h) {
        PixelGrid p = new PixelGrid(w, h);
        Result res = new Result(p);
        int floorY = (int) (h * 0.80);

        // cielo nocturno con estrellas
        for (int y = 0; y < floorY; y++) {
            double t = y / (double) floorY;
            for (int x = 0; x < w; x++) p.set(x, y, PixelGrid.dither(x, y, t * t, SKY0, SKY1));
        }
        java.util.Random r = PixelGrid.rng(11);
        for (int i = 0; i < Math.max(12, w * h / 260); i++) p.set(r.nextInt(w), r.nextInt((int) (h * 0.45)), STAR);

        // haces de luz que salen de la base de la pirámide
        int cx = w / 2;
        for (int k = -3; k <= 3; k++) {
            if (k == 0) continue;
            int x1 = cx + k * (w / 9), y1 = (int) (h * 0.02);
            p.line(cx + k * (w / 40), floorY - 2, x1, y1, BEAM, 0);
        }

        // andamios (torres laterales con cruces)
        truss(p, (int) (w * 0.03), (int) (h * 0.12), (int) (w * 0.095), floorY);
        truss(p, (int) (w * 0.875), (int) (h * 0.12), (int) (w * 0.095), floorY);
        // viga superior
        p.rect((int) (w * 0.03), (int) (h * 0.11), (int) (w * 0.94), 1, TRUSS_HI);
        p.rect((int) (w * 0.03), (int) (h * 0.12), (int) (w * 0.94), 1, TRUSS);

        // pantallas triangulares de LED alrededor de la pirámide
        double[][] tris = {
                {0.17, 0.62, 0.30, 0.20, 0.30, 0.62},      // izquierda grande (apunta arriba)
                {0.83, 0.62, 0.70, 0.20, 0.70, 0.62},      // derecha grande
                {0.12, 0.17, 0.26, 0.17, 0.19, 0.38},      // arriba izq. (invertido)
                {0.88, 0.17, 0.74, 0.17, 0.81, 0.38},      // arriba der. (invertido)
                {0.36, 0.62, 0.40, 0.46, 0.44, 0.62},      // pequeños frente a la pirámide
                {0.64, 0.62, 0.60, 0.46, 0.56, 0.62}
        };
        for (double[] t : tris) {
            double[] xs = {t[0] * w, t[2] * w, t[4] * w}, ys = {t[1] * h, t[3] * h, t[5] * h};
            // marco
            for (int i = 0; i < 3; i++) {
                int j = (i + 1) % 3;
                p.line((int) xs[i], (int) ys[i], (int) xs[j], (int) ys[j], SCREEN_FRAME, 0);
            }
            List<int[]> cells = new ArrayList<>();
            int[][] before = new int[h][];
            for (int y = 0; y < h; y++) before[y] = p.g[y].clone();
            PixelGrid tmp = new PixelGrid(w, h);
            tmp.poly(shrink(xs, 0.88), shrink(ys, 0.88), 99);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (tmp.g[y][x] == 99) cells.add(new int[]{x, y});
            res.screens.add(cells);
        }

        // pirámide central: caras, núcleo iluminado, aristas, resplandor
        int apexY = (int) (h * 0.07), baseY = floorY - 1, half = (int) (w * 0.20);
        for (int k = 4; k >= 1; k--) {                                              // halo en capas
            double[] xs = {cx, cx - half - k * 3, cx + half + k * 3}, ys = {apexY - k * 2, baseY, baseY};
            p.poly(xs, ys, GLOW, k == 4 ? -1 : -1);
        }
        p.poly(new double[]{cx, cx - half, cx}, new double[]{apexY, baseY, baseY}, PYR_L);
        p.poly(new double[]{cx, cx + half, cx}, new double[]{apexY, baseY, baseY}, PYR_R);
        p.poly(new double[]{cx, cx - half * 0.42, cx + half * 0.42},
                new double[]{apexY + (baseY - apexY) * 0.30, baseY - (baseY - apexY) * 0.12, baseY - (baseY - apexY) * 0.12}, PYR_CORE);
        p.line(cx, apexY, cx - half, baseY, PYR_EDGE, 0);
        p.line(cx, apexY, cx + half, baseY, PYR_EDGE, 0);
        p.line(cx, apexY, cx, baseY, PYR_EDGE, 0);
        p.rect(cx - half, baseY, half * 2 + 1, 1, PYR_EDGE);
        p.set(cx, apexY - 1, PYR_EDGE);

        // escenario y público
        p.rect(0, floorY, w, h - floorY, FLOOR);
        p.rect(0, floorY, w, 1, FLOOR_HI);
        for (int x = 0; x < w; x += 4) p.set(x, floorY + 2, FLOOR_HI);
        int crowdTop = (int) (h * 0.90);
        for (int x = 0; x < w; x++) {
            int bump = crowdTop + (int) (Math.sin(x * 0.9) * 1.6 + Math.sin(x * 0.37) * 1.8);
            for (int y = bump; y < h; y++) p.set(x, y, CROWD);
            if (x % 9 == 3) { p.rect(x, bump - 3, 1, 3, CROWD); p.set(x, bump - 4, CROWD); }   // manos arriba
        }
        return res;
    }

    private static double[] shrink(double[] v, double k) {
        double c = (v[0] + v[1] + v[2]) / 3.0;
        return new double[]{c + (v[0] - c) * k, c + (v[1] - c) * k, c + (v[2] - c) * k};
    }

    private static void truss(PixelGrid p, int x, int top, int width, int bottom) {
        p.rect(x, top, 1, bottom - top, TRUSS_HI);
        p.rect(x + width, top, 1, bottom - top, TRUSS_HI);
        int step = Math.max(4, width);
        for (int y = top; y + step <= bottom; y += step) {
            p.line(x, y, x + width, y + step, TRUSS, 0);
            p.line(x + width, y, x, y + step, TRUSS, 0);
            p.rect(x, y, width + 1, 1, TRUSS_HI);
        }
    }

    /** Color LED de la celda (x,y) de la pantalla s en el tick t: parpadeo por pantalla + textura de matriz. */
    public static int ledColor(int s, int x, int y, int t) {
        int i = ledIndex(s, x, y, t);
        return (i & 1) == 0 ? LED_ON[i >> 1] : LED_DIM[i >> 1];
    }

    /** Índice 0..7: (color << 1) | (apagado ? 1 : 0). */
    public static int ledIndex(int s, int x, int y, int t) {
        int c = ((t / 6) + s * 3 + (s % 2 == 0 ? 0 : ((x + y) / 9))) % 4;
        boolean lit = ((x * 7 + y * 13 + t * 3) % 6) != 0;
        return (c << 1) | (lit ? 0 : 1);
    }
}
