package app.secret.view;

import app.secret.SecretContext;
import app.secret.service.MetadataScrubber;
import app.secret.ui.DjPad;
import app.secret.ui.Fx;
import app.secret.ui.LightShow;
import app.secret.ui.LogBox;
import app.secret.ui.SynthwaveGrid;
import app.secret.ui.VocalHud;
import app.stega.StegoVault;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * "CONSOLA CIBER-MUSICAL": la interfaz exclusiva del modo Human After All. Sin los inputs originales: un disco de
 * vinilo gira con la portada (el "track"), un ecualizador late con la actividad y los controles son pads de DJ.
 * Funciones REALES: cargar portada y muestra, auditar y borrar la huella EXIF (MetadataScrubber) y sellar el disco
 * (StegoVault: AES-256-GCM + LSB) usando siempre la portada ya limpia, auditando además el PNG final.
 */
public final class DaftPunkConsoleView extends TakeoverView {

    private final SecretContext ctx;
    private final StegoVault vault = new StegoVault();
    private final MetadataScrubber scrubber = new MetadataScrubber();

    private final LogBox log = new LogBox("#00f5ff", "#05060f", 760, 132);
    private final PasswordField pass = new PasswordField();
    private final ImageView labelImg = new ImageView();
    private final Circle labelBase = new Circle(62, Color.web("#ff2bd6"));
    private final Label chipTrack = Fx.label("TRACK: —", 13, "#00f5ff");
    private final Label chipSample = Fx.label("MUESTRA: —", 13, "#ffe94a");
    private final Label chipPrint = Fx.label("HUELLA: —", 13, "#ff2bd6");
    private final Rectangle[] bars = new Rectangle[28];
    private final Random rnd = new Random();
    private final GuideHud guide;
    private RotateTransition spin;

    private Path cover, source, cleanCover;
    private boolean busy;

    public DaftPunkConsoleView(SecretContext ctx, Runnable onExit) {
        this.ctx = ctx;
        setStyle("-fx-background-color: linear-gradient(to bottom, #07051a, #150a33);");
        Pane root = new Pane();
        root.setPrefSize(W, H);

        SynthwaveGrid grid = new SynthwaveGrid(W, H);
        grid.setOpacity(0.35);
        grid.start();
        LightShow lights = new LightShow(W, H).syncTo(ctx.audio());   // cambia de color en cada golpe de la canción
        lights.setOpacity(0.30);
        lights.start();
        onDispose(() -> { grid.stop(); lights.stop(); });
        root.getChildren().addAll(grid, lights);

        Label title = Fx.label("HUMAN AFTER ALL  //  CONSOLA CIBER-MUSICAL", 26, "#00f5ff");
        title.setEffect(new javafx.scene.effect.DropShadow(14, Color.web("#00f5ff")));
        title.relocate(40, 22);
        Rectangle line = new Rectangle(40, 66, 1200, 3);
        line.setFill(Color.web("#ff2bd6"));
        root.getChildren().addAll(title, line);

        buildVinyl(root);
        buildCenter(root);
        buildPads(root);

        log.relocate(40, 560);
        root.getChildren().add(log);
        getChildren().add(root);

        // HARDER / BETTER / FASTER / STRONGER cuando el vocoder lo canta (reloj = currentTime de la canción)
        VocalHud vocals = new VocalHud(W, H).syncTo(ctx.audio());
        getChildren().add(vocals);
        onDispose(vocals::stop);

        guide = new GuideHud("#00f5ff", "MÓDULO HUMAN AFTER ALL",
                "Borra la huella EXIF de una foto (GPS, cámara, fecha) y esconde un archivo cifrado dentro de ella.",
                List.of("CARGAR TRACK: elige la imagen portada (la que todos verán).",
                        "BORRAR HUELLA EXIF: audita los metadatos y reconstruye la imagen como PNG limpio.",
                        "CARGAR MUESTRA: elige el archivo secreto (máx. 16 MB).",
                        "CLAVE DEL MIXER: escribe una contraseña de al menos 8 caracteres.",
                        "SELLAR DISCO: cifra con AES-256-GCM, lo mete en la portada por LSB y audita el PNG final."),
                "El PNG resultante no debe convertirse a JPG o se pierde el contenido.");
        guide.attachTo(this, Pos.TOP_RIGHT, new Insets(14, 18, 0, 0));
        onDispose(guide::stop);

        log.log("Consola lista. Carga un TRACK (portada) y una MUESTRA (el archivo a esconder).");
        log.log("Harder, Better, Faster, Encaletado... aquí la huella EXIF no sobrevive.");
        onDispose(this::deleteCleanCover);
    }

    @Override public void onShown() { guide.start(); }

    // ------------------------------------------------------------------ construcción

    private void buildVinyl(Pane root) {
        Pane disc = new Pane();
        disc.setPrefSize(300, 300);
        Circle body = new Circle(150, 150, 148);
        body.setFill(new RadialGradient(0, 0, 150, 150, 148, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1b1b2a")), new Stop(1, Color.web("#050508"))));
        body.setStroke(Color.web("#00f5ff"));
        body.setStrokeWidth(3);
        disc.getChildren().add(body);
        for (int r = 70; r < 146; r += 7) {
            Circle g = new Circle(150, 150, r, Color.TRANSPARENT);
            g.setStroke(Color.web("#ffffff", 0.07));
            disc.getChildren().add(g);
        }
        labelBase.setCenterX(150); labelBase.setCenterY(150);
        labelImg.setFitWidth(124); labelImg.setFitHeight(124);
        labelImg.setLayoutX(88); labelImg.setLayoutY(88);
        labelImg.setClip(new Circle(62, 62, 62));
        Circle hole = new Circle(150, 150, 6, Color.web("#05060f"));
        disc.getChildren().addAll(labelBase, labelImg, hole);
        disc.relocate(60, 100);
        spin = track(new RotateTransition(Duration.seconds(4), disc));
        spin.setByAngle(360);
        spin.setCycleCount(Animation.INDEFINITE);
        spin.setInterpolator(javafx.animation.Interpolator.LINEAR);
        spin.setRate(0.35);
        spin.play();
        root.getChildren().add(disc);

        chipTrack.relocate(60, 420);
        chipSample.relocate(60, 450);
        chipPrint.relocate(60, 480);
        root.getChildren().addAll(chipTrack, chipSample, chipPrint);
    }

    private void buildCenter(Pane root) {
        MascotView mascot = new MascotView(MascotView.Skin.STAGE_MECHA, 380, 400);
        mascot.relocate(410, 110);
        root.getChildren().add(mascot);
        onDispose(mascot::stop);
        mascot.mutate();                                          // aparición con glitch y rebote elástico

        for (int i = 0; i < bars.length; i++) {
            Rectangle r = new Rectangle(8, 6, Color.web(i % 3 == 0 ? "#ff2bd6" : (i % 3 == 1 ? "#00f5ff" : "#ffe94a")));
            r.relocate(414 + i * 11, 534);
            bars[i] = r;
            root.getChildren().add(r);
        }
        Timeline eq = track(new Timeline(new KeyFrame(Duration.millis(110), e -> {
            // el ecualizador "patea" en cada bombo: envolvente exp(−6·fase) sobre la posición real de la canción
            double b = ctx.audio().beats();
            double kick = Double.isNaN(b) ? 0.5 : Math.exp(-6 * (b - Math.floor(b)));
            double amp = (busy ? 80 : 30) * (0.35 + 0.9 * kick);
            for (Rectangle r : bars) {
                double h = 6 + rnd.nextDouble() * amp;
                r.setHeight(h);
                r.setLayoutY(540 - h);
            }
        })));
        eq.setCycleCount(Animation.INDEFINITE);
        eq.play();
    }

    private void buildPads(Pane root) {
        DjPad track = new DjPad("CARGAR TRACK (IMAGEN)", "la portada del disco", "#00f5ff", 190, 100);
        DjPad sample = new DjPad("CARGAR MUESTRA (ARCHIVO)", "el secreto a esconder", "#ffe94a", 190, 100);
        DjPad wipe = new DjPad("BORRAR HUELLA EXIF", "GPS · cámara · fecha", "#ff2bd6", 190, 100);
        DjPad key = new DjPad("CLAVE DEL MIXER", "contraseña", "#2fe05a", 190, 100);
        DjPad seal = new DjPad("SELLAR DISCO", "AES-256-GCM + LSB", "#00ff9d", 392, 100);
        // Zero-UI: el pad "SALIR DEL ESCENARIO" desapareció; solo queda la pista del comando (no es clicable)
        Label exit = Fx.label("> escribe CALLE para bajarte del escenario", 13, "#ff5050");
        exit.setOpacity(0.85);
        exit.setMouseTransparent(true);
        track.relocate(830, 90);  sample.relocate(1032, 90);
        wipe.relocate(830, 200);  key.relocate(1032, 200);
        seal.relocate(830, 310);
        exit.relocate(830, 500);
        root.getChildren().addAll(track, sample, wipe, key, seal, exit);

        Label kl = Fx.label("CLAVE DEL MIXER (mín. 8)", 13, "#2fe05a");
        kl.relocate(830, 424);
        pass.setPromptText("••••••••");
        pass.setPrefWidth(392);
        pass.setStyle("-fx-background-color: #2fe05a, #05060f; -fx-background-insets: 0, 2; -fx-text-fill: #2fe05a;"
                + " -fx-font-family: Monospaced; -fx-font-size: 16; -fx-prompt-text-fill: #1b6b30; -fx-padding: 6 10 6 10;");
        pass.relocate(830, 446);
        root.getChildren().addAll(kl, pass);

        track.setOnAction(this::pickTrack);
        sample.setOnAction(this::pickSample);
        wipe.setOnAction(this::eraseFingerprint);
        key.setOnAction(() -> pass.requestFocus());
        seal.setOnAction(this::sealDisc);
    }

    // ------------------------------------------------------------------ acciones (funciones reales)

    private void pickTrack() {
        File f = chooseFile("Track (imagen portadora)", true);
        if (f == null) return;
        deleteCleanCover();
        cover = f.toPath();
        chipTrack.setText("TRACK: " + shorten(f.getName()));
        chipPrint.setText("HUELLA: analizando...");
        try {
            Image img = new Image(f.toURI().toString(), 124, 124, false, true);
            labelImg.setImage(img);
            MetadataScrubber.Report r = scrubber.inspect(cover);
            chipPrint.setText(r.any() ? "HUELLA: SUCIA (" + r.format() + ")" : "HUELLA: limpia");
            log.log(r.any() ? "Huella detectada en el track: " + r.summary() : "El track no trae metadatos visibles.");
        } catch (IOException | RuntimeException ex) {             // inspect() declara IOException; Image falla en runtime
            log.log("No pude leer el track: " + ex.getMessage());
        }
        spin.setRate(1.0);
    }

    private void pickSample() {
        File f = chooseFile("Muestra (archivo a esconder)", false);
        if (f == null) return;
        source = f.toPath();
        chipSample.setText("MUESTRA: " + shorten(f.getName()));
        log.log("Muestra cargada: " + f.getName() + " (" + f.length() / 1024 + " KB)");
    }

    private void eraseFingerprint() {
        if (cover == null) { log.log("Primero carga un TRACK (imagen)."); return; }
        if (busy) return;
        setBusy(true);
        final Path c = cover;
        // El hilo de fondo solo CALCULA (informe + PNG temporal); el campo cleanCover se reasigna en setOnSucceeded,
        // que JavaFX ejecuta en su propio hilo. Antes se escribía desde el worker y podía cruzarse con sealDisc().
        Path[] produced = new Path[1];
        Task<MetadataScrubber.Report> t = new Task<>() {
            @Override protected MetadataScrubber.Report call() throws Exception {
                MetadataScrubber.Report r = scrubber.inspect(c);
                produced[0] = scrubber.scrubToTempPng(c);
                return r;
            }
        };
        t.setOnSucceeded(e -> {
            deleteCleanCover();
            cleanCover = produced[0];
            MetadataScrubber.Report r = t.getValue();
            log.log("Harder, Better, Faster, Encaletado... Metadatos destruidos por completo.");
            log.log(r.any() ? "Destruido en el track (" + r.format() + "): " + r.summary()
                    : "El track (" + r.format() + ") no traía metadatos; reconstruí los píxeles igual (sin ICC ni alfa).");
            chipPrint.setText("HUELLA: DESTRUIDA ✔");
            setBusy(false);
        });
        t.setOnFailed(e -> {
            if (produced[0] != null) { try { Files.deleteIfExists(produced[0]); } catch (IOException | SecurityException ignored) { /* limpieza de mejor esfuerzo: el error real ya se reporta abajo */ } }
            log.log("ERROR: " + t.getException().getMessage());
            setBusy(false);
        });
        run(t);
    }

    private void sealDisc() {
        if (busy) return;
        if (source == null || !Files.isRegularFile(source)) { log.log("Falta la MUESTRA (archivo a esconder)."); return; }
        String pw = pass.getText();
        if (pw.length() < 8) { log.log("La clave del mixer necesita al menos 8 caracteres."); return; }
        setBusy(true);
        final Path src = source, cov = cover, preClean = cleanCover;
        final Path outDir = app.ui.Settings.get().outDirFor(src);
        Task<Path> t = new Task<>() {
            @Override protected Path call() throws Exception {
                Path use = preClean;
                boolean mine = false;
                try {
                    if (use == null && cov != null) {
                        MetadataScrubber.Report r = scrubber.inspect(cov);
                        use = scrubber.scrubToTempPng(cov);
                        mine = true;
                        log.log("Harder, Better, Faster, Encaletado... Metadatos destruidos por completo.");
                        log.log(r.any() ? "Destruido en el track: " + r.summary() : "Track sin metadatos; píxeles reconstruidos.");
                    } else if (use == null) {
                        log.log("Sin track: el skyline generado nace sin un solo byte de EXIF.");
                    }
                    Path out = vault.sealFile(src, pw.toCharArray(), use, outDir, false, log::log, p -> updateProgress(p, 1.0));
                    List<String> removed = scrubber.stripAncillaryInPlace(out);
                    log.log(removed.isEmpty() ? "PNG final auditado: solo chunks críticos (IHDR/IDAT/IEND). Cero metadatos."
                            : "PNG final: eliminé además los chunks " + removed + ".");
                    return out;
                } finally {
                    if (mine && use != null) Files.deleteIfExists(use);
                }
            }
        };
        t.setOnSucceeded(e -> {
            log.log("DISCO SELLADO → " + t.getValue());
            pass.clear();
            spin.setRate(2.2);
            setBusy(false);
        });
        t.setOnFailed(e -> { log.log("ERROR: " + t.getException().getMessage()); setBusy(false); });
        run(t);
    }

    // ------------------------------------------------------------------ utilidades

    private File chooseFile(String title, boolean images) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle(title);
        if (images) fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Imágenes", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        return fc.showOpenDialog(getScene().getWindow());
    }

    /**
     * El trabajo pesado (auditar EXIF, PBKDF2 + AES-256-GCM, LSB) corre en un hilo daemon para no congelar la UI.
     * Dentro de call() solo se escribe en LogBox, que es seguro desde cualquier hilo (se reencola con runLater);
     * setOnSucceeded/setOnFailed ya llegan al hilo de JavaFX.
     */
    private void run(Task<?> t) {
        Thread th = new Thread(t, "arana-daft");
        th.setDaemon(true);
        th.start();
    }

    private void setBusy(boolean b) { this.busy = b; }

    private void deleteCleanCover() {
        Path c = cleanCover;
        cleanCover = null;
        if (c != null) { try { Files.deleteIfExists(c); } catch (IOException | SecurityException ignored) { /* temporal: si no se borra, lo limpia el SO */ } }
    }

    private static String shorten(String s) { return s.length() > 26 ? s.substring(0, 23) + "..." : s; }
}
