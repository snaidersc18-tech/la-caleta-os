package app.ui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/** Utilidades compartidas por las vistas de formulario. */
final class Forms {

    static final int MIN_PASS = 8;

    private Forms() {}

    static Label label(String t) {
        Label l = new Label(t);
        l.setMinWidth(Region.USE_PREF_SIZE);
        l.setPrefWidth(160);
        return l;
    }

    static HBox row(String label, Node... nodes) {
        HBox r = new HBox(8);
        r.setAlignment(Pos.CENTER_LEFT);
        r.getChildren().add(label(label));
        for (Node n : nodes) {
            r.getChildren().add(n);
            HBox.setHgrow(n, n instanceof TextField ? Priority.ALWAYS : Priority.NEVER);
        }
        return r;
    }

    static Region rule() {
        Region r = new Region();
        r.setMinHeight(2);
        r.setPrefHeight(2);
        r.setMaxHeight(2);
        r.getStyleClass().add("ar-rule");
        return r;
    }

    static Label heading(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("ar-heading");
        return l;
    }

    static GlitchButton squareButton(String t) {
        GlitchButton b = new GlitchButton(t);
        b.getStyleClass().add("ar-square");
        b.setMinWidth(40);
        return b;
    }

    static File pickFile(Window w, String title, boolean imagesOnly) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        if (imagesOnly) {
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Imágenes", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        }
        return fc.showOpenDialog(w);
    }

    static File pickDir(Window w, String title) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle(title);
        return dc.showDialog(w);
    }

    static String message(Throwable ex) {
        return ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    static Insets pad(double v) { return new Insets(v); }

    /** Trabajo en segundo plano con progreso y cancelación cooperativa. */
    interface Job<T> {
        T run(DoubleConsumer progress, Consumer<String> log) throws Exception;
    }

    /**
     * Lanza el trabajo en un hilo daemon. Mientras corre: oculta {@code idle}, muestra {@code busy}
     * (barra de progreso) y habilita CANCELAR. La cancelación se comprueba en cada actualización de
     * progreso, es decir, antes de escribir nada en disco.
     */
    static <T> Task<T> launch(Job<T> job, ConsolePanel console, ProgressBar bar, Node idle, Node busy,
                              Button cancel, Consumer<T> onOk, Consumer<Throwable> onErr) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception {
                return job.run(p -> {
                    if (isCancelled()) throw new CancellationException("cancelado");
                    updateProgress(p, 1.0);
                }, console::log);
            }
        };
        bar.progressProperty().bind(task.progressProperty());
        idle.setVisible(false);
        busy.setVisible(true);
        cancel.setDisable(false);
        Runnable reset = () -> {
            bar.progressProperty().unbind();
            bar.setProgress(0);
            busy.setVisible(false);
            idle.setVisible(true);
            cancel.setDisable(true);
        };
        task.setOnSucceeded(e -> { reset.run(); onOk.accept(task.getValue()); });
        task.setOnFailed(e -> { reset.run(); onErr.accept(task.getException()); });
        task.setOnCancelled(e -> { reset.run(); console.println(">>> Operación cancelada. No se escribió nada."); });
        cancel.setOnAction(e -> task.cancel(true));
        Thread th = new Thread(task, "arana-worker");
        th.setDaemon(true);
        th.start();
        return task;
    }
}
