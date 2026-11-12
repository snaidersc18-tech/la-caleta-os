package app.stega;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Genera un camuflaje inocente (skyline nocturno con grano) del tamaño justo
 * para que quepa la carga. En modo amor, el atardecer vira a magenta.
 */
public final class CoverFactory {

    private CoverFactory() {}

    public static BufferedImage skyline(long payloadBytes, boolean love) {
        long needPixels = ((payloadBytes + 4) * 8L + 2) / 3L + 1;
        int w = (int) Math.max(960, Math.ceil(Math.sqrt(needPixels * 16.0 / 9.0 * 1.03)));
        int h = (int) Math.max(540, Math.ceil(w * 9.0 / 16.0));
        while ((long) w * h < needPixels) { w += 16; h = (int) Math.ceil(w * 9.0 / 16.0); }

        Random rnd = new Random();
        int[] px = new int[w * h];

        int[] top = love ? new int[]{18, 0, 40} : new int[]{6, 8, 14};
        int[] hor = love ? new int[]{255, 60, 190} : new int[]{70, 78, 64};

        // cielo en degradado + grano
        for (int y = 0; y < h; y++) {
            double t = Math.pow(y / (double) h, 1.6);
            int r = (int) (top[0] + (hor[0] - top[0]) * t);
            int g = (int) (top[1] + (hor[1] - top[1]) * t);
            int b = (int) (top[2] + (hor[2] - top[2]) * t);
            for (int x = 0; x < w; x++) {
                int n = rnd.nextInt(9) - 4;
                px[y * w + x] = pack(r + n, g + n, b + n);
            }
        }
        // estrellas
        int stars = Math.max(60, w * h / 6000);
        for (int i = 0; i < stars; i++) {
            int x = rnd.nextInt(w), y = rnd.nextInt(Math.max(1, h / 2));
            px[y * w + x] = pack(235, 235, 220);
        }
        // edificios con ventanas encendidas
        int x = 0;
        int accent = love ? pack(255, 210, 250) : pack(225, 255, 120);
        while (x < w) {
            int bw = Math.max(12, (int) (w * (0.02 + rnd.nextDouble() * 0.05)));
            int bh = (int) (h * (0.18 + rnd.nextDouble() * 0.45));
            int base = love ? pack(14, 2, 24) : pack(5, 5, 5);
            for (int yy = h - bh; yy < h; yy++) {
                for (int xx = x; xx < Math.min(w, x + bw); xx++) {
                    int n = rnd.nextInt(5);
                    px[yy * w + xx] = base + n * 0x010101;
                }
            }
            int step = Math.max(6, bw / 5);
            for (int yy = h - bh + step; yy < h - step; yy += step * 2) {
                for (int xx = x + step / 2; xx < Math.min(w, x + bw) - step / 2; xx += step * 2) {
                    if (rnd.nextInt(100) < 38) {
                        for (int dy = 0; dy < step / 2 && yy + dy < h; dy++)
                            for (int dx = 0; dx < step / 2 && xx + dx < w; dx++)
                                px[(yy + dy) * w + xx + dx] = accent;
                    }
                }
            }
            x += bw + rnd.nextInt(4);
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, w, h, px, 0, w);
        return img;
    }

    private static int pack(int r, int g, int b) {
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
