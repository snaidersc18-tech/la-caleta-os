package app.secret;

import app.secret.audio.AudioManager;
import app.ui.ConsolePanel;
import app.ui.MainView;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Fachada mínima de MainView para los easter eggs (Facade + inversión de dependencias): los modos solo conocen este
 * objeto. Aporta una capa propia a pantalla completa (debajo del bloqueo de entrada y de la capa CRT), la máquina de
 * estados del takeover, el audio y las dos operaciones que bloquean/restauran la interfaz base sin modificarla.
 */
public final class SecretContext {

    public static final double W = MainView.W, H = MainView.H;

    private final MainView ui;
    private final Pane layer = new Pane();
    /** Capa por ENCIMA de la interfaz del modo (y debajo del bloqueo/CRT): aquí se monta el Mordisco de la Araña. */
    private final Pane overlay = new Pane();
    private final TakeoverStateMachine fsm = new TakeoverStateMachine();
    private final AudioManager audio;
    private Runnable streetCommand = () -> { };

    public SecretContext(MainView ui, AudioManager audio) {
        this.ui = ui;
        this.audio = audio;
        layer.setMinSize(W, H);
        layer.setPrefSize(W, H);
        layer.setMaxSize(W, H);
        layer.setPickOnBounds(false);          // solo reciben ratón los nodos hijos
        int idx = ui.getChildren().indexOf(ui.lock());
        ui.getChildren().add(idx >= 0 ? idx : ui.getChildren().size(), layer);
        overlay.setMinSize(W, H);
        overlay.setPrefSize(W, H);
        overlay.setMaxSize(W, H);
        overlay.setPickOnBounds(false);
        ui.getChildren().add(ui.getChildren().indexOf(layer) + 1, overlay);
        hookConsoleTyping();
    }

    public MainView ui() { return ui; }
    public Pane layer() { return layer; }
    public Pane overlay() { return overlay; }
    public ConsolePanel console() { return ui.console(); }
    public TakeoverStateMachine fsm() { return fsm; }
    public AudioManager audio() { return audio; }

    /** EasterEggManager registra aquí su flujo "CALLE" (El Mordisco + péndulo de vuelta al Ministerio). */
    void onStreetCommand(Runnable r) { streetCommand = r; }

    /**
     * Una vista con consola propia (p. ej. FERXXO RIDER, cuyo campo de texto no pasa por el TriggerHook) pide volver a
     * la calle: mismo flujo que teclear CALLE en cualquier otra parte.
     */
    public void requestStreet() { streetCommand.run(); }

    /** Línea en la consola base (oculta durante el takeover, pero queda el registro). Segura desde cualquier hilo. */
    public void log(String message) { ui.console().log(message); }

    /**
     * Fase TAKEOVER: la interfaz base se bloquea y desaparece en el MISMO pulso, sin fundido. Deshabilitarla además de
     * ocultarla importa: un nodo invisible pero con foco (p. ej. un campo de texto) seguiría recibiendo teclas; un nodo
     * deshabilitado pierde el foco y no lo puede recuperar.
     */
    public void takeover() {
        Node window = ui.shakeTarget();
        window.setDisable(true);
        window.setVisible(false);
        window.setOpacity(0);
        layer.requestFocus();
    }

    /** Fase RESTORING: devuelve la interfaz base tal como estaba, con un fundido corto. */
    public void restore() {
        Node window = ui.shakeTarget();
        window.setDisable(false);
        window.setVisible(true);
        FadeTransition f = new FadeTransition(Duration.millis(500), window);
        f.setFromValue(0);
        f.setToValue(1);
        f.play();
        ui.requestFocus();
    }

    /** Coloca {@code node} en la capa justo DEBAJO de {@code above} (o encima de todo si {@code above} es null). */
    public void mountBelow(Node node, Node above) {
        int idx = above == null ? -1 : layer.getChildren().indexOf(above);
        if (idx < 0) layer.getChildren().add(node);
        else layer.getChildren().add(idx, node);
    }

    /**
     * Gancho de sonido para el efecto máquina de escribir del núcleo (TypewriterEffect escribe con typeChar()).
     * Se escucha el texto del cuerpo que ConsolePanel expone de solo lectura (sin recorrer sus nodos internos) y solo
     * suena cuando crece exactamente 1 carácter: un println() completo no es "tecleo".
     */
    private void hookConsoleTyping() {
        ui.console().bodyTextProperty().addListener((o, before, after) -> {
            if (before != null && after != null && after.length() == before.length() + 1 && fsm.isIdle()) audio.keyClick();
        });
    }
}
