package app.secret.ui;

/**
 * Los dos músicos del escenario, en pixel art (40x70), inspirados en la fotografía de referencia: trajes negros
 * de lentejuelas que centellean, guantes metálicos y cascos de cúpula brillante con visor negro.
 * THOMAS: casco plata cromado, toca un keytar. GUY-MAN: casco negro con filos dorados, guantes dorados, toca la consola.
 */
public final class Avatars {

    private Avatars() {}

    public static final int W = 40, H = 70;
    // 1 contorno, 2-5 plata (osc->claro), 6-9 negro/oro, 10 traje, 11 solapa, 12-14 lentejuela, 15 visor, 16 reflejo,
    // 17 guante negro, 18 guante dorado, 19-22 teclas/LED, 23 tinta mesa
    public static final int[] PALETTE = {0x000000, 0x050507,
            0x6e7482, 0xaab0bc, 0xdde0e8, 0xffffff,
            0x14151b, 0x3a3d4a, 0xe6b730, 0xfff0a8,
            0x0b0b10, 0x1c1c26, 0xcfd3dc, 0x8f95a3, 0xffe9a0,
            0x07080f, 0xa9c8ff, 0x22232b, 0xe6b730,
            0xf4f4f4, 0x2b2b35, 0x00f5ff, 0xff2bd6, 0x15151c};

    /** @param tick avanza los destellos de lentejuelas y el cabeceo. */
    public static PixelGrid generate(boolean thomas, int tick) {
        PixelGrid p = new PixelGrid(W, H);
        int bob = (tick / 3) % 2;                              // cabeceo al ritmo
        int hy = bob;

        // ---- piernas y zapatos
        p.rect(12, 50, 6, 17, 10); p.rect(22, 50, 6, 17, 10);
        p.rect(11, 66, 8, 3, 17); p.rect(21, 66, 8, 3, 17);
        p.set(12, 66, 16); p.set(22, 66, 16);

        // ---- torso: chaqueta negra de lentejuelas con solapas
        p.poly(new double[]{5, 35, 33, 7}, new double[]{24, 24, 52, 52}, 10);
        p.poly(new double[]{15, 20, 20}, new double[]{24, 24, 38}, 11);
        p.poly(new double[]{25, 20, 20}, new double[]{24, 24, 38}, 11);
        p.rect(18, 22, 4, 3, 10);                               // cuello
        java.util.Random r = new java.util.Random(thomas ? 3 : 9);
        for (int i = 0; i < 140; i++) {                         // lentejuelas que centellean
            int x = 6 + r.nextInt(28), y = 25 + r.nextInt(26);
            if (p.get(x, y) != 10) continue;
            int phase = (tick + i) % 7;
            p.set(x, y, phase == 0 ? 14 : (phase < 3 ? 12 : 13));
        }
        p.rect(19, 28, 2, 8, 6);                                // camisa/corbata oscura

        // ---- brazos (mangas con lentejuelas) y guantes
        int glove = thomas ? 17 : 18;
        if (thomas) {                                           // keytar en diagonal
            p.line(4, 47, 36, 38, 21, 1);
            p.rect(8, 41, 22, 3, 19);
            for (int x = 9; x < 30; x += 2) p.set(x, 41, 20);
            p.line(2, 49, 8, 46, 20, 1);
            p.line(10, 29, 22, 41, 10, 2);  p.line(30, 29, 29, 40, 10, 2);
            p.rect(21, 40, 3, 3, glove);    p.rect(28, 38, 3, 3, glove);
        } else {                                                // consola/teclado al frente
            p.rect(3, 44, 34, 6, 23); p.rect(3, 44, 34, 1, 5);
            for (int x = 5; x < 36; x += 3) p.set(x, 46 + (x / 3 + tick / 2) % 3, 21 + (x / 3) % 2);
            p.line(8, 30, 14, 44, 10, 2);   p.line(32, 30, 26, 44, 10, 2);
            p.rect(12, 42, 4, 3, glove);    p.rect(24, 42, 4, 3, glove);
        }

        // ---- casco
        helmet(p, thomas, hy);
        p.outline(1);
        return p;
    }

    private static void helmet(PixelGrid p, boolean thomas, int hy) {
        int cx = 20;
        if (thomas) {
            p.ellipse(cx, 11 + hy, 11.5, 10.5, 3);                       // cúpula plata
            p.ellipse(cx - 3, 6 + hy, 6, 3, 5);                          // reflejo especular enorme
            p.poly(new double[]{9, 31, 30, 10}, new double[]{12 + hy, 12 + hy, 19 + hy, 19 + hy}, 15);   // visor negro
            p.line(11, 13 + hy, 22, 13 + hy, 16, 0);                     // reflejo en el visor
            p.rect(11, 20 + hy, 18, 4, 2);                               // mentón plata
            p.rect(14, 20 + hy, 12, 2, 15);
            p.ellipse(33, 15 + hy, 3, 3.5, 4); p.ellipse(33, 15 + hy, 1.5, 1.5, 2);   // pieza lateral
        } else {
            p.ellipse(cx, 11 + hy, 11.5, 10.5, 6);                       // cúpula negra brillante
            p.ellipse(cx + 2, 6 + hy, 6, 2.5, 7);                        // reflejo
            for (int a = 0; a < 90; a++) {                               // filo dorado del contorno
                double ang = Math.PI + a * Math.PI / 90.0;
                p.set((int) Math.round(cx + Math.cos(ang) * 11.5), (int) Math.round(11 + hy + Math.sin(ang) * 10.5), 8);
            }
            p.poly(new double[]{9, 31, 30, 10}, new double[]{12 + hy, 12 + hy, 19 + hy, 19 + hy}, 15);
            p.line(24, 13 + hy, 29, 13 + hy, 16, 0);
            p.rect(11, 20 + hy, 18, 4, 8);                               // mentón dorado
            p.rect(14, 21 + hy, 12, 1, 9);
            p.ellipse(7, 15 + hy, 3, 3.5, 8); p.ellipse(7, 15 + hy, 1.5, 1.5, 9);
        }
    }
}
