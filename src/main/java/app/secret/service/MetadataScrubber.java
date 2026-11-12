package app.secret.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Harder, Better, Faster": limpieza de metadatos de la imagen portadora.
 *
 *  1. inspect():  AUDITA el archivo original (JPEG: EXIF/GPS/cámara/fecha/XMP/IPTC/ICC/comentarios;
 *                 PNG: eXIf, tEXt, iTXt, zTXt, tIME, iCCP...) para poder decir qué se destruyó.
 *  2. scrubToTempPng(): reconstruye los píxeles en un BufferedImage nuevo (RGB opaco, sin perfil ICC,
 *                 alfa compuesto sobre negro) y lo guarda como PNG temporal con SOLO los chunks críticos.
 *  3. stripAncillaryInPlace(): garantiza que el PNG final (el que ya trae el secreto) no conserve chunks
 *                 auxiliares. Solo toca chunks fuera de IDAT, así que los bits esteganografiados no cambian.
 */
public final class MetadataScrubber {

    public record Report(String format, boolean exif, boolean gps, String camera, String date,
                         boolean xmp, boolean iptc, boolean icc, boolean comments, List<String> otherChunks) {
        public boolean any() {
            return exif || gps || camera != null || date != null || xmp || iptc || icc || comments || !otherChunks.isEmpty();
        }

        public String summary() {
            List<String> parts = new ArrayList<>();
            if (gps) parts.add("GPS");
            if (camera != null) parts.add("cámara (" + camera + ")");
            if (date != null) parts.add("fecha (" + date + ")");
            if (exif && !gps && camera == null && date == null) parts.add("EXIF");
            if (xmp) parts.add("XMP");
            if (iptc) parts.add("IPTC");
            if (icc) parts.add("perfil de color ICC");
            if (comments) parts.add("comentarios");
            if (!otherChunks.isEmpty()) parts.add("chunks " + String.join("/", otherChunks));
            return String.join(", ", parts);
        }
    }

    private static final Set<String> CRITICAL = Set.of("IHDR", "PLTE", "IDAT", "IEND");
    private static final byte[] PNG_SIG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final long MAX_INSPECT = 96L * 1024 * 1024;

    // ================================================================== 1) auditoría

    public Report inspect(Path file) throws IOException {
        if (file == null) return new Report("(sin portadora)", false, false, null, null, false, false, false, false, List.of());
        if (Files.size(file) > MAX_INSPECT) throw new IOException("Portadora demasiado grande para auditar.");
        byte[] b = Files.readAllBytes(file);
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) return inspectJpeg(b);
        if (isPng(b)) return inspectPng(b);
        return new Report("otro", false, false, null, null, false, false, false, false, List.of());
    }

    private Report inspectJpeg(byte[] b) {
        boolean exif = false, xmp = false, iptc = false, icc = false, com = false;
        TiffInfo tiff = new TiffInfo();
        int i = 2;
        try {
            while (i + 4 <= b.length) {
                if ((b[i] & 0xFF) != 0xFF) { i++; continue; }
                int m = b[i + 1] & 0xFF;
                if (m == 0xFF) { i++; continue; }
                if (m == 0xD8 || (m >= 0xD0 && m <= 0xD7) || m == 0x01) { i += 2; continue; }
                if (m == 0xD9 || m == 0xDA) break;
                int len = ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
                int start = i + 4, end = i + 2 + len;
                if (len < 2 || end > b.length) break;
                if (m == 0xE1 && startsWith(b, start, "Exif\0\0")) { exif = true; parseTiff(b, start + 6, end, tiff); }
                else if (m == 0xE1 && startsWith(b, start, "http://ns.adobe.com/xap/1.0/")) xmp = true;
                else if (m == 0xED) iptc = true;
                else if (m == 0xE2 && startsWith(b, start, "ICC_PROFILE")) icc = true;
                else if (m == 0xFE) com = true;
                i = end;
            }
        } catch (RuntimeException ignored) {
            exif = true; // metadatos ilegibles pero presentes
        }
        return new Report("JPEG", exif, tiff.gps, tiff.camera(), tiff.date, xmp, iptc, icc, com, List.of());
    }

    private Report inspectPng(byte[] b) {
        Set<String> aux = new LinkedHashSet<>();
        boolean exif = false, xmp = false, icc = false, com = false;
        for (String t : chunkTypes(b)) {
            if (CRITICAL.contains(t)) continue;
            switch (t) {
                case "eXIf" -> exif = true;
                case "iCCP" -> icc = true;
                case "tEXt", "zTXt" -> com = true;
                case "iTXt" -> { com = true; xmp = true; }
                default -> aux.add(t);
            }
        }
        return new Report("PNG", exif, false, null, null, xmp, false, icc, com, new ArrayList<>(aux));
    }

    // ================================================================== 2) reconstrucción de píxeles

    /** Copia solo los píxeles: RGB opaco, sin ICC, sin alfa (compuesto sobre negro), sin metadatos. */
    public static BufferedImage rebuild(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            src.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int p = row[x], a = p >>> 24;
                int r = ((p >> 16) & 0xFF) * a / 255, g = ((p >> 8) & 0xFF) * a / 255, bl = (p & 0xFF) * a / 255;
                row[x] = (r << 16) | (g << 8) | bl;
            }
            out.setRGB(0, y, w, 1, row, 0, w);
        }
        return out;
    }

    /** Portadora limpia como PNG temporal (el llamador debe borrarlo: ver CoverLease). */
    public Path scrubToTempPng(Path cover) throws IOException {
        BufferedImage src = ImageIO.read(cover.toFile());
        if (src == null) throw new IOException("No pude leer la portadora (use PNG, JPG, BMP o GIF).");
        BufferedImage clean = rebuild(src);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (!ImageIO.write(clean, "png", bos)) throw new IOException("No hay escritor PNG disponible.");
        byte[] png = stripAncillary(bos.toByteArray());
        Path tmp = Files.createTempFile("caleta_cover_", ".png");
        Files.write(tmp, png);
        return tmp;
    }

    // ================================================================== 3) PNG final

    /** Tipos de chunk presentes, en orden. */
    public List<String> chunkTypes(byte[] png) {
        List<String> out = new ArrayList<>();
        if (!isPng(png)) return out;
        int p = 8;
        while (p + 12 <= png.length) {
            long len = u32(png, p);
            if (len < 0 || p + 12 + len > png.length) break;
            out.add(new String(png, p + 4, 4, StandardCharsets.ISO_8859_1));
            p += 12 + (int) len;
        }
        return out;
    }

    /** Devuelve el PNG sin ningún chunk que no sea IHDR/PLTE/IDAT/IEND (los CRC se conservan intactos). */
    public byte[] stripAncillary(byte[] png) throws IOException {
        if (!isPng(png)) throw new IOException("No es un PNG válido.");
        ByteArrayOutputStream out = new ByteArrayOutputStream(png.length);
        out.write(png, 0, 8);
        int p = 8;
        while (p + 12 <= png.length) {
            long len = u32(png, p);
            if (len < 0 || p + 12 + len > png.length) throw new IOException("PNG truncado o corrupto.");
            String type = new String(png, p + 4, 4, StandardCharsets.ISO_8859_1);
            int total = 12 + (int) len;
            if (CRITICAL.contains(type)) out.write(png, p, total);
            p += total;
        }
        return out.toByteArray();
    }

    /** Reescribe el PNG sin chunks auxiliares. @return los tipos eliminados (vacío = ya estaba limpio). */
    public List<String> stripAncillaryInPlace(Path png) throws IOException {
        byte[] raw = Files.readAllBytes(png);
        List<String> removed = new ArrayList<>();
        for (String t : chunkTypes(raw)) if (!CRITICAL.contains(t)) removed.add(t);
        if (removed.isEmpty()) return removed;
        Path tmp = png.resolveSibling(png.getFileName() + ".clean");
        Files.write(tmp, stripAncillary(raw));
        Files.move(tmp, png, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return removed;
    }

    public boolean isClean(Path png) throws IOException {
        for (String t : chunkTypes(Files.readAllBytes(png))) if (!CRITICAL.contains(t)) return false;
        return true;
    }

    // ================================================================== EXIF / TIFF

    private static final class TiffInfo {
        String make, model, date;
        boolean gps;
        String camera() {
            String s = ((make == null ? "" : make) + " " + (model == null ? "" : model)).trim();
            return s.isEmpty() ? null : s;
        }
    }

    private static void parseTiff(byte[] b, int base, int end, TiffInfo info) {
        if (base + 8 > end) return;
        boolean le = b[base] == 'I';
        int ifd0 = (int) rd(b, base + 4, 4, le);
        readIfd(b, base, end, ifd0, le, info, 0);
    }

    private static void readIfd(byte[] b, int base, int end, int off, boolean le, TiffInfo info, int depth) {
        if (depth > 2) return;
        int p = base + off;
        if (off < 0 || p + 2 > end) return;
        int n = (int) rd(b, p, 2, le);
        for (int k = 0; k < n && k < 256; k++) {
            int e = p + 2 + k * 12;
            if (e + 12 > end) return;
            int tag = (int) rd(b, e, 2, le);
            int type = (int) rd(b, e + 2, 2, le);
            long count = rd(b, e + 4, 4, le);
            switch (tag) {
                case 0x010F -> info.make = ascii(b, base, end, e, type, count, le);
                case 0x0110 -> info.model = ascii(b, base, end, e, type, count, le);
                case 0x0132 -> { if (info.date == null) info.date = ascii(b, base, end, e, type, count, le); }
                case 0x9003 -> info.date = ascii(b, base, end, e, type, count, le);
                case 0x8769 -> readIfd(b, base, end, (int) rd(b, e + 8, 4, le), le, info, depth + 1);
                case 0x8825 -> {
                    int g = base + (int) rd(b, e + 8, 4, le);
                    if (g + 2 <= end && rd(b, g, 2, le) > 0) info.gps = true;
                }
                default -> { }
            }
        }
    }

    private static String ascii(byte[] b, int base, int end, int entry, int type, long count, boolean le) {
        if (type != 2 || count < 1 || count > 512) return null;
        int at = count <= 4 ? entry + 8 : base + (int) rd(b, entry + 8, 4, le);
        if (at < 0 || at + count > end) return null;
        String s = new String(b, at, (int) count, StandardCharsets.ISO_8859_1).replace("\0", "").trim();
        return s.isEmpty() ? null : s;
    }

    private static long rd(byte[] b, int p, int n, boolean le) {
        long v = 0;
        for (int i = 0; i < n; i++) {
            int x = b[p + (le ? n - 1 - i : i)] & 0xFF;
            v = (v << 8) | x;
        }
        return v;
    }

    private static boolean startsWith(byte[] b, int at, String s) {
        byte[] t = s.getBytes(StandardCharsets.ISO_8859_1);
        if (at + t.length > b.length) return false;
        for (int i = 0; i < t.length; i++) if (b[at + i] != t[i]) return false;
        return true;
    }

    private static boolean isPng(byte[] b) {
        if (b.length < 8) return false;
        for (int i = 0; i < 8; i++) if (b[i] != PNG_SIG[i]) return false;
        return true;
    }

    private static long u32(byte[] b, int p) { return rd(b, p, 4, false); }
}
