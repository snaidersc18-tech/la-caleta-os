package app.ui;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.io.InputStream;

/**
 * Carga opcional de una fuente pixel desde /fonts/pixel.ttf (por ejemplo VT323 o
 * "Share Tech Mono"/"Press Start 2P", todas con licencia OFL). Si no existe, cae a Monospaced.
 */
public final class PixelFonts {
    private static String family;

    private PixelFonts() {}

    public static synchronized String family() {
        if (family == null) {
            family = "Monospaced";
            try (InputStream in = PixelFonts.class.getResourceAsStream("/fonts/pixel.ttf")) {
                if (in != null) {
                    Font f = Font.loadFont(in, 14);
                    if (f != null) family = f.getFamily();
                }
            } catch (IOException | RuntimeException ex) {             // se queda en Monospaced, pero queda rastro
                System.err.println("[fuente] No pude cargar /fonts/pixel.ttf: " + ex);
            }
        }
        return family;
    }

    public static Font font(double size, boolean bold) {
        return Font.font(family(), bold ? FontWeight.BOLD : FontWeight.NORMAL, size);
    }
}
