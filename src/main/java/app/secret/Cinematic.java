package app.secret;

import javafx.scene.Node;

/**
 * Contrato de una fase cinemática del takeover (preludio "Dave the Diver", escenario piramidal, playa de Concho...).
 * <ol>
 *   <li>{@link #play(Runnable)} arranca la secuencia. {@code onFinished} se invoca UNA sola vez, desde el
 *       {@code setOnFinished} de la transición maestra (o al saltarla), con la pantalla cubierta por el último cuadro.</li>
 *   <li>Quien la orquesta monta entonces lo siguiente DEBAJO de {@link #node()} y llama a {@link #dissolve()}, que la
 *       desvanece revelándolo y libera animaciones y partículas.</li>
 *   <li>{@link #stop()} es el corte inmediato (salir a la calle a mitad de la cinemática).</li>
 * </ol>
 */
public interface Cinematic {
    Node node();
    void play(Runnable onFinished);
    void dissolve();
    void stop();
}
