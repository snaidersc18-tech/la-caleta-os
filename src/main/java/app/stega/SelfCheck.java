package app.stega;

import app.crypto.CryptoException;
import app.crypto.CryptoService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/**
 * Comprobación de humo (sin JavaFX):  java -cp target/classes app.stega.SelfCheck
 * Cifra, esconde, relee, descifra y prueba contraseña errónea y manipulación.
 */
public final class SelfCheck {
    public static void main(String[] args) throws Exception {
        Random r = new Random(7);
        byte[] data = new byte[300_000];
        r.nextBytes(data);
        Path dir = Files.createTempDirectory("arhk");
        Path src = dir.resolve("secreto.bin");
        Files.write(src, data);

        StegoVault v = new StegoVault();
        Path png = v.sealFile(src, "contraseña-larga-123".toCharArray(), null, dir, false,
                s -> System.out.println("  · " + s), p -> {});
        System.out.println("PNG: " + png + " (" + Files.size(png) / 1024 + " KB)");

        var back = v.open(png, "contraseña-larga-123".toCharArray(), s -> {}, p -> {});
        check(Arrays.equals(back.data(), data), "ida y vuelta exacta");
        check("secreto.bin".equals(back.name()), "nombre de archivo preservado");

        try { v.open(png, "otra-contraseña-mala".toCharArray(), s -> {}, p -> {}); check(false, "pass errónea"); }
        catch (CryptoException e) { check(true, "pass errónea rechazada: " + e.getMessage()); }

        // manipulación: cambiar un bit del payload debe romper el tag GCM
        var img = javax.imageio.ImageIO.read(png.toFile());
        int rgb = img.getRGB(40, 3);
        img.setRGB(40, 3, rgb ^ 0x010000);
        Path tampered = dir.resolve("t.png");
        javax.imageio.ImageIO.write(img, "png", tampered.toFile());
        try { v.open(tampered, "contraseña-larga-123".toCharArray(), s -> {}, p -> {}); check(false, "manipulación"); }
        catch (CryptoException e) { check(true, "manipulación detectada"); }

        // nota de texto + camuflaje propio
        Path note = dir.resolve("nota.png");
        v.sealNote("Para Sofía: ñandú, corazón ♥ y tildes áéíóú", "picaku-piojosa-1".toCharArray(), note, true, s -> {}, p -> {});
        check(v.open(note, "picaku-piojosa-1".toCharArray(), s -> {}, p -> {}).noteText()
                .equals("Para Sofía: ñandú, corazón ♥ y tildes áéíóú"), "nota UTF-8");

        check(new CryptoService().encrypt(new byte[]{1}, "abcdefgh".toCharArray()).length
                == 1 + CryptoService.OVERHEAD, "overhead esperado");
        System.out.println("TODO OK");
    }

    private static void check(boolean ok, String what) {
        System.out.println((ok ? "[OK]   " : "[FAIL] ") + what);
        if (!ok) throw new AssertionError(what);
    }
}
