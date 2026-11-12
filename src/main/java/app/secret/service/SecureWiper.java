package app.secret.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * "Wipe Kamikaze": sobrescribe el archivo con ceros, fuerza el vaciado a disco, lo verifica
 * releyendo, lo trunca, lo renombra a un nombre aleatorio y lo elimina.
 *
 * LÍMITES REALES: en SSD (wear-leveling), sistemas con journaling/copy-on-write (APFS, btrfs, ZFS),
 * snapshots o papelera/backup, sobrescribir no garantiza que los bloques físicos viejos desaparezcan.
 * Para esos casos usa cifrado de disco completo. Aquí se hace lo máximo posible a nivel de archivo.
 */
public final class SecureWiper {

    public record Result(long bytes, boolean verifiedZero) {}

    private static final int BLOCK = 1 << 20;
    private final SecureRandom random = new SecureRandom();

    /** Solo la fase de sobrescritura + verificación (pública para poder probarla por separado). */
    public Result overwriteWithZeros(Path file) throws IOException {
        requireRegular(file);
        long size = Files.size(file);
        file.toFile().setWritable(true);
        boolean zero;
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.WRITE)) {
            ByteBuffer zeros = ByteBuffer.allocate(BLOCK);
            long pos = 0;
            while (pos < size) {
                zeros.clear();
                zeros.limit((int) Math.min(BLOCK, size - pos));
                while (zeros.hasRemaining()) pos += ch.write(zeros, pos);
            }
            ch.force(true);
        }
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate(BLOCK);
            zero = true;
            long pos = 0;
            while (pos < size && zero) {
                buf.clear();
                int n = ch.read(buf, pos);
                if (n < 0) break;
                for (int i = 0; i < n; i++) if (buf.get(i) != 0) { zero = false; break; }
                pos += n;
            }
        }
        return new Result(size, zero);
    }

    /** Sobrescribe, trunca, renombra y elimina. Falla (sin borrar) si la verificación no da ceros. */
    public Result wipe(Path file) throws IOException {
        Result r = overwriteWithZeros(file);
        if (!r.verifiedZero()) throw new IOException("La verificación de ceros falló; el archivo NO fue eliminado.");
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.WRITE)) {
            ch.truncate(0);
            ch.force(true);
        }
        Path target = file;
        try {
            byte[] rnd = new byte[8];
            random.nextBytes(rnd);
            target = file.resolveSibling(HexFormat.of().formatHex(rnd) + ".tmp");
            Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | UnsupportedOperationException e) {
            target = file; // si no se puede renombrar, se elimina con el nombre original
        }
        Files.delete(target);
        return r;
    }

    private static void requireRegular(Path file) throws IOException {
        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Solo se destruyen archivos regulares (no enlaces ni carpetas): " + file.getFileName());
        }
    }
}
