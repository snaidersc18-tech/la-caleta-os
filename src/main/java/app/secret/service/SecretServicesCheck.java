package app.secret.service;

import java.nio.file.Files;
import java.nio.file.Path;

/** Prueba de humo SIN JavaFX:  java -cp target/classes app.secret.service.SecretServicesCheck [foto-con-exif.jpg] */
public final class SecretServicesCheck {
    public static void main(String[] a) throws Exception {
        Path dir = Files.createTempDirectory("caleta");
        // ---- SecureWiper
        Path f = dir.resolve("original.bin");
        byte[] data = new byte[3_500_000];
        new java.util.Random(3).nextBytes(data);
        Files.write(f, data);
        Path link = dir.resolve("hardlink.bin");
        Files.createLink(link, f);                       // mismo inodo: permite ver el contenido tras el wipe
        var r1 = new SecureWiper().overwriteWithZeros(f);
        byte[] after = Files.readAllBytes(link);
        boolean allZero = true; for (byte x : after) if (x != 0) { allZero = false; break; }
        ok(r1.verifiedZero() && allZero && after.length == data.length, "overwriteWithZeros: " + r1.bytes() + " bytes a cero");
        new SecureWiper().wipe(f);
        ok(!Files.exists(f), "wipe: el original ya no existe");
        ok(Files.size(link) == 0, "wipe: el contenido por otro enlace quedó truncado a 0");
        try { new SecureWiper().wipe(dir); ok(false, "carpeta"); } catch (java.io.IOException e) { ok(true, "wipe rechaza carpetas"); }

        // ---- MetadataScrubber
        if (a.length > 0) {
            var s = new MetadataScrubber();
            Path src = Path.of(a[0]);
            var rep = s.inspect(src);
            System.out.println("  auditoría: " + rep.format() + " -> " + rep.summary());
            Path tmp = s.scrubToTempPng(src);
            ok(s.isClean(tmp), "portadora limpia: solo IHDR/IDAT/IEND " + s.chunkTypes(Files.readAllBytes(tmp)));
            ok(!s.inspect(tmp).any(), "re-auditoría de la limpia: 0 metadatos");
            Files.deleteIfExists(tmp);
        }
        System.out.println("TODO OK");
    }
    private static void ok(boolean c, String m) { System.out.println((c ? "[OK]   " : "[FAIL] ") + m); if (!c) throw new AssertionError(m); }
}
