package app.secret.view;

import app.secret.SecretContext;
import app.secret.audio.BeatSync;
import app.secret.audio.MusicTrack;
import app.secret.audio.SoundEffect;
import app.secret.service.SecureWiper;
import app.secret.ui.ConchoSprite;
import app.secret.ui.Fx;
import app.secret.ui.LogBox;
import app.secret.ui.MascotGroove;
import app.secret.ui.PixelArt;
import app.secret.ui.PixelBackdrop;
import app.stega.StegoVault;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * UI tropical lo-fi del modo "Un Verano Sin Cifrar". Botones: "Foto a Encaletar", "Clave del Perreo" y el botón rojo
 * gigante "Safaera (Destruir Original)", que SELLA (AES-256-GCM + LSB, verificando el PNG) y solo después sobrescribe
 * el original con ceros y lo elimina. Pide doble confirmación. Concho celebra con baile, bocadillo y el flash de 200 ms.
 */
public final class VeranoTropicalView extends TakeoverView {

    private static final String[] MESSAGES = {
            "¡Ay bendito, encriptao!", "¡Eso va seguro, pa!", "¡Safaera completa, mi loco!", "¡Wepa! ¡Selladito!",
            "¡Ni el Estado lo abre, mi pana!", "¡Qué rico cifrar, brother!", "¡Candao como bóveda!", "¡Dímelo, Concho!"};
    private static final int[] DANCE = {4, 5, 6, 5, 4, 5, 6, 0};

    private final SecretContext ctx;
    private final StegoVault vault = new StegoVault();
    private final SecureWiper wiper = new SecureWiper();
    private final Random rnd = new Random();

    private final Pane root = new Pane();
    private final LogBox log = new LogBox("#ffe3b0", "#2a1020", 740, 160);
    private final PasswordField pass = new PasswordField();
    private final Label fileLabel = Fx.label("—", 14, "#ffe3b0");
    private final Button safaera = new Button("SAFAERA (DESTRUIR ORIGINAL)");
    private final Canvas concho = new Canvas(ConchoSprite.W * 5, ConchoSprite.H * 5);
    private final Label bubble = Fx.label("", 15, "#2a1020");
    private final Rectangle flash = new Rectangle(W, H, Color.WHITE);
    private final MascotView mascot = new MascotView(MascotView.Skin.TROPICAL_HEART, 340, 300);

    private final GuideHud guide;
    private Path source;
    private boolean armed, busy;
    private Timeline dance;
    private PauseTransition disarm;

    public VeranoTropicalView(SecretContext ctx, Runnable onExit) {
        this.ctx = ctx;
        root.setPrefSize(W, H);

        PixelBackdrop beach = PixelBackdrop.beach(160, 90, 8);
        beach.setOpacity(0.55);
        beach.start();
        onDispose(beach::stop);
        Rectangle veil = new Rectangle(W, H);
        veil.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3a1c4a", 0.55)), new Stop(1, Color.web("#7a2a2a", 0.70))));
        root.getChildren().addAll(beach, veil);

        Label title = Fx.label("UN VERANO SIN CIFRAR", 46, "#fff0c8");
        title.setEffect(new DropShadow(0, 4, 4, Color.web("#c94f6d")));
        title.relocate(420, 18);
        Label sub = Fx.label("borrado kamikaze · lo-fi · pa' que nadie te vea", 14, "#ffd2a0");
        sub.relocate(424, 80);
        root.getChildren().addAll(title, sub);

        buildCard();
        buildLeft();
        // Zero-UI: ya no hay botón de salida; solo la pista del comando (no es clicable)
        Label exit = Fx.label("> escribe CALLE para volver", 14, "#ffd2a0");
        exit.setOpacity(0.8);
        exit.setMouseTransparent(true);
        exit.relocate(424, 104);
        root.getChildren().add(exit);

        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        getChildren().addAll(root, flash);

        guide = new GuideHud("#ffb36b", "MÓDULO SAFAERA",
                "Encripta tu archivo y destruye el original sin dejar rastro: solo queda la versión cifrada en un PNG.",
                List.of("ELEGIR ARCHIVO: la foto o documento a proteger.",
                        "CLAVE DEL PERREO: contraseña de al menos 8 caracteres (guárdala: sin ella no hay vuelta).",
                        "SAFAERA, 1er clic: se ARMA (el botón pide confirmación durante 4 s).",
                        "SAFAERA, 2º clic: sella con AES-256-GCM, VERIFICA el PNG y solo entonces sobrescribe el original con ceros y lo borra."),
                "Borrado kamikaze: irreversible. En SSD/flash la sobrescritura no es 100 % garantizable (wear leveling).");
        guide.attachTo(this, Pos.TOP_LEFT, new Insets(14, 0, 0, 18));
        onDispose(guide::stop);
        onDispose(() -> { mascot.stop(); if (dance != null) dance.stop(); });

        log.log("Un verano sin cifrar: elige la foto, ponle la clave del perreo y destruye el original.");
        log.log("Ojo: SAFAERA borra el original de verdad, y solo después de verificar el sello.");
    }

    private void buildCard() {
        Rectangle card = new Rectangle(420, 118, 800, 566);
        card.setArcWidth(26); card.setArcHeight(26);
        card.setFill(Color.web("#2a1020", 0.84));
        card.setStroke(Color.web("#ffb36b"));
        card.setStrokeWidth(4);
        root.getChildren().add(card);

        Label l1 = Fx.label("FOTO A ENCALETAR", 15, "#ffb36b");
        l1.relocate(450, 140);
        Button pick = tropicalButton("ELEGIR ARCHIVO...", 16);
        pick.relocate(450, 168);
        pick.setOnAction(e -> pickFile());
        fileLabel.relocate(640, 174);

        Label l2 = Fx.label("CLAVE DEL PERREO (mín. 8)", 15, "#ffb36b");
        l2.relocate(450, 232);
        pass.setPromptText("••••••••");
        pass.setPrefWidth(740);
        pass.setStyle("-fx-background-color: #ffb36b, #1a0a14; -fx-background-insets: 0, 2; -fx-text-fill: #fff0c8;"
                + " -fx-font-family: Monospaced; -fx-font-size: 18; -fx-prompt-text-fill: #6b4a3a; -fx-padding: 8 12 8 12;");
        pass.relocate(450, 258);

        safaera.setPrefSize(740, 120);
        safaera.setWrapText(true);
        safaera.setStyle("-fx-background-color: linear-gradient(#ff3b30, #a0100a); -fx-text-fill: white; -fx-font-size: 28;"
                + " -fx-font-weight: bold; -fx-font-family: Monospaced; -fx-border-color: #ffe3b0; -fx-border-width: 4;"
                + " -fx-background-radius: 18; -fx-border-radius: 18; -fx-cursor: hand;");
        safaera.setEffect(new DropShadow(26, Color.web("#ff3b30")));
        safaera.relocate(450, 326);
        safaera.setOnAction(e -> onSafaera());

        log.relocate(450, 478);
        root.getChildren().addAll(l1, pick, fileLabel, l2, pass, safaera, log);
    }

    private void buildLeft() {
        mascot.relocate(40, 100);
        root.getChildren().add(mascot);
        // la mascota y Concho bailan EN el beat de la canción (108 BPM medidos), anclados al currentTime del audio
        double beat = MusicTrack.BAD_BUNNY_THEME.beatMs();
        MascotGroove groove = new MascotGroove(mascot, root, () -> new Point2D(210, 120), ctx.audio(), beat);
        groove.start();
        onDispose(groove::stop);

        concho.relocate(120, 410);
        root.getChildren().add(concho);
        drawConcho(0, false);
        TranslateTransition bob = track(new TranslateTransition(Duration.millis(2 * beat), concho));
        bob.setFromY(0); bob.setToY(-4); bob.setCycleCount(Animation.INDEFINITE);
        bob.setInterpolator(BeatSync.NOD);                                // cabecea cada 2 beats (media frase del dembow)
        bob.play();
        BeatSync sync = new BeatSync(ctx.audio()).lock(bob, 2, 0);
        sync.start();
        onDispose(sync::stop);

        bubble.setStyle("-fx-background-color: white; -fx-padding: 6 12 6 12; -fx-border-color: #2a1020; -fx-border-width: 3;"
                + " -fx-background-radius: 10; -fx-border-radius: 10;");
        bubble.relocate(30, 372);
        bubble.setOpacity(0);
        root.getChildren().add(bubble);
    }

    /** Entrada de la interfaz bajo el flash: la mascota muta al Corazón Triste tropical y Concho saluda. */
    @Override public void onShown() {
        guide.start();
        mascot.mutate();
        say("¡Wepa! ¡Bienvenido al verano!");
    }

    // ------------------------------------------------------------------ función real: Safaera

    private void pickFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Foto (o archivo) a encaletar");
        File f = fc.showOpenDialog(getScene().getWindow());
        if (f == null) return;
        source = f.toPath();
        fileLabel.setText(f.getName().length() > 34 ? f.getName().substring(0, 31) + "..." : f.getName());
        log.log("Cargado: " + f.getName() + " (" + f.length() / 1024 + " KB)");
    }

    private void onSafaera() {
        if (busy) return;
        if (source == null || !Files.isRegularFile(source)) { log.log("Falta la FOTO A ENCALETAR."); return; }
        if (pass.getText().length() < 8) { log.log("La clave del perreo necesita al menos 8 caracteres."); return; }
        if (!armed) {                                                 // primer clic: se arma
            armed = true;
            safaera.setText("¿SEGURO? OTRO CLIC = SIN VUELTA ATRÁS");
            disarm = track(new PauseTransition(Duration.seconds(4)));
            disarm.setOnFinished(e -> { armed = false; safaera.setText("SAFAERA (DESTRUIR ORIGINAL)"); });
            disarm.play();
            return;
        }
        if (disarm != null) disarm.stop();
        armed = false;
        safaera.setText("SAFAERA (DESTRUIR ORIGINAL)");
        busy = true;
        final Path src = source;
        final String pw = pass.getText();
        final Path outDir = app.ui.Settings.get().outDirFor(src);
        Task<Path> t = new Task<>() {
            @Override protected Path call() throws Exception {
                Path out = vault.sealFile(src, pw.toCharArray(), null, outDir, false, log::log, p -> { });
                SecureWiper.Result r = wiper.wipe(src);        // solo se llega aquí si el sello ya fue verificado
                log.log("Debí tirar más fotos cuando tenía el archivo sin encriptar... Archivo original eliminado pa' siempre.");
                log.log("Sobrescrito con ceros (" + r.bytes() + " bytes, " + (r.verifiedZero() ? "verificado" : "sin verificar")
                        + "), renombrado y eliminado.");
                return out;
            }
        };
        t.setOnSucceeded(e -> {
            log.log("Guardado en: " + t.getValue());
            source = null;
            fileLabel.setText("—");
            pass.clear();
            busy = false;
            celebrate();
        });
        t.setOnFailed(e -> { log.log("ERROR: " + t.getException().getMessage() + " (el original NO se tocó)"); busy = false; });
        // Orden de seguridad dentro de call(): sellar → verificar (lo hace sealFile) → SOLO entonces wipe(). Si algo
        // falla antes, la excepción salta el borrado y setOnFailed (ya en el hilo de JavaFX) avisa que el original sigue.
        Thread th = new Thread(t, "arana-safaera");
        th.setDaemon(true);
        th.start();
    }

    // ------------------------------------------------------------------ Concho

    private void celebrate() {
        ctx.audio().play(SoundEffect.CAMERA_FLASH);
        flash.setOpacity(1.0);
        Fx.fade(flash, 200, 1.0, 0.0).play();                         // FLASH blanco puro: opacidad 1.0 -> 0.0 en 200 ms
        say(MESSAGES[rnd.nextInt(MESSAGES.length)]);
        if (dance != null) dance.stop();
        dance = new Timeline();
        for (int i = 0; i < DANCE.length; i++) {
            final int pose = DANCE[i];
            final boolean fl = i == 0;
            dance.getKeyFrames().add(new KeyFrame(Duration.millis(i * 210), e -> drawConcho(pose, fl)));
        }
        dance.getKeyFrames().add(new KeyFrame(Duration.millis(DANCE.length * 210), e -> drawConcho(0, false)));
        dance.play();
    }

    private void say(String msg) {
        bubble.setText(msg);
        bubble.setOpacity(1);
        PauseTransition p = new PauseTransition(Duration.seconds(3.2));
        p.setOnFinished(e -> bubble.setOpacity(0));
        p.play();
    }

    private void drawConcho(int pose, boolean flashOn) {
        GraphicsContext g = concho.getGraphicsContext2D();
        g.clearRect(0, 0, concho.getWidth(), concho.getHeight());
        PixelArt.draw(g, ConchoSprite.generate(pose, flashOn).g, ConchoSprite.PALETTE, 0, 0, 5);
    }

    private static Button tropicalButton(String text, double size) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #ffb36b; -fx-text-fill: #2a1020; -fx-font-family: Monospaced; -fx-font-weight: bold;"
                + " -fx-font-size: " + size + "; -fx-background-radius: 12; -fx-cursor: hand; -fx-padding: 6 14 6 14;");
        return b;
    }
}
