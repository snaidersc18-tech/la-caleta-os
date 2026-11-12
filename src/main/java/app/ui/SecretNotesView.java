package app.ui;

import app.crypto.Payload;
import app.stega.StegoVault;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entorno del estado 2: notas secretas de texto puro. Mismo motor real
 * (AES-256-GCM + LSB) pero con el camuflaje generado en tonos neón.
 */
final class SecretNotesView extends ChamferPane {

    enum Mode { WRITE, OPEN }

    private final ConsolePanel console;
    private final StegoVault vault = new StegoVault();

    private final Label heading = Forms.heading("");
    private final TextArea note = new TextArea();
    private final PasswordField pass = new PasswordField();
    private final TextField file = new TextField();
    private final GlitchButton pick = Forms.squareButton("...");
    private final GlitchButton run = new GlitchButton("");
    private final GlitchButton cancel = new GlitchButton("CANCELAR");
    private final ProgressBar bar = new ProgressBar(0);
    private final HBox busy = new HBox(bar);
    private final HBox fileRow = Forms.row("Nota sellada", file, pick);
    private Mode mode = Mode.WRITE;

    SecretNotesView(ConsolePanel console) {
        super(14, false, false, true, false);
        this.console = console;

        note.setWrapText(true);
        note.setPrefRowCount(6);
        VBox.setVgrow(note, Priority.ALWAYS);
        pass.setPromptText("Enter");
        file.setPromptText("imagen .png con la nota...");
        pick.setOnAction(e -> {
            File f = Forms.pickFile(getScene().getWindow(), "Nota sellada (imagen)", true);
            if (f != null) file.setText(f.getAbsolutePath());
        });

        run.getStyleClass().add("ar-primary");
        run.setMaxWidth(Double.MAX_VALUE);
        run.setOnAction(e -> { if (mode == Mode.WRITE) seal(); else open(); });
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        busy.setVisible(false);
        busy.setAlignment(Pos.CENTER);
        StackPane action = new StackPane(run, busy);
        cancel.setDisable(true);
        HBox bottom = new HBox(cancel);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        VBox inner = new VBox(12, note, fileRow, Forms.row("Contraseña", pass), Forms.rule(), action, bottom);
        inner.getStyleClass().add("ar-group");
        inner.setPadding(new Insets(14));
        VBox.setVgrow(inner, Priority.ALWAYS);

        VBox root = new VBox(10, heading, inner);
        root.setPadding(new Insets(16));
        getChildren().add(root);
        setMode(Mode.WRITE);
    }

    void setMode(Mode m) {
        this.mode = m;
        boolean write = m == Mode.WRITE;
        heading.setText(write ? "// FASE_<3: NOTAS SECRETAS" : "// FASE_<3: ABRIR NOTA SELLADA");
        note.setPromptText(write ? "Escríbele aquí. Nadie más lo va a leer..." : "Aquí aparecerá la nota descifrada.");
        note.setEditable(write);
        fileRow.setVisible(!write);
        fileRow.setManaged(!write);
        run.setBaseText(write ? "[ SELLAR NOTA EN EL CORAZÓN ]" : "[ ABRIR NOTA SELLADA ]");
        if (write) note.clear();
    }

    private void seal() {
        String text = note.getText();
        if (text.isBlank()) { console.println(">>> La nota está vacía. Hasta el silencio dice algo, pero escribe."); return; }
        String pw = pass.getText();
        if (pw.length() < Forms.MIN_PASS) {
            console.println(">>> Clave floja (mínimo 8 caracteres). Para ella, mínimo una que valga la pena.");
            return;
        }
        Path out = StegoVault.uniquePath(Settings.get().notesDir(),
                "nota_picaku_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")), ".png");
        console.println(">>> Sellando nota: AES-256-GCM + bits escondidos en un skyline de neón...");
        Forms.launch(
                (progress, log) -> vault.sealNote(text, pw.toCharArray(), out, true, log, progress),
                console, bar, run, busy, cancel,
                path -> {
                    console.println(">>> Nota guardada en: " + path);
                    console.println(">>> Sellada. Hasta el algoritmo se puso a suspirar.");
                    note.clear();
                    pass.clear();
                },
                ex -> console.println(">>> ERROR: " + Forms.message(ex)));
    }

    private void open() {
        final Path img;
        try {
            img = Path.of(file.getText().trim());
        } catch (InvalidPathException ex) {
            console.println(">>> ERROR: ruta inválida.");
            return;
        }
        if (file.getText().isBlank() || !Files.isRegularFile(img)) {
            console.println(">>> ERROR: elige la imagen con la nota.");
            return;
        }
        String pw = pass.getText();
        if (pw.isEmpty()) { console.println(">>> ERROR: falta la contraseña."); return; }
        Forms.launch(
                (progress, log) -> vault.open(img, pw.toCharArray(), log, progress),
                console, bar, run, busy, cancel,
                (Payload p) -> {
                    if (p.kind() == Payload.Kind.NOTE) {
                        note.setText(p.noteText());
                        console.println(">>> Nota abierta. Léela despacio.");
                    } else {
                        console.println(">>> Esa imagen guarda un archivo, no una nota. Usa [2] en modo calle.");
                    }
                    pass.clear();
                },
                ex -> console.println(">>> ERROR: " + Forms.message(ex)));
    }
}
