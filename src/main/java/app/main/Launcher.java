package app.main;

/**
 * Punto de entrada para el fat-jar. No extiende Application a propósito: así la JVM no exige
 * JavaFX en el module-path y "java -jar" funciona sin banderas extra.
 */
public final class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
