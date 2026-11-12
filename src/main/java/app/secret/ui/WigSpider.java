package app.secret.ui;

import app.ui.SpiderSprite;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * El Guachimán "BICHOTA" (BBYWOW): el sprite pixel art de siempre ({@link SpiderSprite}) con la peluca azul de la
 * referencia: pelo turquesa liso recogido en DOS COLETAS altas con ganchitos de estrella blancos, mechones sueltos
 * enmarcando la cara y puntas onduladas; ojitos cerrados de felicidad ("^ ^"), sonrisa grande con dientes, cachetes
 * rosados y una gargantilla blanca con un corazoncito. La boca puede abrirse para cantar.
 * <p>
 * Rejilla más ancha que el sprite (las coletas sobresalen a los lados): el cuerpo va corrido {@link #OX} celdas.
 * Para muchas copias (las mini arañas) usar {@link #image}: una textura que se dibuja una vez.
 */
public final class WigSpider extends Canvas {

    public static final int OX = 8, GW = SpiderSprite.W + 2 * OX, GH = SpiderSprite.H + 4;
    private static final int HAIR = 5, HAIR_HI = 6, HAIR_DK = 7, STAR = 8, LIPS = 9, TONGUE = 10, BLUSH = 11, CHOKER = 12;
    private static final Color[] PALETTE = {
            Color.TRANSPARENT, Color.web("#f4ead6"), Color.web("#1a1220"), Color.web("#9c8b9a"), Color.WHITE,
            Color.web("#1fb8c8"), Color.web("#86eaf2"), Color.web("#0d8494"), Color.web("#fffdf2"),
            Color.web("#ff7aa2"), Color.web("#ff4f7b"), Color.web("#ff9fb8"), Color.web("#f7f7fb")};

    private final double px;
    private int phase = -1;
    private boolean mouthOpen;

    public WigSpider(double pixelSize) {
        super(GW * pixelSize, GH * pixelSize);
        this.px = pixelSize;
        setMouseTransparent(true);
        pose(0, false);
    }

    /** Patas en la fase {@code phase} (0/1) y boca abierta (cantando) o sonrisa. Solo redibuja si algo cambió. */
    public void pose(int phase, boolean mouthOpen) {
        if (phase == this.phase && mouthOpen == this.mouthOpen) return;
        this.phase = phase;
        this.mouthOpen = mouthOpen;
        paint(getGraphicsContext2D(), grid(phase, mouthOpen), px);
    }

    /** Textura con fondo transparente (para copias baratas en ImageView). Solo hilo FX. */
    public static Image image(double pixelSize, int phase, boolean mouthOpen) {
        Canvas c = new Canvas(GW * pixelSize, GH * pixelSize);
        paint(c.getGraphicsContext2D(), grid(phase, mouthOpen), pixelSize);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }

    private static void paint(GraphicsContext g, int[][] grid, double px) {
        g.clearRect(0, 0, GW * px, GH * px);
        for (int y = 0; y < GH; y++) {
            int x = 0;
            while (x < GW) {
                int v = grid[y][x];
                if (v == 0) { x++; continue; }
                int run = 1;
                while (x + run < GW && grid[y][x + run] == v) run++;
                g.setFill(PALETTE[v]);
                g.fillRect(x * px, y * px, run * px, px);
                x += run;
            }
        }
    }

    // ------------------------------------------------------------------ la rejilla

    static int[][] grid(int phase, boolean mouthOpen) {
        int[][] g = new int[GH][GW];
        int[][] body = SpiderSprite.generate(phase);
        for (int y = 0; y < SpiderSprite.H; y++)
            for (int x = 0; x < SpiderSprite.W; x++) if (body[y][x] != 0) g[y + 2][x + OX] = body[y][x];

        happyEyes(g);
        if (mouthOpen) sing(g); else grin(g);
        blush(g);
        choker(g);

        boolean[][] hair = hairMask();
        for (int y = 0; y < GH; y++)                                     // contorno de tinta alrededor del pelo
            for (int x = 0; x < GW; x++)
                if (!hair[y][x] && g[y][x] == 0 && touches(hair, x, y)) g[y][x] = SpiderSprite.INK;
        for (int y = 0; y < GH; y++)
            for (int x = 0; x < GW; x++) {
                if (!hair[y][x]) continue;
                // mechones lisos y verticales: brillo y sombra en franjas, con las puntas en sombra
                int stripe = Math.floorMod(x * 2 + (y > 40 ? (int) Math.round(1.5 * Math.sin(y / 2.0)) : 0), 7);
                int v = stripe == 0 ? HAIR_HI : stripe == 4 ? HAIR_DK : HAIR;
                if (y + 1 < GH && !hair[y + 1][x] && y > 30) v = HAIR_DK;
                g[y][x] = v;
            }
        star(g, cx(16), 6);                                              // ganchitos de estrella en las coletas
        star(g, cx(44), 6);
        return g;
    }

    /** x del sprite → x de la rejilla ancha. */
    private static int cx(int spriteX) { return spriteX + OX; }

    /**
     * Pelo: casco liso sobre el cráneo (la frente queda despejada), dos coletas ALTAS que salen de los costados de la
     * coronilla, caen largas por fuera del cuerpo y terminan en puntas onduladas, y un mechón suelto a cada lado de
     * la cara.
     */
    private static boolean[][] hairMask() {
        boolean[][] m = new boolean[GH][GW];
        int top = 2;                                                     // el cuerpo va 2 filas más abajo
        for (int y = 0; y < GH; y++)
            for (int x = 0; x < GW; x++) {
                int sx = x - OX, sy = y - top;                           // coordenadas del sprite
                boolean cap = in(sx, sy, 30, 19, 20, 16) && sy <= 11 && !in(sx, sy, 30, 24, 15, 13.5);
                boolean strandL = sy >= 10 && sy <= 30 && sx >= 11 + sy / 12 && sx <= 13 + sy / 12;
                boolean strandR = sy >= 10 && sy <= 30 && sx >= 47 - sy / 12 && sx <= 49 - sy / 12;
                m[y][x] = cap || strandL || strandR;
            }
        tail(m, cx(16), 7, -1);
        tail(m, cx(44), 7, 1);
        return m;
    }

    /** Una coleta: nace en (x0, y0), sale hacia afuera y cae; se ensancha a mitad y ondula en las puntas. */
    private static void tail(boolean[][] m, int x0, int y0, int dir) {
        for (double t = 0; t <= 1; t += 0.01) {
            double x = x0 + dir * (14 * Math.sin(Math.min(1, t * 2.2) * Math.PI / 2) - 3 * t), y = y0 - 2 + 50 * t * t + 6 * t;
            if (t > 0.6) x += dir * 1.6 * Math.sin((t - 0.6) * 22);      // puntas onduladas (vista de espalda)
            double r = 2 + 3.2 * Math.sin(Math.PI * Math.min(1, t * 1.15));
            for (int yy = (int) (y - r); yy <= y + r; yy++)
                for (int xx = (int) (x - r); xx <= x + r; xx++)
                    if (xx >= 0 && yy >= 0 && xx < GW && yy < GH && (xx - x) * (xx - x) + (yy - y) * (yy - y) <= r * r) m[yy][xx] = true;
        }
    }

    /** Estrella blanca de 5 puntas (7x7) con contorno. */
    private static void star(int[][] g, int x, int y) {
        String[] s = {"...#...", "...#...", "#######", ".#####.", "..###..", ".##.##.", "##...##"};
        for (int j = 0; j < s.length; j++)
            for (int i = 0; i < 7; i++) {
                int xx = x - 3 + i, yy = y - 3 + j;
                if (xx < 0 || yy < 0 || xx >= GW || yy >= GH) continue;
                if (s[j].charAt(i) == '#') g[yy][xx] = STAR;
            }
        for (int j = -1; j <= 7; j++)                                    // contorno de tinta alrededor de la estrella
            for (int i = -1; i <= 7; i++) {
                int xx = x - 3 + i, yy = y - 3 + j;
                if (xx < 0 || yy < 0 || xx >= GW || yy >= GH || g[yy][xx] == STAR) continue;
                boolean near = false;
                for (int dy = -1; dy <= 1 && !near; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int ax = xx + dx, ay = yy + dy;
                        if (ax >= 0 && ay >= 0 && ax < GW && ay < GH && g[ay][ax] == STAR && Math.abs(dx) + Math.abs(dy) == 1) { near = true; break; }
                    }
                if (near) g[yy][xx] = SpiderSprite.INK;
            }
    }

    /** Ojos cerrados de felicidad "^ ^" (como en la foto). */
    private static void happyEyes(int[][] g) {
        for (int[] e : new int[][]{SpiderSprite.EYE_L, SpiderSprite.EYE_R}) {
            int ex = e[0] + OX, ey = e[1] + 2;
            SpiderSprite.paintEllipse(g, ex, ey, 8, 7, SpiderSprite.BONE);
            for (int x = -5; x <= 5; x++) {
                int y = ey + 1 - (int) Math.round(2.6 * (1 - (x / 5.0) * (x / 5.0)));
                set(g, ex + x, y, SpiderSprite.INK);
                set(g, ex + x, y + 1, SpiderSprite.INK);
            }
            set(g, ex + 6, ey - 1, SpiderSprite.INK);                    // pestañitas
            set(g, ex + 7, ey - 2, SpiderSprite.INK);
        }
    }

    /** Sonrisa grande con dientes (borra la dentadura de calavera). */
    private static void grin(int[][] g) {
        int cx = 30 + OX, top = 33 + 2;
        SpiderSprite.paintEllipse(g, cx, 34 + 2, 12, 9, SpiderSprite.BONE);
        for (int x = -9; x <= 9; x++) {
            int bottom = top + (int) Math.round(6 * (1 - (x / 9.5) * (x / 9.5)));
            for (int y = top; y <= bottom; y++) set(g, cx + x, y, SpiderSprite.INK);
            if (Math.abs(x) <= 7) set(g, cx + x, top + 1, SpiderSprite.WHITE);    // dientes de arriba
            if (Math.abs(x) <= 3) set(g, cx + x, bottom - 1, TONGUE);
        }
        for (int x = -10; x <= 10; x++) set(g, cx + x, top - 1, LIPS);   // labio rosado
    }

    /** Boca abierta cantando: labios rosados alrededor de un óvalo oscuro con lengua. */
    private static void sing(int[][] g) {
        int cx = 30 + OX, cy = 35 + 2;
        SpiderSprite.paintEllipse(g, cx, 34 + 2, 12, 9, SpiderSprite.BONE);
        SpiderSprite.paintEllipse(g, cx, cy, 6, 5, LIPS);
        SpiderSprite.paintEllipse(g, cx, cy, 4, 3, SpiderSprite.INK);
        for (int x = -2; x <= 2; x++) set(g, cx + x, cy + 2, TONGUE);
        for (int x = -3; x <= 3; x++) set(g, cx + x, cy - 2, SpiderSprite.WHITE);
    }

    private static void blush(int[][] g) {
        SpiderSprite.paintEllipse(g, 14 + OX, 31, 2, 1, BLUSH);
        SpiderSprite.paintEllipse(g, 46 + OX, 31, 2, 1, BLUSH);
    }

    /** Gargantilla blanca bajo la mandíbula, con un corazoncito. */
    private static void choker(int[][] g) {
        int y = 44 + 2, cx = 30 + OX;
        for (int x = -8; x <= 8; x++) { set(g, cx + x, y - 1, SpiderSprite.INK); set(g, cx + x, y, CHOKER); set(g, cx + x, y + 1, SpiderSprite.INK); }
        set(g, cx - 1, y + 2, TONGUE); set(g, cx + 1, y + 2, TONGUE); set(g, cx, y + 2, TONGUE); set(g, cx, y + 3, TONGUE);
    }

    private static void set(int[][] g, int x, int y, int v) { if (x >= 0 && y >= 0 && x < GW && y < GH) g[y][x] = v; }

    private static boolean touches(boolean[][] m, int x, int y) {
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx, yy = y + dy;
                if ((dx != 0 || dy != 0) && xx >= 0 && yy >= 0 && xx < GW && yy < GH && m[yy][xx]) return true;
            }
        return false;
    }

    private static boolean in(int x, int y, double cx, double cy, double rx, double ry) {
        double nx = (x - cx) / rx, ny = (y - cy) / ry;
        return nx * nx + ny * ny <= 1;
    }
}
