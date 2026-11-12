package app.secret.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Dibuja grids de sprites a escala entera (pixel-perfect). */
public final class PixelArt {
    private PixelArt() {}

    private static final java.util.Map<int[], Color[]> CACHE = new java.util.IdentityHashMap<>();

    /** Colores ya construidos por paleta (evita crear miles de Color por cuadro). */
    public static synchronized Color[] colors(int[] palette) {
        return CACHE.computeIfAbsent(palette, pal -> {
            Color[] c = new Color[pal.length];
            for (int i = 0; i < pal.length; i++) c[i] = Color.web(String.format("#%06x", pal[i]));
            return c;
        });
    }

    public static void draw(GraphicsContext g, int[][] grid, int[] palette, double x, double y, double px) {
        Color[] cols = colors(palette);
        for (int j = 0; j < grid.length; j++) {
            int i = 0;
            while (i < grid[j].length) {
                int v = grid[j][i];
                if (v == 0) { i++; continue; }
                int run = 1;
                while (i + run < grid[j].length && grid[j][i + run] == v) run++;
                g.setFill(cols[Math.min(v, cols.length - 1)]);
                g.fillRect(x + i * px, y + j * px, run * px, px);
                i += run;
            }
        }
    }

    public static Canvas canvas(int[][] grid, int[] palette, double px) {
        Canvas c = new Canvas(grid[0].length * px, grid.length * px);
        draw(c.getGraphicsContext2D(), grid, palette, 0, 0, px);
        return c;
    }
}
