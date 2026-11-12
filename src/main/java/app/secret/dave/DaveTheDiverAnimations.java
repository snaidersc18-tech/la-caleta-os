package app.secret.dave;

import app.secret.Cinematic;
import app.secret.audio.AudioManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.function.Function;

/**
 * Fábrica de las tres cinemáticas estilo "Dave the Diver". Cada una se entrega como {@link Playback} (un
 * {@link Cinematic}) SIN montar ni reproducir: quien la orquesta decide dónde va y cuándo arranca.
 * <pre>
 *   Playback p = DaveTheDiverAnimations.sofia(audio);
 *   layer.getChildren().add(p.node());
 *   p.play(() -> { ...montar la interfaz debajo...; p.dissolve(); });
 * </pre>
 * Clic o ESC saltan al final. La escena queda congelada en su último cuadro hasta {@link Playback#dissolve()}.
 */
public final class DaveTheDiverAnimations {
    private DaveTheDiverAnimations() {}

    public static Playback daftPunk(AudioManager audio) { return new Playback(DaftPunkCinematic::build, audio); }
    public static Playback badBunny(AudioManager audio) { return new Playback(BadBunnyCinematic::build, audio); }
    public static Playback sofia(AudioManager audio) { return new Playback(SofiaCinematic::build, audio); }

    /** Mando de la reproducción. */
    public static final class Playback implements Cinematic {
        private final CineStage stage;
        private final Function<CineStage, Animation> builder;
        private Animation master;
        private Runnable onFinished;
        private boolean finished, disposed;

        Playback(Function<CineStage, Animation> builder, AudioManager audio) {
            this.builder = builder;
            this.stage = new CineStage(audio);
        }

        public CineStage stage() { return stage; }
        @Override public Node node() { return stage; }

        @Override public void play(Runnable onFinished) {
            if (master != null) return;
            this.onFinished = onFinished;
            // El guion se construye aquí (no en el constructor) para que los nodos nazcan ya dentro de la escena y
            // los Timeline indefinidos (estrellas, partículas) no corran mientras la cinemática no se ve.
            master = builder.apply(stage);
            master.setOnFinished(e -> finish());
            stage.setOnMouseClicked(e -> skip());
            stage.setFocusTraversable(true);
            stage.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) skip(); });
            stage.requestFocus();
            if (stage.syncTrack() == null || stage.audio() == null) { master.play(); return; }
            // Rhythm sync: la canción arranca justo antes de su drop y el guion se ancla al audio real
            double songStart = Math.max(0, stage.syncTrack().dropMs - stage.syncTimelineMs());
            stage.audio().playMusic(stage.syncTrack(), songStart);
            stage.audio().whenMusicPlaying(pos -> {
                if (finished || disposed) return;
                if (pos < 0) { master.play(); return; }              // sin audio: el guion corre igual
                master.playFrom(Duration.millis(Math.max(0, pos - songStart)));
                follow(songStart);
            });
        }

        private javafx.animation.AnimationTimer follower;

        /**
         * Seguidor: cada frame compara el guion con la canción (currentTime − inicio) y corrige con la VELOCIDAD del
         * guion, proporcional al error (±50 % como máximo): el MediaPlayer reporta la posición del seek un instante antes
         * de que el sonido arranque y los frames perdidos desfasan el guion; así ninguna de las dos cosas se acumula.
         * Nunca se usa jumpTo: saltar hacia atrás repetiría cues (nodos duplicados) y hacia adelante se los saltaría.
         */
        private void follow(double songStart) {
            follower = new javafx.animation.AnimationTimer() {
                @Override public void handle(long now) {
                    double pos = stage.audio().musicPositionMs();
                    if (finished || disposed || master.getStatus() != Animation.Status.RUNNING) { master.setRate(1); stop(); return; }
                    if (pos < 0) return;
                    double err = master.getCurrentTime().toMillis() - (pos - songStart);   // > 0: el guion va adelantado
                    master.setRate(Math.abs(err) < 6 ? 1 : 1 - Math.max(-0.5, Math.min(0.5, err / 200)));
                }
            };
            follower.start();
        }

        /** Instante (ms) en que arranca {@code child} dentro de la secuencia {@code seq} (suma de los anteriores). */
        public static double offsetOf(javafx.animation.SequentialTransition seq, Animation child) {
            double ms = 0;
            for (Animation a : seq.getChildren()) {
                if (a == child) return ms;
                ms += a.getTotalDuration().toMillis();
            }
            throw new IllegalArgumentException("la animación no está en la secuencia");
        }

        /** Salta al final: detiene la secuencia y entrega el control. */
        public void skip() { if (master != null) master.stop(); finish(); }

        private void finish() {
            if (finished) return;
            finished = true;
            stage.setOnMouseClicked(null);
            stage.setOnKeyPressed(null);
            if (onFinished != null) onFinished.run();
        }

        /** Desvanece el escenario (revela lo de debajo) y libera animaciones y partículas. */
        @Override public void dissolve() {
            if (disposed) return;
            if (master != null) master.stop();
            stage.setMouseTransparent(true);
            FadeTransition f = new FadeTransition(Duration.millis(650), stage);
            f.setToValue(0);
            f.setOnFinished(e -> dispose());
            f.play();
        }

        /** Corte inmediato. */
        @Override public void stop() { if (master != null) master.stop(); dispose(); }

        private void dispose() {
            if (disposed) return;
            disposed = true;
            if (follower != null) follower.stop();
            stage.dispose();
            if (stage.getParent() instanceof Pane p) p.getChildren().remove(stage);
        }
    }
}
