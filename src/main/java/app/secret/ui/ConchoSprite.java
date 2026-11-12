package app.secret.ui;

/**
 * Concho en pixel art, fiel a la referencia: rana dorada de ojos negros enormes y brillantes, camisa celeste con
 * correas de mochila, pantalón rojo con franja blanca, zapatillas azules y una cámara negra sostenida con ambas manos.
 * Poses: 0 reposo, 1-2 caminar, 3 apuntar la cámara al usuario, 4-6 baile de celebración.
 */
public final class ConchoSprite {

    private ConchoSprite() {}

    public static final int OUT = 1, GOLD = 2, GOLD_L = 3, GOLD_D = 4, EYE = 5, EYE_HI = 6, EYE_W = 7, SHIRT = 8,
            SHIRT_D = 9, BUTTON = 10, STRAP = 11, STRAP_D = 12, PANTS = 13, PANTS_D = 14, STRIPE = 15, SHOE = 16,
            SOLE = 17, CAM = 18, LENS = 19, GLARE = 20, FLASH = 21;
    public static final int[] PALETTE = {0x000000, 0x2e1608, 0xf0b126, 0xffd75a, 0xc98a14, 0x10131c, 0x6fa4ff, 0xffffff,
            0x4aa8c8, 0x347f9c, 0xd8f1fb, 0xc8905a, 0x8f5f32, 0xd9242b, 0x9a1a20, 0xf6f6f6, 0x2a58d6, 0xf4f4f4,
            0x1b1b20, 0x5a6577, 0xcfe3ff, 0x666672};
    public static final int W = 32, H = 52, POSES = 7;

    public static PixelGrid generate(int pose, boolean flash) {
        PixelGrid p = new PixelGrid(W, H);
        final int oy = 6;                                   // margen superior (brazos en alto)
        int bob = (pose == 1 || pose == 2) ? -1 : (pose == 5 ? 1 : 0);
        int dy = oy + bob;

        // ---- piernas (pantalón rojo con franja blanca) y zapatillas azules
        int liftL = (pose == 1 || pose == 6) ? 3 : 0, liftR = (pose == 2 || pose == 4) ? 3 : 0;
        int squat = pose == 5 ? 2 : 0;
        leg(p, 8, 35 + dy + squat, 6, 7 - liftL - squat, liftL, true);
        leg(p, 18, 35 + dy + squat, 6, 7 - liftR - squat, liftR, false);
        p.rect(7, 31 + dy + squat, 18, 5, PANTS);
        p.rect(7, 35 + dy + squat, 18, 1, PANTS_D);

        // ---- torso: camisa celeste, botones, cuello
        p.poly(new double[]{7, 25, 26.5, 5.5}, new double[]{16 + dy, 16 + dy, 33 + dy + squat, 33 + dy + squat}, SHIRT);
        p.rect(23, 17 + dy, 3, 15, SHIRT_D);
        p.line(16, 17 + dy, 16, 32 + dy + squat, SHIRT_D, 0);
        for (int y = 20; y <= 30; y += 4) p.set(16, y + dy, BUTTON);
        p.poly(new double[]{9, 16, 16}, new double[]{15 + dy, 15 + dy, 20 + dy}, SHIRT_D);
        p.poly(new double[]{23, 16, 16}, new double[]{15 + dy, 15 + dy, 20 + dy}, SHIRT_D);
        // correas de mochila
        p.rect(9, 16 + dy, 3, 16 + squat, STRAP);  p.rect(20, 16 + dy, 3, 16 + squat, STRAP);
        p.rect(9, 16 + dy, 1, 16 + squat, STRAP_D); p.rect(22, 16 + dy, 1, 16 + squat, STRAP_D);

        // ---- brazos y cámara según la pose
        int camX = 16, camY = 26 + dy, hlx = 11, hly = 27 + dy, hrx = 21, hry = 27 + dy;
        int slx = 6, sly = 19 + dy, srx = 26, sry = 19 + dy;
        if (pose == 3) { camY = 13 + dy; hlx = 10; hly = 14 + dy; hrx = 22; hry = 14 + dy; }
        if (pose == 4 || pose == 6) { camX = 27; camY = 6 + dy; hlx = 5; hly = 8 + dy; hrx = 27; hry = 8 + dy; }
        if (pose == 5) { camY = 24 + dy; hlx = 11; hly = 25 + dy; hrx = 21; hry = 25 + dy; }
        arm(p, slx, sly, hlx, hly, -1);
        arm(p, srx, sry, hrx, hry, 1);
        if (pose != 3) { camera(p, camX, camY, flash); hand(p, hlx, hly); hand(p, hrx, hry); }

        // ---- cabeza dorada de rana con ojos saltones
        p.ellipse(16, 10 + dy, 10.5, 6.2, GOLD);
        p.ellipse(16, 12 + dy, 8, 3.2, GOLD_L);
        p.ellipse(9, 4.5 + dy, 4.6, 4.4, GOLD);
        p.ellipse(23, 4.5 + dy, 4.6, 4.4, GOLD);
        p.ellipse(9, 5 + dy, 3.6, 3.6, EYE);
        p.ellipse(23, 5 + dy, 3.6, 3.6, EYE);
        for (int i = 0; i < 2; i++) {
            int ex = i == 0 ? 9 : 23;
            p.set(ex - 1, 3 + dy, EYE_HI); p.set(ex, 3 + dy, EYE_HI); p.set(ex - 1, 4 + dy, EYE_HI);
            p.set(ex - 2, 2 + dy, EYE_W);
            p.set(ex + 1, 7 + dy, EYE_HI);
        }
        p.rect(14, 12 + dy, 5, 1, GOLD_D);                       // sutil línea de boca
        p.set(14, 9 + dy, GOLD_D); p.set(18, 9 + dy, GOLD_D);    // fosas nasales
        p.rect(7, 14 + dy, 18, 1, GOLD_D);
        if (pose == 3) { camera(p, camX, camY, flash); hand(p, hlx, hly); hand(p, hrx, hry); }   // apunta al usuario: cámara al frente
        p.outline(OUT);
        return p;
    }

    private static void leg(PixelGrid p, int x, int y, int w, int h, int lift, boolean left) {
        p.rect(x, y, w, Math.max(1, h), PANTS);
        p.rect(left ? x : x + w - 1, y, 1, Math.max(1, h), STRIPE);          // franja blanca exterior
        p.rect(left ? x + w - 1 : x, y, 1, Math.max(1, h), PANTS_D);
        int sy = y + Math.max(1, h);
        p.rect(x - 1, sy, w + 2, 3, SHOE);
        p.rect(x - 1, sy + 3, w + 2, 1, SOLE);
        p.set(x + 1, sy, SOLE);
    }

    private static void arm(PixelGrid p, int sx, int sy, int hx, int hy, int dir) {
        int ex = (sx + hx) / 2 + dir * 2, ey = (sy + hy) / 2;
        p.line(sx, sy, ex, ey, SHIRT, 1);                                   // manga
        p.line(ex, ey, hx, hy, GOLD, 1);                                    // antebrazo dorado
        p.ellipse(sx, sy, 2.6, 2.6, SHIRT);
    }

    private static void hand(PixelGrid p, int x, int y) {
        p.ellipse(x, y, 2.2, 2.0, GOLD_L);
        p.set(x, y + 2, GOLD_D);
    }

    private static void camera(PixelGrid p, int cx, int cy, boolean flash) {
        p.rect(cx - 5, cy - 3, 11, 7, CAM);
        p.rect(cx - 5, cy - 3, 11, 1, 0x00 + LENS);                          // borde superior metálico
        p.rect(cx - 3, cy - 5, 3, 2, CAM);                                  // visor
        p.ellipse(cx + 0.5, cy + 0.5, 2.6, 2.6, LENS);
        p.ellipse(cx + 0.5, cy + 0.5, 1.5, 1.5, EYE);
        p.set(cx - 0, cy - 1, GLARE);
        p.rect(cx + 3, cy - 5, 3, 2, flash ? FLASH : 0x00 + SOLE);          // bombillo del flash
        if (flash) { p.set(cx + 4, cy - 6, FLASH); p.set(cx + 2, cy - 5, FLASH); p.set(cx + 6, cy - 5, FLASH); }
    }
}
