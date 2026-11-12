package app.crypto;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Cifrado autenticado AES-256-GCM con llave derivada por PBKDF2-HMAC-SHA256.
 *
 * Formato del blob:
 *   MAGIC(4 "ARHK") | VERSION(1) | SALT(16) | IV(12) | CIPHERTEXT + TAG(16)
 * MAGIC y VERSION viajan como AAD: si alguien los altera, el tag GCM falla.
 * Salt e IV son aleatorios por mensaje (nunca se reutiliza un IV con la misma llave).
 */
public final class CryptoService {

    public static final int SALT_LEN = 16;
    public static final int IV_LEN = 12;
    public static final int TAG_BITS = 128;
    public static final int KDF_ITERATIONS = 210_000;
    public static final int HEADER_LEN = 4 + 1 + SALT_LEN + IV_LEN;
    /** Bytes que el blob cifrado añade al texto plano: cabecera + tag. */
    public static final int OVERHEAD = HEADER_LEN + TAG_BITS / 8;

    private static final byte[] MAGIC = {'A', 'R', 'H', 'K'};
    private static final byte VERSION = 1;

    private final SecureRandom random = new SecureRandom();

    public byte[] encrypt(byte[] plain, char[] password) throws CryptoException {
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        random.nextBytes(salt);
        random.nextBytes(iv);

        byte[] keyBytes = deriveKey(password, salt);
        try {
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad());
            byte[] ct = cipher.doFinal(plain);

            byte[] out = new byte[HEADER_LEN + ct.length];
            System.arraycopy(MAGIC, 0, out, 0, 4);
            out[4] = VERSION;
            System.arraycopy(salt, 0, out, 5, SALT_LEN);
            System.arraycopy(iv, 0, out, 5 + SALT_LEN, IV_LEN);
            System.arraycopy(ct, 0, out, HEADER_LEN, ct.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Falló el cifrado AES-256-GCM: " + e.getMessage(), e);
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    public byte[] decrypt(byte[] blob, char[] password) throws CryptoException {
        if (blob == null || blob.length < OVERHEAD) {
            throw new CryptoException("Datos demasiado cortos: eso no es un bloque del Ministerio Del Rebusque.");
        }
        for (int i = 0; i < 4; i++) {
            if (blob[i] != MAGIC[i]) {
                throw new CryptoException("Firma inválida: esta imagen no trae nada del Ministerio Del Rebusque.");
            }
        }
        if (blob[4] != VERSION) {
            throw new CryptoException("Versión de formato no soportada: " + blob[4]);
        }
        byte[] salt = Arrays.copyOfRange(blob, 5, 5 + SALT_LEN);
        byte[] iv = Arrays.copyOfRange(blob, 5 + SALT_LEN, HEADER_LEN);

        byte[] keyBytes = deriveKey(password, salt);
        try {
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad());
            return cipher.doFinal(blob, HEADER_LEN, blob.length - HEADER_LEN);
        } catch (AEADBadTagException e) {
            throw new CryptoException("Contraseña incorrecta o la imagen fue alterada.", e);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Falló el descifrado: " + e.getMessage(), e);
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    private static byte[] aad() {
        return new byte[]{MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3], VERSION};
    }

    private static byte[] deriveKey(char[] password, byte[] salt) throws CryptoException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, KDF_ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new CryptoException("No se pudo derivar la llave (PBKDF2): " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }
}
