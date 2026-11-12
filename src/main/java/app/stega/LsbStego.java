package app.stega;

import java.awt.image.BufferedImage;

/**
 * Esteganografía LSB: 1 bit por canal (R,G,B) => 3 bits por píxel.
 *
 * Disposición: LONGITUD (32 bits, big-endian) seguida de los bytes, MSB primero,
 * recorriendo píxeles en orden de filas. El alfa se descarta (salida RGB opaca).
 * Como lo que se incrusta ya es texto cifrado (indistinguible de ruido),
 * el cambio estadístico en los bits bajos es mínimo.
 */
public final class LsbStego {

    private static final int LEN_BITS = 32;

    private LsbStego() {}

    /** Bytes de carga útil que caben en una imagen de w x h. */
    public static long capacityBytes(int w, int h) {
        long bits = (long) w * h * 3L - LEN_BITS;
        return Math.max(0, bits / 8);
    }

    public static BufferedImage embed(BufferedImage cover, byte[] data) throws StegoException {
        int w = cover.getWidth(), h = cover.getHeight();
        if (data.length > capacityBytes(w, h)) {
            throw new StegoException("El camuflaje solo aguanta " + capacityBytes(w, h)
                    + " bytes y el paquete pesa " + data.length + ". Usa una imagen más grande.");
        }
        int[] px = cover.getRGB(0, 0, w, h, null, 0, w);
        long bit = 0;
        bit = writeBits(px, bit, data.length, LEN_BITS);
        for (byte b : data) bit = writeBits(px, bit, b & 0xFF, 8);

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    public static byte[] extract(BufferedImage img) throws StegoException {
        int w = img.getWidth(), h = img.getHeight();
        long cap = capacityBytes(w, h);
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        long bit = 0;
        long len = 0;
        for (int i = 0; i < LEN_BITS; i++) len = (len << 1) | readBit(px, bit++);
        if (len < 0 || len > cap) {
            throw new StegoException("Esta imagen no parece traer carga oculta (longitud inválida).");
        }
        byte[] out = new byte[(int) len];
        for (int i = 0; i < out.length; i++) {
            int v = 0;
            for (int j = 0; j < 8; j++) v = (v << 1) | readBit(px, bit++);
            out[i] = (byte) v;
        }
        return out;
    }

    private static long writeBits(int[] px, long bitPos, int value, int count) {
        for (int i = count - 1; i >= 0; i--) {
            int bit = (value >>> i) & 1;
            int pixel = (int) (bitPos / 3);
            int channel = (int) (bitPos % 3);          // 0=R 1=G 2=B
            int shift = 16 - 8 * channel;
            int rgb = px[pixel];
            rgb = (rgb & ~(1 << shift)) | (bit << shift);
            px[pixel] = rgb | 0xFF000000;
            bitPos++;
        }
        return bitPos;
    }

    private static int readBit(int[] px, long bitPos) {
        int pixel = (int) (bitPos / 3);
        int channel = (int) (bitPos % 3);
        return (px[pixel] >>> (16 - 8 * channel)) & 1;
    }
}
