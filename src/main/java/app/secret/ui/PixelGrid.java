package app.secret.ui;

import java.util.Random;

/**
 * Lienzo de píxeles de índices (0 = transparente) con primitivas para generar pixel art por código.
 * Sin dependencias de JavaFX: se puede probar y previsualizar en cualquier JVM.
 */
public final class PixelGrid {

    public final int w, h;
    /** Celdas (fila, columna). Visible solo dentro de app.secret.ui: fuera se usa get/set. */
    final int[][] g;

    public PixelGrid(int w, int h) { this.w = w; this.h = h; this.g = new int[h][w]; }

    public boolean in(int x, int y) { return x >= 0 && y >= 0 && x < w && y < h; }
    public int get(int x, int y) { return in(x, y) ? g[y][x] : 0; }
    public void set(int x, int y, int v) { if (in(x, y)) g[y][x] = v; }

    public void rect(int x, int y, int rw, int rh, int v) {
        for (int j = y; j < y + rh; j++) for (int i = x; i < x + rw; i++) set(i, j, v);
    }

    public void ellipse(double cx, double cy, double rx, double ry, int v) {
        for (int y = (int) Math.floor(cy - ry); y <= Math.ceil(cy + ry); y++)
            for (int x = (int) Math.floor(cx - rx); x <= Math.ceil(cx + rx); x++) {
                double nx = (x - cx) / (rx + 0.4), ny = (y - cy) / (ry + 0.4);
                if (nx * nx + ny * ny <= 1.0) set(x, y, v);
            }
    }

    /** Línea de Bresenham con pincel cuadrado de radio r. */
    public void line(int x0, int y0, int x1, int y1, int v, int r) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            for (int j = -r; j <= r; j++) for (int i = -r; i <= r; i++) set(x0 + i, y0 + j, v);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    /** Polígono relleno (par-impar por scanline). Solo pinta donde {@code only} == -1 o el píxel vale {@code only}. */
    public void poly(double[] xs, double[] ys, int v) { poly(xs, ys, v, -1); }

    public void poly(double[] xs, double[] ys, int v, int only) {
        int n = xs.length;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double y : ys) { minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
        for (int y = (int) Math.floor(minY); y <= (int) Math.ceil(maxY); y++) {
            double sy = y + 0.5;
            double[] cross = new double[n];
            int c = 0;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                if ((ys[i] > sy) != (ys[j] > sy)) cross[c++] = xs[i] + (sy - ys[i]) * (xs[j] - xs[i]) / (ys[j] - ys[i]);
            }
            java.util.Arrays.sort(cross, 0, c);
            for (int k = 0; k + 1 < c; k += 2)
                for (int x = (int) Math.round(cross[k]); x < (int) Math.round(cross[k + 1]); x++)
                    if (only < 0 || get(x, y) == only) set(x, y, v);
        }
    }

    /** Contorno de 1 px (valor {@code ink}) en todo borde entre píxeles rellenos y vacíos. */
    public void outline(int ink) {
        int[][] src = new int[h][];
        for (int y = 0; y < h; y++) src[y] = g[y].clone();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if (src[y][x] != 0) continue;
                boolean edge = (x > 0 && src[y][x - 1] != 0) || (x < w - 1 && src[y][x + 1] != 0)
                        || (y > 0 && src[y - 1][x] != 0) || (y < h - 1 && src[y + 1][x] != 0);
                if (edge) g[y][x] = ink;
            }
    }

    /** Mezcla ordenada 2x2 entre dos índices: t en [0,1] (0 = a, 1 = b). */
    public static int dither(int x, int y, double t, int a, int b) {
        int[][] bayer = {{0, 2}, {3, 1}};
        double th = (bayer[y & 1][x & 1] + 0.5) / 4.0;
        return t > th ? b : a;
    }

    public static Random rng(long seed) { return new Random(seed); }
}
