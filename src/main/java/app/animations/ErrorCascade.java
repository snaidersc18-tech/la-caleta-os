package app.animations;

import app.narrative.ErrorMessages;
import app.narrative.ErrorMessages.Msg;
import app.ui.PixelFonts;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleConsumer;

/**
 * "Cascada de errores" del Payload Picaku.
 *
 * Fase A (aceleración): ráfagas de ventanitas de error cada vez más seguidas
 *   (intervalo = 460 ms * 0.84^n, mínimo 40 ms) y cada vez más por ráfaga (1, 2, 3).
 *   Cada una "salta" con ScaleTransition y sacude la pantalla; el temblor crece con el caos.
 * Fase B (colapso visual): todas tiemblan con jitter ±3 px mientras la pantalla vibra fuerte.
 * Fase C (desaparición): se cierran en orden aleatorio, encogiéndose y desvaneciéndose,
 *   y un flash blanco cierra la secuencia.
 */
public final class ErrorCascade {

    private static final String RED = "#ff2a2a";
    private static final String GREEN = "#39ff14";
    private static final String BONE = "#efece1";

    private final Pane layer;
    private final double w, h;
    private final DoubleConsumer shake;
    private final Runnable flash;
    private final Random rnd = new Random();
    private final List<Node> windows = new ArrayList<>();
    private Timeline jitter;

    public ErrorCascade(Pane layer, double w, double h, DoubleConsumer shake, Runnable flash) {
        this.layer = layer; this.w = w; this.h = h; this.shake = shake; this.flash = flash;
    }

    public void play(Runnable onFinished) {
        List<Msg> msgs = ErrorMessages.shuffled();
        Timeline master = new Timeline();
        double t = 0;
        int cursor = 0;
        int ticks = 14;

        for (int i = 0; i < ticks; i++) {
            t += Math.max(40, 460 * Math.pow(0.84, i));
            int burst = i < 6 ? 1 : (i < 10 ? 2 : 3);
            final int tickIndex = i;
            final List<Msg> chunk = new ArrayList<>();
            for (int b = 0; b < burst; b++) chunk.add(msgs.get(cursor++ % msgs.size()));
            master.getKeyFrames().add(new KeyFrame(Duration.millis(t), e -> {
                for (Msg m : chunk) spawn(m);
                shake.accept(2.0 + tickIndex * 0.55);
                if (tickIndex % 5 == 4) flash.run();
            }));
        }

        // Fase B: jitter general + temblor fuerte
        final double holdStart = t + 80;
        master.getKeyFrames().add(new KeyFrame(Duration.millis(holdStart), e -> {
            startJitter();
            shake.accept(9);
            flash.run();
        }));
        master.getKeyFrames().add(new KeyFrame(Duration.millis(holdStart + 450), e -> shake.accept(7)));

        // Fase C: cierre escalonado en orden aleatorio
        final double collapseStart = holdStart + 950;
        master.getKeyFrames().add(new KeyFrame(Duration.millis(collapseStart), e -> collapse(onFinished)));

        master.play();
    }

    // ------------------------------------------------------------------ ventanas

    private void spawn(Msg m) {
        Node win = buildWindow(m);
        win.relocate(rnd.nextDouble() * (w - 330), 20 + rnd.nextDouble() * (h - 170));
        win.setScaleX(0.55);
        win.setScaleY(0.55);
        windows.add(win);
        layer.getChildren().add(win);
        ScaleTransition pop = new ScaleTransition(Duration.millis(130), win);
        pop.setToX(1); pop.setToY(1);
        pop.setInterpolator(Interpolator.EASE_OUT);
        pop.play();
    }

    private Node buildWindow(Msg m) {
        Label title = new Label("⚠ " + m.title() + "  0x" + String.format("%04X", rnd.nextInt(0xFFFF)));
        title.setStyle("-fx-text-fill: #050505; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-font-size: 13px;");
        Label close = new Label("X");
        close.setStyle("-fx-text-fill: #050505; -fx-font-weight: bold; -fx-padding: 3 8 3 8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(title, spacer, close);
        bar.setStyle("-fx-background-color: " + RED + ";");

        Label body = new Label(m.body());
        body.setWrapText(true);
        body.setMaxWidth(292);
        body.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 15px;");
        body.setFont(PixelFonts.font(15, false));

        Label btn = new Label("[ " + m.button() + " ]");
        btn.setStyle("-fx-text-fill: " + BONE + "; -fx-border-color: " + BONE
                + "; -fx-border-width: 1; -fx-padding: 3 10 3 10; -fx-font-size: 12px;");
        HBox btnRow = new HBox(btn);
        btnRow.setPadding(new Insets(0, 14, 12, 14));

        VBox box = new VBox(bar, wrap(body), btnRow);
        box.setPrefWidth(320); box.setMinWidth(320); box.setMaxWidth(320);
        box.setStyle("-fx-background-color: #070707; -fx-border-color: " + RED + "; -fx-border-width: 2;");
        box.setEffect(new DropShadow(14, Color.web(RED, 0.65)));
        box.setMouseTransparent(true);
        return box;
    }

    private static Node wrap(Label body) {
        HBox box = new HBox(body);
        box.setPadding(new Insets(14, 14, 10, 14));
        return box;
    }

    private void startJitter() {
        jitter = new Timeline(new KeyFrame(Duration.millis(45), e -> {
            for (Node n : windows) {
                n.setTranslateX((rnd.nextDouble() * 2 - 1) * 3.5);
                n.setTranslateY((rnd.nextDouble() * 2 - 1) * 3.5);
            }
        }));
        jitter.setCycleCount(Animation.INDEFINITE);
        jitter.play();
    }

    private void collapse(Runnable onFinished) {
        List<Node> order = new ArrayList<>(windows);
        Collections.shuffle(order, rnd);
        Timeline closing = new Timeline();
        int step = 26;
        for (int i = 0; i < order.size(); i++) {
            Node n = order.get(i);
            closing.getKeyFrames().add(new KeyFrame(Duration.millis(i * step), e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(150), n);
                st.setToX(0.04); st.setToY(0.04);
                FadeTransition ft = new FadeTransition(Duration.millis(150), n);
                ft.setToValue(0);
                ParallelTransition pt = new ParallelTransition(st, ft);
                pt.setOnFinished(ev -> layer.getChildren().remove(n));
                pt.play();
            }));
        }
        double end = order.size() * step + 220;
        closing.getKeyFrames().add(new KeyFrame(Duration.millis(end), e -> {
            if (jitter != null) jitter.stop();
            windows.clear();
            layer.getChildren().clear();
            flash.run();
            onFinished.run();
        }));
        closing.play();
    }
}
