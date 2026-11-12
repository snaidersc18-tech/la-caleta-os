package app.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Contenido lógico que se cifra: un archivo cualquiera o una nota de texto.
 * Serialización: KIND(1) | UTF(name) | LEN(4) | DATA
 */
public record Payload(Kind kind, String name, byte[] data) {

    public enum Kind {
        FILE((byte) 1), NOTE((byte) 2);
        final byte id;
        Kind(byte id) { this.id = id; }
        static Kind of(byte id) throws CryptoException {
            for (Kind k : values()) if (k.id == id) return k;
            throw new CryptoException("Tipo de contenido desconocido: " + id);
        }
    }

    public static Payload file(String name, byte[] data) { return new Payload(Kind.FILE, name, data); }

    public static Payload note(String text) {
        return new Payload(Kind.NOTE, "nota.txt", text.getBytes(StandardCharsets.UTF_8));
    }

    public String noteText() { return new String(data, StandardCharsets.UTF_8); }

    public byte[] pack() throws CryptoException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length + 64);
             DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(kind.id);
            out.writeUTF(name);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new CryptoException("No se pudo empaquetar el contenido.", e);
        }
    }

    public static Payload unpack(byte[] raw) throws CryptoException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            Kind kind = Kind.of(in.readByte());
            String name = in.readUTF();
            int len = in.readInt();
            if (len < 0 || len > raw.length) throw new CryptoException("Longitud de datos corrupta.");
            byte[] data = new byte[len];
            in.readFully(data);
            return new Payload(kind, name, data);
        } catch (IOException e) {
            throw new CryptoException("Contenido corrupto o truncado.", e);
        }
    }

    /**
     * Nombre seguro para escribir en disco: solo el último segmento, sin separadores
     * ni "..", para impedir path traversal con una imagen maliciosa.
     */
    public String safeFileName() {
        String n = name == null ? "" : name.replace('\\', '/');
        Path p = Path.of(n.isBlank() ? "extraido.bin" : n).getFileName();
        String s = p == null ? "extraido.bin" : p.toString();
        if (s.isBlank() || s.equals("..") || s.equals(".")) s = "extraido.bin";
        return s;
    }
}
