package app.animations;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

/**
 * Escucha lo que se teclea en la escena y dispara una acción cuando el texto termina en alguna
 * de las palabras clave (SOFIA / PICAKU). Se ignora mientras el foco esté dentro de un campo de
 * texto, para que contraseñas o notas que contengan esas palabras no activen el easter egg.
 */
public final class KeyTrigger {

    private KeyTrigger() {}

    public static void install(Scene scene, Runnable action, String... words) {
        StringBuilder buf = new StringBuilder();
        scene.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (scene.getFocusOwner() instanceof TextInputControl) { buf.setLength(0); return; }
            String ch = e.getCharacter();
            if (ch == null || ch.isEmpty() || ch.charAt(0) < 32) return;
            buf.append(ch.toUpperCase());
            if (buf.length() > 16) buf.delete(0, buf.length() - 16);
            String s = buf.toString();
            for (String w : words) {
                if (s.endsWith(w.toUpperCase())) {
                    buf.setLength(0);
                    action.run();
                    return;
                }
            }
        });
    }
}
