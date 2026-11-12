package app.secret;

import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Gancho de teclado de los easter eggs, inyectado a nivel de VENTANA (no de escena).
 * <p>
 * Por qué en la ventana: JavaFX despacha un KeyEvent recorriendo la cadena Window → Scene → nodos, y los filtros se
 * ejecutan en ese orden. El núcleo instala su propio {@code KeyTrigger} como filtro de la ESCENA; un filtro en la
 * ventana corre antes que él, así que puede "reclamar" una palabra clave y consumir la última tecla antes de que el
 * núcleo la vea. Sin esto, "SOFIA" llegaba a los dos oyentes y se abrían dos interfaces a la vez.
 * <p>
 * Detalle fino: el KeyTrigger del núcleo ya guardó "SOFI" en su búfer. Si solo consumiéramos la "A", la próxima "A"
 * que se tecleara completaría su palabra. Por eso, tras reclamar, se inyecta un espacio sintético en la escena: el
 * búfer del núcleo queda en "SOFI " y su sufijo ya nunca puede coincidir.
 */
final class TriggerHook {

    private static final int MAX_BUFFER = 16;

    private final StringBuilder buf = new StringBuilder();
    private final Map<String, Runnable> words = new LinkedHashMap<>();
    private Runnable onKeystroke = () -> { };
    private java.util.function.Consumer<String> onBuffer = s -> { };
    private boolean injecting;

    /** Registra una palabra clave reclamada por app.secret. */
    TriggerHook on(String word, Runnable action) {
        words.put(word.toUpperCase(Locale.ROOT), action);
        return this;
    }

    /** Se llama en cada tecla imprimible (sonido de tecleo mecánico). */
    TriggerHook onKeystroke(Runnable r) {
        onKeystroke = r;
        return this;
    }

    /** Se llama con el búfer actual tras cada tecla (eco de la línea de comandos en la consola). */
    TriggerHook onBuffer(java.util.function.Consumer<String> c) {
        onBuffer = c;
        return this;
    }

    void install(Scene scene) {
        // MainView entra en la escena antes de que la escena tenga ventana (stage.setScene llega después):
        // si aún no hay ventana, se espera a que se asigne.
        if (scene.getWindow() != null) attach(scene, scene.getWindow());
        else scene.windowProperty().addListener((o, old, w) -> { if (w != null && old == null) attach(scene, w); });
    }

    private void attach(Scene scene, Window window) {
        window.addEventFilter(KeyEvent.KEY_TYPED, e -> handle(scene, e));
        // Zero-UI: sin botones de escape, un campo de texto con foco "secuestraría" el teclado. ESC lo suelta para
        // que se puedan escribir comandos (CALLE / SALIR). Solo actúa si el foco está en un campo: el ESC de las
        // cinemáticas (saltar) sigue funcionando igual.
        window.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && scene.getFocusOwner() instanceof TextInputControl && scene.getRoot() != null) {
                scene.getRoot().requestFocus();
                buf.setLength(0);
                onBuffer.accept("");
                e.consume();
            }
        });
    }

    private void handle(Scene scene, KeyEvent e) {
        if (injecting) return;                                   // nuestro propio espacio sintético: no se procesa
        String ch = e.getCharacter();
        if (ch == null || ch.isEmpty() || ch.charAt(0) < 32) return;
        onKeystroke.run();
        // Igual que el núcleo: lo que se teclea dentro de un campo (contraseñas, notas) nunca dispara nada.
        if (scene.getFocusOwner() instanceof TextInputControl) { buf.setLength(0); onBuffer.accept(""); return; }

        buf.append(ch.toUpperCase(Locale.ROOT));
        if (buf.length() > MAX_BUFFER) buf.delete(0, buf.length() - MAX_BUFFER);
        String typed = buf.toString();
        onBuffer.accept(typed.trim());
        for (Map.Entry<String, Runnable> w : words.entrySet()) {
            if (typed.endsWith(w.getKey())) {
                buf.setLength(0);
                onBuffer.accept("");
                e.consume();                                     // la última letra no llega a la escena ni al núcleo
                breakCoreSuffix(scene);
                w.getValue().run();
                return;
            }
        }
    }

    /** Inyecta un espacio en la escena para romper el sufijo que el KeyTrigger del núcleo tenga a medias. */
    private void breakCoreSuffix(Scene scene) {
        injecting = true;
        try {
            Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_TYPED, " ", "", KeyCode.UNDEFINED, false, false, false, false));
        } finally {
            injecting = false;                                   // fireEvent es síncrono: al volver ya pasó por todos los filtros
        }
    }
}
