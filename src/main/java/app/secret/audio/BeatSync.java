package app.secret.audio;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Engancha animaciones cíclicas (TranslateTransition, Timeline...) al pulso REAL de la canción.
 * <p>
 * Una animación de JavaFX corre con el reloj de pulsos de la escena y la música con el reloj de la tarjeta de sonido:
 * aunque la duración del ciclo sea exactamente {@code n · beat}, en un minuto se separan decenas de ms (y un frame
 * perdido las desfasa de golpe). Este {@link AnimationTimer} lee {@link AudioManager#beats()} en cada frame, calcula
 * en qué punto de su ciclo DEBERÍA ir cada animación y, si se desvía más de {@value #TOLERANCE_MS} ms, la recoloca con
 * {@code jumpTo}. Si no hay música (o aún no arranca) no toca nada: las animaciones siguen a su ritmo nominal.
 * <p>
 * Requisito: la animación NO usa autoReverse (un ciclo = periodo completo); para "bajar y subir" en un ciclo se usa un
 * interpolador de ida y vuelta como {@link #NOD}.
 */
public final class BeatSync extends AnimationTimer {

    private static final double TOLERANCE_MS = 18;

    /** Cabeceo: baja rápido justo EN el golpe (20 % del beat) y vuelve suave arriba antes del siguiente. 0 → 1 → 0. */
    public static final javafx.animation.Interpolator NOD = new javafx.animation.Interpolator() {
        @Override protected double curve(double t) {
            if (t < 0.2) return Math.sin(t / 0.2 * Math.PI / 2);
            double u = (t - 0.2) / 0.8;
            return 1 - u * u * (3 - 2 * u);
        }
    };

    private record Lock(Animation a, double periodBeats, double offsetBeats) { }

    private final AudioManager audio;
    private final List<Lock> locks = new ArrayList<>();

    public BeatSync(AudioManager audio) { this.audio = audio; }

    /**
     * La animación {@code a} (cycleCount INDEFINITE, sin autoReverse, ciclo = {@code periodBeats} beats) queda
     * enganchada de modo que su tiempo 0 coincide con los beats {@code offsetBeats + k·periodBeats}.
     */
    public BeatSync lock(Animation a, double periodBeats, double offsetBeats) {
        locks.add(new Lock(a, periodBeats, offsetBeats));
        return this;
    }

    @Override public void handle(long now) {
        double b = audio.beats(), beat = audio.beatMs();
        if (Double.isNaN(b) || Double.isNaN(beat)) return;
        for (Lock l : locks) {
            double periodMs = l.periodBeats * beat;
            double want = floorMod((b - l.offsetBeats) * beat, periodMs);
            Animation a = l.a;
            if (a.getStatus() != Animation.Status.RUNNING) { a.playFrom(Duration.millis(want)); continue; }
            double have = floorMod(a.getCurrentTime().toMillis(), periodMs);
            double err = Math.abs(have - want);
            err = Math.min(err, periodMs - err);                      // distancia circular dentro del ciclo
            if (err > TOLERANCE_MS) a.jumpTo(Duration.millis(want));
        }
    }

    private static double floorMod(double x, double m) { return ((x % m) + m) % m; }
}
