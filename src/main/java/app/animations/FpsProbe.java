package app.animations;

import javafx.animation.AnimationTimer;

/**
 * Diagnóstico de fluidez para TODA la app ({@code -Drebusque.fps=true}): un AnimationTimer cuenta los pulsos reales de
 * JavaFX y cada segundo imprime en stderr los FPS, el peor frame y una etiqueta de la escena actual. Así cada
 * optimización se valida con números y no a ojo.
 */
public final class FpsProbe {

    private static volatile String scene = "arranque";

    private FpsProbe() {}

    /** Etiqueta la escena en curso (intro, menu, daftpunk, calle, salir...). Sin efecto si la sonda está apagada. */
    public static void scene(String name) { scene = name; }

    /** Arranca la sonda si la propiedad del sistema está activa. Llamar una vez desde el hilo de JavaFX. */
    public static void startIfEnabled() {
        if (!Boolean.getBoolean("rebusque.fps")) return;
        long t0 = System.nanoTime();
        new AnimationTimer() {
            private long sec, frames, worst, prev;
            @Override public void handle(long now) {
                if (sec == 0) sec = now;
                if (prev != 0) worst = Math.max(worst, now - prev);
                prev = now;
                frames++;
                if (now - sec >= 1_000_000_000L) {
                    System.err.printf("[fps] t=%5.1fs  %-9s %3d fps  peor frame %6.1f ms%n",
                            (now - t0) / 1e9, scene, frames, worst / 1e6);
                    sec = now; frames = 0; worst = 0;
                }
            }
        }.start();
    }
}
