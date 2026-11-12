package app.narrative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mensajes de la cascada de errores del Payload Picaku (satírico-románticos). */
public final class ErrorMessages {
    private ErrorMessages() {}

    public record Msg(String title, String body, String button) {}

    private static final List<Msg> ALL = List.of(
            new Msg("CRITICAL ERROR", "Demasiado hermosa.", "ACEPTAR"),
            new Msg("WARNING", "Sistema enamorado.", "IGNORAR"),
            new Msg("BREACH", "Picaku detectada en el perímetro.", "RENDIRSE"),
            new Msg("FATAL", "sonrisa.exe consume el 100% de la CPU.", "OK"),
            new Msg("KERNEL PANIC", "'corazón' no es un tipo de dato válido.", "CASTEAR"),
            new Msg("ACCESS DENIED", "El firewall se rindió ante unos ojos.", "NEGOCIAR"),
            new Msg("ERROR 404", "Tristeza no encontrada.", "BUSCAR"),
            new Msg("SEGFAULT", "Desbordamiento del buffer de ternura.", "REINICIAR"),
            new Msg("OVERFLOW", "piojosa > MAX_VALUE (Picaku Piojosa).", "ACEPTAR"),
            new Msg("ALERTA", "El Estado no tiene jurisdicción sobre este flechazo.", "APELAR"),
            new Msg("ANTIVIRUS", "Licencia vencida: el amor no renueva suscripción.", "RENOVAR"),
            new Msg("BREACH", "Se filtraron 3.000 mariposas por el puerto 143.", "CERRAR PUERTO"),
            new Msg("EXCEPTION", "NoPuedoDejarDePensarEnTiException.", "CATCH(TODO)"),
            new Msg("CRITICAL", "Contraseña = tu risa. Hash irrompible.", "ROMPER"),
            new Msg("WARNING", "1 beso pendiente de sincronización.", "SINCRONIZAR"),
            new Msg("DLL ERROR", "Alcolirykoz.dll reemplazado por Aerophon.dll.", "REVERTIR"),
            new Msg("SYSTEM", "reggaeton.sys eliminado. Entra saxofón.", "DALE"),
            new Msg("ERROR 0xCAFE", "Demasiado café, demasiada Sofia.", "OTRO CAFÉ"),
            new Msg("FATAL", "MINISTERIO DEL REBUSQUE conquistado.", "RESISTIR"),
            new Msg("STACK OVERFLOW", "Sonrisa recursiva sin caso base.", "SONREÍR"),
            new Msg("PANIC", "El corazón hizo rm -rf a mi serenidad.", "SUDO"),
            new Msg("WARNING", "Pulso a 180 bpm: el procesador no da abasto.", "RESPIRAR"),
            new Msg("BREACH", "Intrusión de poesía en capa 8 (la humana).", "VERSEAR"),
            new Msg("NULL", "NullPointerException: ¿dónde dejé la compostura?", "BUSCAR")
    );

    public static List<Msg> shuffled() {
        List<Msg> l = new ArrayList<>(ALL);
        Collections.shuffle(l);
        return l;
    }
}
