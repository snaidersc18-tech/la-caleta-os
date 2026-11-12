package app.stega;

import app.crypto.CryptoException;
import app.crypto.CryptoService;
import app.crypto.Payload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Orquesta el flujo real: empaquetar -> cifrar (AES-256-GCM) -> incrustar (LSB) -> guardar PNG
 * -> VERIFICAR releyendo el PNG y descifrando. Todo funciona sobre bytes reales; no hay simulación.
 */
public final class StegoVault {

    /** Tope de carga para no reventar la memoria (el camuflaje crece con la carga). */
    public static final long MAX_PAYLOAD_BYTES = 16L * 1024 * 1024;

    private final CryptoService crypto = new CryptoService();

    // ------------------------------------------------------------------ SELLAR

    public Path sealFile(Path source, char[] password, Path coverOrNull, Path outDir,
                         boolean love, Consumer<String> log, DoubleConsumer progress)
            throws IOException, CryptoException, StegoException {
        long size = Files.size(source);
        if (size > MAX_PAYLOAD_BYTES) {
            throw new StegoException("Archivo muy pesado (" + (size >> 20) + " MB). Máximo "
                    + (MAX_PAYLOAD_BYTES >> 20) + " MB: comprímelo o pártelo.");
        }
        byte[] data = Files.readAllBytes(source);
        Payload payload = Payload.file(source.getFileName().toString(), data);
        Path out = uniquePath(outDir, stripExt(source.getFileName().toString()) + ".arhk", ".png");
        return seal(payload, password, coverOrNull, out, love, log, progress);
    }

    public Path sealNote(String text, char[] password, Path outFile, boolean love,
                         Consumer<String> log, DoubleConsumer progress)
            throws IOException, CryptoException, StegoException {
        return seal(Payload.note(text), password, null, outFile, love, log, progress);
    }

    private Path seal(Payload payload, char[] password, Path coverOrNull, Path outFile, boolean love,
                      Consumer<String> log, DoubleConsumer progress)
            throws IOException, CryptoException, StegoException {
        progress.accept(0.05);
        byte[] packed = payload.pack();

        log.accept("derivando llave y sellando (AES-256-GCM, PBKDF2 x" + CryptoService.KDF_ITERATIONS + ")");
        byte[] blob = crypto.encrypt(packed, password);
        progress.accept(0.40);

        BufferedImage cover;
        if (coverOrNull != null) {
            cover = ImageIO.read(coverOrNull.toFile());
            if (cover == null) throw new StegoException("No pude leer el camuflaje: usa PNG, JPG, BMP o GIF.");
            log.accept("camuflaje cargado: " + cover.getWidth() + "x" + cover.getHeight()
                    + " (cabe " + LsbStego.capacityBytes(cover.getWidth(), cover.getHeight()) + " bytes)");
        } else {
            cover = CoverFactory.skyline(blob.length, love);
            log.accept("sin camuflaje: fabricando skyline " + cover.getWidth() + "x" + cover.getHeight());
        }
        progress.accept(0.55);

        BufferedImage stego = LsbStego.embed(cover, blob);
        progress.accept(0.75);

        Files.createDirectories(outFile.toAbsolutePath().getParent());
        if (!ImageIO.write(stego, "png", outFile.toFile())) {
            throw new StegoException("No se pudo escribir el PNG de salida.");
        }
        progress.accept(0.85);

        // Verificación de ida y vuelta: releer del disco, extraer, descifrar y comparar.
        log.accept("verificando sello (releyendo el PNG desde disco)");
        Payload back = openInternal(outFile, password);
        if (back.kind() != payload.kind() || !Arrays.equals(back.data(), payload.data())) {
            Files.deleteIfExists(outFile);
            throw new StegoException("La verificación falló: no se entregó nada. Reintenta.");
        }
        progress.accept(1.0);
        return outFile;
    }

    // ------------------------------------------------------------------ ABRIR

    public Payload open(Path stegoImage, char[] password, Consumer<String> log, DoubleConsumer progress)
            throws IOException, CryptoException, StegoException {
        progress.accept(0.1);
        log.accept("escaneando bits bajos de la imagen");
        Payload p = openInternal(stegoImage, password);
        progress.accept(1.0);
        return p;
    }

    private Payload openInternal(Path stegoImage, char[] password)
            throws IOException, CryptoException, StegoException {
        BufferedImage img = ImageIO.read(stegoImage.toFile());
        if (img == null) throw new StegoException("No pude leer la imagen (formato no soportado).");
        byte[] blob = LsbStego.extract(img);
        byte[] packed = crypto.decrypt(blob, password);
        return Payload.unpack(packed);
    }

    /** Escribe un payload de tipo FILE en disco con nombre seguro y sin pisar archivos existentes. */
    public Path writeFile(Payload p, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        String name = p.safeFileName();
        Path target = uniquePath(outDir, stripExt(name), extOf(name));
        Files.write(target, p.data());
        return target;
    }

    // ------------------------------------------------------------------ util

    public static Path uniquePath(Path dir, String base, String ext) {
        Path p = dir.resolve(base + ext);
        int i = 1;
        while (Files.exists(p)) p = dir.resolve(base + "_" + (i++) + ext);
        return p;
    }

    private static String stripExt(String n) {
        int i = n.lastIndexOf('.');
        return i > 0 ? n.substring(0, i) : n;
    }

    private static String extOf(String n) {
        int i = n.lastIndexOf('.');
        return i > 0 ? n.substring(i) : "";
    }
}
