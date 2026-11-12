package app.secret;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados ESTRICTA del UI Takeover. Es la pieza que corrige el bug de las dos interfaces solapadas:
 * solo existe un flujo a la vez y cada fase solo puede avanzar a las fases permitidas.
 * <pre>
 *   IDLE ──► TAKEOVER ──► CINEMATIC ──► INTERFACE ──► RESTORING ──► IDLE
 *                │             │                          ▲
 *                └─────────────┴──────── (CALLE / error) ─┘
 * </pre>
 * Cualquier transición que no esté en la tabla se rechaza (devuelve false) y no cambia nada; así un segundo
 * disparador, un setOnFinished tardío o un doble clic en "salir" no pueden montar otra vista encima.
 */
public final class TakeoverStateMachine {

    public enum State { IDLE, TAKEOVER, CINEMATIC, INTERFACE, RESTORING }

    private static final Map<State, Set<State>> LEGAL = new EnumMap<>(State.class);
    static {
        LEGAL.put(State.IDLE, EnumSet.of(State.TAKEOVER));
        LEGAL.put(State.TAKEOVER, EnumSet.of(State.CINEMATIC, State.RESTORING));
        LEGAL.put(State.CINEMATIC, EnumSet.of(State.INTERFACE, State.RESTORING));
        LEGAL.put(State.INTERFACE, EnumSet.of(State.RESTORING));
        LEGAL.put(State.RESTORING, EnumSet.of(State.IDLE));
    }

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.IDLE);

    /**
     * Intenta pasar a {@code to}. Solo desde el hilo de JavaFX: todas las fases tocan nodos de la escena, y serializar
     * las transiciones en ese único hilo es lo que hace innecesario cualquier lock.
     */
    public boolean transition(State to) {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("Transición fuera del hilo de JavaFX: " + to);
        if (!LEGAL.get(state.get()).contains(to)) return false;
        state.set(to);
        return true;
    }

    public State state() { return state.get(); }
    public boolean is(State s) { return state.get() == s; }
    public boolean isIdle() { return state.get() == State.IDLE; }
}
