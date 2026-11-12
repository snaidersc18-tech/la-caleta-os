package app.secret.modes;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.SecretMode;
import app.secret.TakeoverStateMachine.State;
import app.secret.audio.MusicTrack;
import app.secret.audio.SoundEffect;
import app.secret.view.TakeoverView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Plantilla (Template Method) del UI Takeover. El flujo es SIEMPRE el mismo y lo dirige la máquina de estados:
 * <pre>
 *   activate()  ─► TAKEOVER   bloquea y oculta la UI base en el mismo pulso (+ SFX de entrada)
 *               ─► CINEMATIC  reproduce las cinemáticas en cadena; cada una avisa desde su setOnFinished
 *               ─► INTERFACE  SOLO entonces se crea la interfaz del modo, DEBAJO del último cuadro congelado,
 *                             y la cinemática se disuelve revelándola
 *   deactivate()─► RESTORING ─► IDLE   corta todo, libera recursos y devuelve la UI base
 * </pre>
 * Cada subclase solo declara QUÉ cinemáticas, QUÉ interfaz y QUÉ sonidos; nunca el CUÁNDO.
 */
abstract class BaseMode implements SecretMode {

    protected SecretContext ctx;
    private boolean active;
    private final List<Cinematic> cinematics = new ArrayList<>();
    private TakeoverView ui;

    // ------------------------------------------------------------------ lo que declara cada modo

    /** Cinemáticas de entrada, en orden. Se crean de forma perezosa (Supplier) justo antes de reproducirse. */
    protected abstract List<Supplier<Cinematic>> cinematics();

    /** Interfaz rediseñada del modo. Solo se invoca tras el setOnFinished de la última cinemática. */
    protected abstract TakeoverView createInterface(Runnable exit);

    protected SoundEffect entrySound() { return null; }
    protected MusicTrack cinematicMusic() { return null; }
    protected MusicTrack interfaceMusic() { return null; }

    // ------------------------------------------------------------------ flujo (final: el orden no se negocia)

    @Override public final boolean isActive() { return active; }

    @Override public final void activate(SecretContext c) {
        this.ctx = c;
        if (!ctx.fsm().transition(State.TAKEOVER)) return;
        active = true;

        // 1) TAKEOVER: bloqueo + ocultación inmediatos (sin fundido: la cinemática cubre el corte en el mismo frame)
        ctx.takeover();
        if (entrySound() != null) ctx.audio().play(entrySound());
        if (cinematicMusic() != null) ctx.audio().playMusic(cinematicMusic());
        else ctx.audio().stopMusic(0);                          // el tema de la CALLE no se encima con la cinemática

        // 2) CINEMATIC
        ctx.fsm().transition(State.CINEMATIC);
        playNext(cinematics().iterator(), null);
    }

    /**
     * Encadena las cinemáticas. Cuando la anterior termina (su setOnFinished), la siguiente se monta DEBAJO de su
     * último cuadro, arranca, y la anterior se disuelve encima: el espectador nunca ve un hueco ni dos escenas peleando.
     */
    private void playNext(Iterator<Supplier<Cinematic>> it, Cinematic previous) {
        if (!active || !ctx.fsm().is(State.CINEMATIC)) return;  // salieron a la calle a mitad: callback tardío ignorado
        if (!it.hasNext()) { showInterface(previous); return; }

        Cinematic next = it.next().get();
        cinematics.add(next);
        app.animations.RenderTuning.cacheEffects(next.node());   // efectos a caché de GPU (60 FPS en GPUs integradas)
        ctx.mountBelow(next.node(), previous == null ? null : previous.node());
        next.play(once(() -> playNext(it, next)));
        if (previous != null) previous.dissolve();
        next.node().requestFocus();                              // ESC / ESPACIO van a la cinemática en curso
    }

    /** 3) INTERFACE: se crea aquí y no antes; la última cinemática se disuelve encima revelándola. */
    private void showInterface(Cinematic last) {
        if (!ctx.fsm().transition(State.INTERFACE)) return;
        ui = createInterface(this::deactivate);
        app.animations.RenderTuning.cacheEffects(ui);
        ctx.mountBelow(ui, last == null ? null : last.node());
        if (interfaceMusic() != null) ctx.audio().playMusic(interfaceMusic());
        ui.onShown();
        if (last != null) last.dissolve();
        ui.requestFocus();
    }

    /** 4) RESTORING → IDLE. Idempotente: se puede llamar desde "CALLE" (el Mordisco) o ante un error. */
    @Override public final void deactivate() {
        if (!active) return;
        active = false;
        ctx.fsm().transition(State.RESTORING);
        for (Cinematic c : cinematics) {
            try { c.stop(); } catch (RuntimeException ignored) { /* seguir limpiando */ }
        }
        cinematics.clear();
        if (ui != null) { ui.dispose(); ctx.layer().getChildren().remove(ui); ui = null; }
        ctx.audio().stopMusic(0);                               // stop() limpio: la próxima pista nunca se encima
        ctx.restore();
        ctx.fsm().transition(State.IDLE);
    }

    /** Envuelve un callback para que solo pueda ejecutarse una vez (skip + setOnFinished no lo duplican). */
    private static Runnable once(Runnable r) {
        boolean[] done = {false};
        return () -> { if (!done[0]) { done[0] = true; r.run(); } };
    }
}
