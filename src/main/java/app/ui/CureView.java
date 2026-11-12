package app.ui;

import app.crypto.Payload;
import app.narrative.StreetLogs;
import app.stega.StegoVault;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** [1] CURAR ARCHIVO (única operación del Menú CALLE): lee los bits ocultos, descifra y restaura el archivo original. */
final class CureView extends ChamferPane {

    private final ConsolePanel console;
    private final StegoVault vault = new StegoVault();

    private final TextField img = new TextField();
    private final PasswordField pass = new PasswordField();
    private final TextField outDir = new TextField();
    private final GlitchButton run = new GlitchButton("[ EXTRAER: SOBREVIVIENDO A LA FUNA ]");
    private final GlitchButton cancel = new GlitchButton("CANCELAR");
    private final GlitchButton clear = new GlitchButton("LIMPIAR");
    private final ProgressBar bar = new ProgressBar(0);
    private final HBox busy = new HBox(bar);

    CureView(ConsolePanel console) {
        super(14, false, false, true, false);
        this.console = console;

        img.setPromptText("imagen camuflada (.png) con carga oculta...");
        pass.setPromptText("Enter");
        outDir.setPromptText("(vacío = carpeta de la imagen)");

        GlitchButton imgBtn = Forms.squareButton("...");
        GlitchButton dirBtn = Forms.squareButton(">");
        imgBtn.setOnAction(e -> {
            File f = Forms.pickFile(getScene().getWindow(), "Imagen camuflada", true);
            if (f != null) img.setText(f.getAbsolutePath());
        });
        dirBtn.setOnAction(e -> {
            File f = Forms.pickDir(getScene().getWindow(), "Carpeta de salida");
            if (f != null) outDir.setText(f.getAbsolutePath());
        });

        run.getStyleClass().add("ar-primary");
        run.setMaxWidth(Double.MAX_VALUE);
        run.setOnAction(e -> start());

        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        busy.setVisible(false);
        busy.setAlignment(Pos.CENTER);
        StackPane action = new StackPane(run, busy);

        cancel.setDisable(true);
        clear.setOnAction(e -> { img.clear(); pass.clear(); outDir.clear(); });
        HBox bottom = new HBox(10, new Label(), cancel, clear);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(bottom.getChildren().get(0), Priority.ALWAYS);

        VBox inner = new VBox(12,
                Forms.row("Imagen Camuflada", img, imgBtn),
                Forms.row("Contraseña", pass),
                Forms.row("Carpeta de Salida", outDir, dirBtn),
                Forms.rule(),
                action,
                bottom);
        inner.getStyleClass().add("ar-group");
        inner.setPadding(new Insets(14));

        Label sub = new Label("Antivirus de barrio: devuélvele la cara al secreto. Sin juicio y sin funa.");
        sub.getStyleClass().add("ar-dim");
        VBox root = new VBox(10, Forms.heading("// ANTIVIRUS DE BARRIO: CURAR ARCHIVO"), sub, inner);
        root.setPadding(new Insets(16));
        getChildren().add(root);
    }

    private void start() {
        final Path image;
        try {
            image = Path.of(img.getText().trim());
        } catch (InvalidPathException ex) {
            console.println(">>> ERROR: ruta inválida.");
            return;
        }
        if (img.getText().isBlank() || !Files.isRegularFile(image)) {
            console.println(">>> ERROR: la imagen no existe.");
            return;
        }
        final String pw = pass.getText();
        if (pw.isEmpty()) {
            console.println(">>> ERROR: falta la contraseña.");
            return;
        }
        final Path dest = outDir.getText().isBlank() ? Settings.get().outDirFor(image) : Path.of(outDir.getText().trim());

        console.println(StreetLogs.pick(StreetLogs.EXTRACT_START));
        Forms.launch(
                (progress, log) -> {
                    Payload p = vault.open(image, pw.toCharArray(), log, progress);
                    return vault.writeFile(p, dest);
                },
                console, bar, run, busy, cancel,
                out -> {
                    console.println(">>> Archivo restaurado en: " + out);
                    console.println(StreetLogs.pick(StreetLogs.EXTRACT_DONE));
                    pass.clear();
                },
                ex -> {
                    String m = Forms.message(ex);
                    console.println(">>> ERROR: " + m);
                    if (m.startsWith("Contraseña incorrecta")) console.println(StreetLogs.pick(StreetLogs.WRONG_PASSWORD));
                });
    }
}
