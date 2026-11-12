package app.secret.view;

import app.crypto.Payload;
import app.secret.SecretContext;
import app.secret.audio.MusicTrack;
import app.secret.audio.SongCues;
import app.secret.dave.ArtSprite;
import app.secret.dave.ParticleCanvas;
import app.secret.ui.Fx;
import app.secret.ui.MusicReactor;
import app.secret.ui.SpectrumWave;
import app.stega.StegoVault;
import app.ui.Settings;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * INTERFAZ DE SOFÍA · MÓDULO CARTAS. Aparece al terminar su cinemática.
 * <p>
 * Conserva la función de cartas del modo amor del núcleo (escribir / abrir una nota secreta) con el MISMO motor:
 * {@link StegoVault#sealNote} cifra el texto con AES-256-GCM y lo esconde por LSB en un PNG de neón;
 * {@link StegoVault#open} lo recupera. La diferencia es solo de interfaz: retrato, corazones y un escritorio de cartas.
 * El cifrado corre en un {@link Task} (hilo daemon) para no congelar la UI; sus callbacks vuelven al hilo de JavaFX.
 */
public final class SofiaView extends TakeoverView {

    private static final double PORTRAIT = 400;
    private static final int MIN_PASS = 8;
    private static final String PINK = "#ff9ac8";

    private final SecretContext ctx;
    private final StegoVault vault = new StegoVault();
    private final Pane root = new Pane();
    private final ParticleCanvas hearts = new ParticleCanvas();
    private final List<Node> entrance;
    private final GuideHud guide;
    private final MusicReactor reactor;
    private int kicks;

    private final ToggleButton writeTab = tab("✉  ESCRIBIR CARTA");
    private final ToggleButton readTab = tab("♥  ABRIR CARTA");
    private final TextArea letter = new TextArea();
    private final TextField file = new TextField();
    private final Button pick = smallButton("...");
    private final HBox fileRow = new HBox(8, file, pick);
    private final PasswordField pass = new PasswordField();
    private final LoveLetterButton action = new LoveLetterButton("", 0.42);
    private final ProgressBar bar = new ProgressBar(0);
    private final Label status = Fx.label("Escríbele algo. Solo quien tenga la clave lo podrá leer.", 13, "#e6d6f2");
    private boolean busy;

    public SofiaView(SecretContext ctx, Runnable onExit) {
        this.ctx = ctx;
        setStyle("-fx-background-color: linear-gradient(to bottom, #0b0820, #2a1240);");
        root.setPrefSize(W, H);

        Node portrait = framedPortrait();
        Label tag = Fx.label("SECTOR S.P.  //  CARTAS SELLADAS", 14, PINK);
        tag.relocate(520, 40);
        Label name = new Label("Sofía");
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 64));
        name.setTextFill(Color.WHITE);
        name.setEffect(new DropShadow(22, Color.web("#ff5d8f")));
        name.relocate(516, 54);
        Label dedication = new Label("Mi Picaku Piojosa ♥");
        dedication.setFont(Font.font("Georgia", 24));
        dedication.setTextFill(Color.web("#ffb3c7"));
        dedication.relocate(708, 88);

        Node desk = letterDesk();
        desk.relocate(520, 150);

        // Zero-UI: ya no hay botón de salida; solo la pista del comando (no es clicable)
        Label exit = Fx.label("> escribe CALLE para volver  ·  ESC suelta el texto", 13, PINK);
        exit.setOpacity(0.75);
        exit.setMouseTransparent(true);
        exit.relocate(70, 652);

        // Ondas del espectro al pie de la pantalla, detrás de todo el contenido
        SpectrumWave wave = new SpectrumWave(W, 200, Color.web("#7b2cbf"), Color.web("#ff5d8f"), Color.web("#ffd166"));
        wave.setLayoutY(H - 200);
        wave.setOpacity(0.6);

        root.getChildren().addAll(starField(), wave, hearts, portrait, tag, name, dedication, desk, exit);
        getChildren().add(root);
        entrance = List.of(portrait, tag, name, dedication, desk, exit);
        entrance.forEach(n -> n.setOpacity(0));

        // La música mueve la interfaz: el retrato late con los graves, las ondas siguen el espectro y cada bombo suelta
        // corazones (uno o dos desde abajo; en los golpes más fuertes, un puñado desde el retrato)
        reactor = new MusicReactor(ctx.audio(), MusicTrack.SOFIA_THEME)
                .heartbeat(portrait, 0.022)
                .wave(wave)
                .onKick(() -> {
                    hearts.floatHearts(2, Color.web("#ff5d8f"), Color.web("#ffb3c7"), Color.web("#ffd166"));
                    if (++kicks % 4 == 0)
                        hearts.burst(270, 370, 6, ParticleCanvas.Kind.HEART, 160, -0.3, 1.4, 12, Color.web("#ff5d8f"), Color.web("#ffd6e7"));
                });
        reactor.start();
        onDispose(reactor::stop);

        guide = new GuideHud(PINK, "MÓDULO CARTAS",
                "Escribe un mensaje privado y guárdalo cifrado dentro de una foto de neón (PNG). Solo se lee con la clave.",
                List.of("ESCRIBIR CARTA: redacta el mensaje en el recuadro.",
                        "Pon una clave de al menos 8 caracteres y pulsa el sobre: se cifra con AES-256-GCM y se esconde en la imagen.",
                        "La foto se guarda en la carpeta de notas (Configuración o ~/AranaHack).",
                        "ABRIR CARTA: elige la foto, escribe la clave y pulsa el sobre para leerla."),
                "Envíala como ARCHIVO/documento: si una app la recomprime a JPG, la carta se pierde.");
        guide.attachTo(this, Pos.TOP_RIGHT, new Insets(18, 18, 0, 0));
        onDispose(guide::stop);

        // corazones de ambiente en gravedad cero, uno cada 1.3 s (los bombos sueltan el resto; un solo Canvas: barato)
        Timeline drift = track(new Timeline(new KeyFrame(Duration.millis(1300),
                e -> hearts.floatHearts(1, Color.web("#ff5d8f"), Color.web("#ffb3c7"), Color.web("#ffd166")))));
        drift.setCycleCount(Animation.INDEFINITE);
        drift.play();
        onDispose(hearts::stop);
        setMode(true);
    }

    /**
     * Entrada A TEMPO: cada bloque sube 16 px y aparece en una corchea de la canción (medio beat, 300.8 ms a 99.75 BPM),
     * empezando en el siguiente golpe. Los instantes se calculan sobre la rejilla del MP3 y los dispara el currentTime
     * real ({@link SongCues}), no un retraso fijo. Sin audio, la misma cadencia corre con el reloj de pared.
     */
    @Override public void onShown() {
        MusicTrack song = MusicTrack.SOFIA_THEME;
        double eighth = song.beatMs() / 2, pos = ctx.audio().musicPositionMs();
        // siguiente corchea con al menos 150 ms de margen (si no suena nada, la rejilla arranca en 0)
        double k0 = pos < 0 ? 1 : Math.ceil((pos + 150 - song.firstBeatMs) / eighth);
        double first = song.firstBeatMs + k0 * eighth;
        SongCues cues = new SongCues();
        for (int i = 0; i < entrance.size(); i++) {
            Node n = entrance.get(i);
            cues.at(first + i * eighth, () -> {
                TranslateTransition t = new TranslateTransition(Duration.millis(520), n);
                t.setFromY(16); t.setToY(0); t.setInterpolator(Interpolator.EASE_OUT);
                track(new ParallelTransition(Fx.fade(n, 520, 0, 1), t)).play();
            });
        }
        onDispose(cues.start(ctx.audio(), song, first - 250));
        // red de seguridad: si la canción da la vuelta justo ahora (el bucle vuelve a 0), nada se queda invisible
        PauseTransition safety = track(new PauseTransition(Duration.millis(entrance.size() * eighth + 2500)));
        safety.setOnFinished(e -> cues.feed(Double.MAX_VALUE));
        safety.play();
        guide.start();
    }

    // ------------------------------------------------------------------ construcción

    private Node framedPortrait() {
        ImageView img = ArtSprite.fitted(ArtSprite.Art.SOFIA, PORTRAIT);
        Rectangle clip = new Rectangle(PORTRAIT, PORTRAIT);
        clip.setArcWidth(60); clip.setArcHeight(60);
        img.setClip(clip);                                       // recorta las esquinas negras del PNG original
        Rectangle frame = new Rectangle(-14, -14, PORTRAIT + 28, PORTRAIT + 28);
        frame.setArcWidth(66); frame.setArcHeight(66);
        frame.setFill(Color.web("#140a26", 0.85));
        frame.setStroke(Color.web("#ffe08a")); frame.setStrokeWidth(4);
        Group g = new Group(frame, img);
        g.setEffect(new DropShadow(40, Color.web(PINK, 0.7)));
        g.relocate(70, 170);
        // respiración: ±5 px con EASE_BOTH (aproxima un seno), ida y vuelta indefinida
        TranslateTransition breathe = track(new TranslateTransition(Duration.millis(1900), img));
        breathe.setByY(-5); breathe.setAutoReverse(true); breathe.setCycleCount(Animation.INDEFINITE);
        breathe.setInterpolator(Interpolator.EASE_BOTH);
        breathe.play();
        return g;
    }

    private Node letterDesk() {
        ToggleGroup tabs = new ToggleGroup();
        writeTab.setToggleGroup(tabs);
        readTab.setToggleGroup(tabs);
        writeTab.setSelected(true);
        // una pestaña siempre seleccionada: si se intenta deseleccionar la activa, se vuelve a marcar
        tabs.selectedToggleProperty().addListener((o, was, now) -> {
            if (now == null) was.setSelected(true);
            else setMode(now == writeTab);
        });
        HBox tabRow = new HBox(8, writeTab, readTab);

        letter.setWrapText(true);
        letter.setPrefSize(700, 250);
        stylize(letter);
        file.setPromptText("foto .png con la carta...");
        file.setPrefWidth(640);
        stylize(file);
        pick.setOnAction(e -> pickFile());
        pass.setPromptText("clave (mín. 8)");
        pass.setPrefWidth(360);
        stylize(pass);
        action.setOnPress(this::run);
        HBox actionRow = new HBox(18, pass, action);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        bar.setPrefWidth(700);
        bar.setVisible(false);
        bar.setStyle("-fx-accent: #ff5d8f; -fx-control-inner-background: #1c0f30;");
        status.setWrapText(true);
        status.setMaxWidth(700);

        return new javafx.scene.layout.VBox(12, tabRow, letter, fileRow, actionRow, bar, status);
    }

    /** Cambia entre escribir y abrir; el mismo TextArea sirve de papel (editable) o de lector (solo lectura). */
    private void setMode(boolean write) {
        letter.setEditable(write);
        letter.setPromptText(write ? "Querida Sofía..." : "Aquí aparecerá la carta descifrada.");
        if (write) letter.clear();
        fileRow.setVisible(!write);
        fileRow.setManaged(!write);
        action.setText(write ? "Sellar carta" : "Abrir carta");
        status.setText(write ? "Escríbele algo. Solo quien tenga la clave lo podrá leer."
                : "Elige la foto que guarda la carta y escribe su clave.");
    }

    // ------------------------------------------------------------------ función real (motor del núcleo)

    private void run() {
        if (busy) return;
        if (writeTab.isSelected()) seal(); else open();
    }

    private void seal() {
        String text = letter.getText();
        if (text.isBlank()) { status.setText("La carta está vacía."); return; }
        if (pass.getText().length() < MIN_PASS) { status.setText("La clave necesita al menos " + MIN_PASS + " caracteres."); return; }
        char[] pw = pass.getText().toCharArray();
        Path dir = Settings.get().notesDir();
        String base = "carta_sofia_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Task<Path> task = new Task<>() {
            // call() corre en el hilo de fondo. updateMessage/updateProgress son seguros desde aquí: JavaFX los
            // agrupa y los publica en el hilo de la UI (por eso status y bar pueden estar enlazados a ellos).
            @Override protected Path call() throws Exception {
                Files.createDirectories(dir);
                Path out = StegoVault.uniquePath(dir, base, ".png");
                return vault.sealNote(text, pw, out, true, this::updateMessage, p -> updateProgress(p, 1.0));
            }
        };
        launch(task, pw, path -> {
            status.setText("Carta sellada en: " + path);
            letter.clear();
            hearts.burst(860, 420, 30, ParticleCanvas.Kind.HEART, 320, -0.3, 1.4, 14, Color.web("#ff5d8f"), Color.web("#ffd6e7"));
        });
    }

    private void open() {
        Path img;
        try { img = Path.of(file.getText().trim()); } catch (InvalidPathException ex) { status.setText("Ruta inválida."); return; }
        if (file.getText().isBlank() || !Files.isRegularFile(img)) { status.setText("Elige la foto que guarda la carta."); return; }
        if (pass.getText().isEmpty()) { status.setText("Falta la clave."); return; }
        char[] pw = pass.getText().toCharArray();
        Task<Payload> task = new Task<>() {
            @Override protected Payload call() throws Exception {
                return vault.open(img, pw, this::updateMessage, p -> updateProgress(p, 1.0));
            }
        };
        launch(task, pw, p -> {
            if (p.kind() == Payload.Kind.NOTE) {
                letter.setText(p.noteText());
                status.setText("Carta abierta. Léela despacio ♥");
            } else {
                status.setText("Esa imagen guarda un archivo, no una carta: ábrela con [2] CURAR ARCHIVO del menú base.");
            }
        });
    }

    /**
     * Arranca la tarea en un hilo daemon (no impide cerrar la app). Mientras corre, la barra y el estado se ENLAZAN a
     * progress/message de la tarea; setOnSucceeded/setOnFailed se ejecutan ya en el hilo de JavaFX, así que pueden
     * tocar nodos directamente sin Platform.runLater. La clave se borra de memoria al terminar, salga bien o mal.
     */
    private <T> void launch(Task<T> task, char[] pw, java.util.function.Consumer<T> onOk) {
        busy = true;
        action.setDisable(true);
        bar.setVisible(true);
        bar.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());
        Runnable done = () -> {
            Arrays.fill(pw, '\0');
            bar.progressProperty().unbind();
            status.textProperty().unbind();
            bar.setVisible(false);
            action.setDisable(false);
            pass.clear();
            busy = false;
        };
        task.setOnSucceeded(e -> { done.run(); if (!isDisposed()) onOk.accept(task.getValue()); });
        task.setOnFailed(e -> {
            done.run();
            Throwable ex = task.getException();
            status.setText("ERROR: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        });
        Thread t = new Thread(task, "caleta-cartas");
        t.setDaemon(true);
        t.start();
    }

    private void pickFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Foto con la carta");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG", "*.png"));
        File f = fc.showOpenDialog(getScene().getWindow());
        if (f != null) file.setText(f.getAbsolutePath());
    }

    // ------------------------------------------------------------------ estilo

    private static ToggleButton tab(String text) {
        ToggleButton b = new ToggleButton(text);
        b.setStyle("-fx-background-color: transparent; -fx-border-color: " + PINK + "; -fx-border-width: 0 0 2 0;"
                + " -fx-text-fill: #ffd6e7; -fx-font-family: Monospaced; -fx-font-weight: bold; -fx-font-size: 14; -fx-cursor: hand;");
        b.selectedProperty().addListener((o, a, on) -> b.setOpacity(on ? 1 : 0.55));
        b.setOpacity(0.55);
        return b;
    }

    private static Button smallButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + PINK + "; -fx-text-fill: #12001f; -fx-font-family: Monospaced; -fx-font-weight: bold;"
                + " -fx-font-size: 14; -fx-background-radius: 6; -fx-cursor: hand;");
        return b;
    }

    private static void stylize(TextInputControl c) {
        c.setStyle("-fx-background-color: #ff9ac8, #140a26; -fx-background-insets: 0, 2; -fx-text-fill: #ffe9f4;"
                + " -fx-control-inner-background: #140a26; -fx-font-family: Monospaced; -fx-font-size: 15;"
                + " -fx-prompt-text-fill: #7a4a6e; -fx-highlight-fill: #ff5d8f;");
    }

    private static Pane starField() {
        Pane p = new Pane();
        p.setMouseTransparent(true);
        Random r = new Random(5);
        for (int i = 0; i < 90; i++) {
            Circle c = new Circle(r.nextDouble() * W, r.nextDouble() * H, 0.7 + r.nextDouble() * 1.8, Color.WHITE);
            c.setOpacity(0.15 + r.nextDouble() * 0.6);
            p.getChildren().add(c);
        }
        return p;
    }
}
