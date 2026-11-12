package app.narrative;

import java.util.Random;

/**
 * Narrativa del estado 1: sátira callejera y anti-sistema, homenaje al espíritu de
 * Alcolirykoz (con guiños a los títulos "La caza de Nariño" y "La funa"). Frases originales.
 */
public final class StreetLogs {
    private static final Random R = new Random();

    private StreetLogs() {}

    public static final String[] BOOT = {
            ">>> MINISTERIO DEL REBUSQUE en línea. Aquí no hay cámaras de seguridad, solo bytes sin permiso.",
            ">>> Cargando AES-256-GCM... más blindado que el discurso de un candidato en campaña.",
            ">>> Esteganografía lista: tu archivo se disfraza de foto bonita, como el barrio cuando lo gentrifican.",
            ">>> Antivirus de barrio listo, parcero. Tráeme la foto infectada y la curamos sin funa."
    };

    public static final String[] EXTRACT_START = {
            ">>> Sobreviviendo a la funa: nadie te señala, nadie te cancela, solo tú y tu secreto.",
            ">>> Buscando en los píxeles lo que la falsa moral prefiere no ver.",
            ">>> Escaneando la foto: la doble moral no vio nada, pero nosotros sí."
    };

    public static final String[] EXTRACT_DONE = {
            ">>> EXTRAÍDO. El secreto volvió a casa sin pasar por el juicio de la vitrina.",
            ">>> Recuperado. Cero juicios, cero likes, cero fiscales de la moral.",
            ">>> Listo el pollo: el archivo salió del escondite entero y sin cargos."
    };

    public static final String[] WRONG_PASSWORD = {
            ">>> ACCESO DENEGADO: contraseña incorrecta o imagen tocada. Hasta la funa perdona más.",
            ">>> Nada. Esa clave no abre este candado, hermano."
    };

    /** Comandos de la consola (navegación sin botones). */
    public static final String COMMANDS =
            ">>> Comandos: escribe CALLE para volver a la calle · SALIR para apagar el Ministerio · ESC suelta un campo de texto.";

    public static String pick(String[] a) { return a[R.nextInt(a.length)]; }
}
