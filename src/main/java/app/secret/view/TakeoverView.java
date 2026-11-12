package app.secret.view;

import javafx.animation.Animation;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de todas las vistas exclusivas (UI Takeover): un StackPane de 1280x720 que bloquea el ratón hacia lo que hay
 * debajo y lleva la cuenta de sus animaciones para detenerlas todas en {@link #dispose()}.
 */
public abstract class TakeoverView extends StackPane {

    public static final double W = 1280, H = 720;
    private final List<Animation> animations = new ArrayList<>();
    private final List<Runnable> cleanups = new ArrayList<>();
    private boolean disposed;

    protected TakeoverView() {
        setMinSize(W, H); setPrefSize(W, H); setMaxSize(W, H);
        setPickOnBounds(true);
        setFocusTraversable(true);
    }

    protected final <A extends Animation> A track(A a) { animations.add(a); return a; }
    protected final void onDispose(Runnable r) { cleanups.add(r); }

    /** Gancho: la vista acaba de montarse bajo el último cuadro de la cinemática (entra con su propia animación). */
    public void onShown() { }

    /** Detiene animaciones y libera recursos temporales. Idempotente. */
    public void dispose() {
        if (disposed) return;
        disposed = true;
        for (Animation a : animations) a.stop();
        animations.clear();
        for (Runnable r : cleanups) { try { r.run(); } catch (RuntimeException ignored) { /* seguir */ } }
        cleanups.clear();
    }

    /** Para callbacks asíncronos (Task) que pueden llegar después de cerrar la vista. */
    protected final boolean isDisposed() { return disposed; }

    /** dispose() + retirarse de su contenedor. */
    protected final void disposeAndRemove() {
        dispose();
        if (getParent() instanceof Pane p) p.getChildren().remove(this);
    }
}
