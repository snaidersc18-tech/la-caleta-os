package app.secret.ui;

/** Sprites pixel art pequeños generados por código: notas musicales que "canta" la mascota (MascotGroove). */
public final class Sprites {

    private Sprites() {}

    /** Nota musical de 7 px de ancho; dos variantes (corchea / doble corchea) alternadas por {@code variant}. */
    public static PixelGrid note(int variant) {
        String[] a = {"...XXX.", "...X..X", "...X...", "...X...", "...X...", ".XXX...", "XXXX...", ".XX...."},
                b = {"..XXXXX", "..X...X", "..X...X", "..X...X", ".XX..XX", "XXX.XXX", ".X...X."};
        String[] s = variant % 2 == 0 ? a : b;
        PixelGrid p = new PixelGrid(7, s.length);
        for (int y = 0; y < s.length; y++) for (int x = 0; x < 7; x++) if (s[y].charAt(x) == 'X') p.set(x, y, 1);
        return p;
    }
}
