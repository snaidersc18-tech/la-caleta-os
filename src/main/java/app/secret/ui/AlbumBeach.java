package app.secret.ui;

/**
 * Playa del Corazón Triste en pixel art, fiel a la referencia: cielo rosa con ondas más claras, sol amarillo
 * con rayos rojos en zigzag y pajaritos, mar celeste/lavanda, dos delfines lavanda saltando, palmeras verdes de
 * frondas deshilachadas, dunas naranja con manchas rojas/amarillas y flores rosadas. Se adapta a cualquier tamaño.
 */
public final class AlbumBeach {

    private AlbumBeach() {}

    public static final int SKY_A = 1, SKY_B = 2, RAY = 3, SUN_O = 4, SUN_Y = 5, SEA = 6, SEA_LAV = 7, SEA_DEEP = 8,
            DUNE_O = 9, DUNE_R = 10, DUNE_Y = 11, PALM = 12, PALM_L = 13, PALM_D = 14, TRUNK = 15, TRUNK_D = 16,
            DOLPHIN = 17, DOLPHIN_D = 18, DOLPHIN_L = 19, BIRD = 20, FLOWER = 21, FLOWER_C = 22, STEM = 23, FOAM = 24;

    public static final int[] PALETTE = {0x000000,
            0xe8627f, 0xf8b4b2, 0xe8200f, 0xffa81f, 0xffe45c, 0xa9cdf6, 0xc5c5f5, 0x7fb3ee,
            0xff8a1f, 0xf0521e, 0xffc21f, 0x3fa845, 0x8be05a, 0x1f7a34, 0x8a4f2a, 0x5a2f18,
            0xa99be3, 0x4b3a8a, 0xd9d2f7, 0x3a2a2a, 0xe86aa6, 0xffb347, 0x2f7a3a, 0xffffff};

    public static PixelGrid background(int w, int h) {
        PixelGrid p = new PixelGrid(w, h);
        int horizon = (int) (h * 0.47);

        // cielo rosa con ondas diagonales más claras
        for (int y = 0; y < horizon; y++)
            for (int x = 0; x < w; x++) {
                double wave = Math.sin(x * 0.17 + y * 0.42 + Math.sin(y * 0.2) * 2.0);
                p.set(x, y, wave > 0.35 ? SKY_B : SKY_A);
            }

        // sol: rayos rojos en zigzag + disco naranja/amarillo
        int cx = (int) (w * 0.69), cy = horizon;
        int r = (int) (Math.min(w, h) * 0.22);
        int spikes = 26;
        for (int i = 0; i < spikes; i++) {
            double a0 = Math.PI + i * Math.PI / spikes, a1 = a0 + Math.PI / spikes, am = (a0 + a1) / 2;
            p.poly(new double[]{cx + Math.cos(a0) * (r - 2), cx + Math.cos(am) * (r * 1.42), cx + Math.cos(a1) * (r - 2)},
                    new double[]{cy + Math.sin(a0) * (r - 2), cy + Math.sin(am) * (r * 1.42), cy + Math.sin(a1) * (r - 2)}, RAY);
        }
        p.ellipse(cx, cy, r, r, SUN_O);
        p.ellipse(cx, cy + r * 0.12, r * 0.74, r * 0.74, SUN_Y);
        for (int[] b : new int[][]{{-r / 3, -r / 2}, {r / 8, -r / 3}, {r / 5, -r * 2 / 3}}) {        // pajaritos
            p.line(cx + b[0], cy + b[1], cx + b[0] + 2, cy + b[1] + 1, BIRD, 0);
            p.line(cx + b[0] + 2, cy + b[1] + 1, cx + b[0] + 4, cy + b[1], BIRD, 0);
        }

        // mar celeste con parches lavanda y azul profundo
        int seaEnd = (int) (h * 0.92);
        for (int y = horizon; y < seaEnd; y++)
            for (int x = 0; x < w; x++) {
                int v = SEA;
                double n = Math.sin(x * 0.09 + y * 0.31) + Math.sin(x * 0.045 - y * 0.12);
                if (n > 0.9) v = SEA_LAV;
                if (x < w * 0.16 && y > horizon + (seaEnd - horizon) * 0.25 && n < -0.3) v = SEA_DEEP;
                p.set(x, y, v);
            }
        p.rect(0, horizon, w, 1, SEA_DEEP);

        // dunas naranjas con manchas
        hill(p, (int) (w * 0.16), (int) (h * 0.80), (int) (w * 0.30), (int) (h * 0.24));
        hill(p, (int) (w * 0.82), (int) (h * 0.86), (int) (w * 0.34), (int) (h * 0.18));
        for (int y = (int) (h * 0.84); y < h; y++)
            for (int x = 0; x < w; x++) p.set(x, y, dune(x, y));

        // palmeras
        palm(p, (int) (w * 0.205), (int) (h * 0.66), (int) (w * 0.185), (int) (h * 0.36), (int) (w * 0.17), 1.0);
        palm(p, (int) (w * 0.91), (int) (h * 0.80), (int) (w * 0.90), (int) (h * 0.58), (int) (w * 0.12), 0.8);

        // flores
        flower(p, (int) (w * 0.04), (int) (h * 0.90), 4);
        flower(p, (int) (w * 0.955), (int) (h * 0.92), 4);
        flower(p, (int) (w * 0.12), (int) (h * 0.95), 3);
        return p;
    }

    private static int dune(int x, int y) {
        double n = Math.sin(x * 0.13 + y * 0.21) + Math.sin(x * 0.05 - y * 0.37) + Math.sin(x * 0.31 + y * 0.07);
        if (n > 1.1) return DUNE_R;
        if (n < -0.9) return DUNE_Y;
        return DUNE_O;
    }

    private static void hill(PixelGrid p, int cx, int baseY, int halfW, int height) {
        for (int x = Math.max(0, cx - halfW); x < Math.min(p.w, cx + halfW); x++) {
            double t = (x - cx) / (double) halfW;
            int top = baseY - (int) (height * (1 - t * t));
            for (int y = top; y < p.h; y++) p.set(x, y, dune(x, y));
        }
    }

    /** Palmera de tronco corto y copa enorme de frondas deshilachadas. */
    private static void palm(PixelGrid p, int bx, int by, int tx, int ty, int span, double k) {
        p.line(bx, by, (bx + tx) / 2 + 2, (by + ty) / 2, TRUNK, 1);
        p.line((bx + tx) / 2 + 2, (by + ty) / 2, tx, ty, TRUNK, 1);
        for (int y = ty; y < by; y += 3) p.set(tx + (bx - tx) * (y - ty) / Math.max(1, by - ty), y, TRUNK_D);
        java.util.Random r = new java.util.Random(7 + bx);
        int leaves = 15;
        for (int i = 0; i < leaves; i++) {
            double a = Math.toRadians(-175 + i * (170.0 / (leaves - 1)));
            double len = span * (0.55 + 0.45 * r.nextDouble()) * k;
            double px = tx, py = ty, ang = a;
            for (int s = 0; s < 16; s++) {                           // la fronda se arquea y cae
                double step = len / 16.0;
                ang += 0.075 * Math.signum(Math.cos(a) == 0 ? 1 : Math.cos(a));
                double nx = px + Math.cos(ang) * step, ny = py + Math.sin(ang) * step + s * 0.16;
                p.line((int) px, (int) py, (int) nx, (int) ny, i % 3 == 0 ? PALM_D : PALM, 0);
                if (s > 2) {                                         // flecos
                    int fx = (int) nx, fy = (int) ny, fl = 2 + r.nextInt(4);
                    p.line(fx, fy, fx - 1, fy + fl, r.nextBoolean() ? PALM_L : PALM, 0);
                    p.line(fx, fy, fx + 1, fy + fl, r.nextBoolean() ? PALM_L : PALM_D, 0);
                }
                px = nx; py = ny;
            }
        }
        p.ellipse(tx, ty + 1, 2, 2, PALM_D);
    }

    private static void flower(PixelGrid p, int x, int y, int r) {
        p.line(x, y + r, x - 1, y + r * 4, STEM, 0);
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5 - Math.PI / 2;
            p.ellipse(x + Math.cos(a) * r * 0.9, y + Math.sin(a) * r * 0.9, r * 0.6, r * 0.6, FLOWER);
        }
        p.ellipse(x, y, r * 0.5, r * 0.5, FLOWER_C);
    }

    /** Dos delfines lavanda saltando juntos (esquina superior izquierda), con contorno oscuro. */
    public static PixelGrid dolphins(int w, int h, double phase) {
        PixelGrid p = new PixelGrid(w, h);
        double bob = Math.sin(phase * Math.PI * 2);
        dolphin(p, (int) (w * 0.10), (int) (h * 0.23 + bob * 1.5), 1.0, 0.9);
        dolphin(p, (int) (w * 0.255), (int) (h * 0.17 - bob * 1.5), 0.85, -0.7);
        return p;
    }

    private static void dolphin(PixelGrid p, int cx, int cy, double scale, double tilt) {
        PixelGrid d = new PixelGrid(p.w, p.h);
        int len = (int) (p.w * 0.105 * scale);
        // cuerpo curvado (arco de salto): de la cola (abajo-izq.) al hocico (abajo-der.)
        for (int i = 0; i <= len; i++) {
            double t = i / (double) len;
            double x = cx + (t - 0.5) * len;
            double y = cy - Math.sin(t * Math.PI) * len * 0.34 + (t - 0.5) * tilt * 3;
            double thick = (0.5 + Math.sin(t * Math.PI * 0.9)) * len * 0.085 + 0.6;
            if (t < 0.12) thick *= 0.55;
            d.ellipse(x, y, thick, thick * 0.9, DOLPHIN);
            if (t > 0.45) d.ellipse(x, y + thick * 0.45, thick * 0.6, thick * 0.4, DOLPHIN_L);   // vientre claro
        }
        int hx = cx + len / 2, hy = cy + (int) (tilt * 1.5) + 1;
        d.line(hx - 2, hy - 1, hx + 3, hy, DOLPHIN, 0);                                           // hocico
        int tx = cx - len / 2, ty = cy + (int) (-tilt * 1.5) + 1;
        d.poly(new double[]{tx, tx - len * 0.12, tx + 2}, new double[]{ty + 1, ty + len * 0.14, ty + len * 0.1}, DOLPHIN); // cola
        d.poly(new double[]{cx - 1, cx + 2, cx - 3}, new double[]{cy - len * 0.30, cy - len * 0.34, cy - len * 0.14}, DOLPHIN);  // aleta
        d.outline(DOLPHIN_D);
        for (int y = 0; y < p.h; y++) for (int x = 0; x < p.w; x++) if (d.g[y][x] != 0) p.g[y][x] = d.g[y][x];
        p.set(hx - 1, hy - 3, DOLPHIN_D);                                                          // ojo
    }
}
