package app.ui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Path;

/** [3] CONFIGURACIÓN: efectos visuales y carpeta de salida. */
final class ConfigView extends ChamferPane {

    ConfigView() {
        super(14, false, false, true, false);
        Settings s = Settings.get();

        CheckBox crt = new CheckBox("Efecto CRT (scanlines, viñeta, parpadeo)");
        crt.selectedProperty().bindBidirectional(s.crtProperty());
        CheckBox glitch = new CheckBox("Glitch en botones y tiras de interferencia");
        glitch.selectedProperty().bindBidirectional(s.glitchProperty());

        TextField dir = new TextField();
        dir.setEditable(false);
        dir.setPromptText("(misma carpeta del archivo de origen)");
        s.outputDirProperty().addListener((o, a, b) -> dir.setText(b == null ? "" : b.toString()));
        GlitchButton pick = Forms.squareButton(">");
        pick.setOnAction(e -> {
            File f = Forms.pickDir(getScene().getWindow(), "Carpeta de salida por defecto");
            if (f != null) s.outputDirProperty().set(Path.of(f.getAbsolutePath()));
        });
        GlitchButton reset = new GlitchButton("RESET");
        reset.setOnAction(e -> s.outputDirProperty().set(null));

        Label info = new Label("""
                Cifrado ........ AES-256-GCM (autenticado)
                Llave .......... PBKDF2-HMAC-SHA256 x 210.000
                Ocultamiento ... LSB, 1 bit por canal RGB
                Salida ......... siempre PNG sin pérdida""");
        info.getStyleClass().add("ar-dim");

        VBox inner = new VBox(14, crt, glitch, Forms.rule(), Forms.row("Carpeta de salida", dir, pick, reset),
                Forms.rule(), info);
        inner.getStyleClass().add("ar-group");
        inner.setPadding(new Insets(14));

        VBox root = new VBox(10, Forms.heading("// CONFIGURACIÓN"), inner);
        root.setPadding(new Insets(16));
        getChildren().add(root);
    }
}
