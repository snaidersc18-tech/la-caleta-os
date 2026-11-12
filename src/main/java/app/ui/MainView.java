package app.ui;

import app.animations.CrtOverlay;
import app.animations.GlitchText;
import app.animations.KeyTrigger;
import app.animations.LoveParticles;
import app.animations.PicakuPayload;
import app.narrative.StreetLogs;
import app.secret.audio.AudioManager;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * "MINISTERIO DEL REBUSQUE": composición de toda la interfaz sobre un lienzo lógico de 1280x720 (se escala al tamaño
 * de la ventana desde Main). Orden de capas, de abajo hacia arriba:
 *   window (UI) < effectsLayer (cascada/partículas) < flash < [capas de app.secret] < lock < CRT
 * <p>
 * ZERO-UI: no hay botón de salir ni de "volver a la calle". La navegación de escape es SOLO por comandos tecleados
 * (los interpreta app.secret.TriggerHook a nivel de ventana): CALLE vuelve al inicio, SALIR apaga el programa.
 * La cinemática de arranque NO vive aquí: Main la monta en un telón negro por encima de esta vista desde el frame 0.
 */
public final class MainView extends StackPane {

    public static final double W = 1280, H = 720;

    /**
     * Menú CALLE (Zero-Trust: solo se cura). Cifrar no existe en la vista principal: las ÚNICAS puertas para esconder
     * archivos son los modos secretos (DAFTPUNK, BADBUNNY/CONCHO). Tampoco hay [SALIR]: se escribe SALIR.
     */
    private static final String[] STREET_LABELS = {"[1] CURAR ARCHIVO", "[2] CONFIGURACIÓN"};
    /** Modo amor (PICAKU): sin [VOLVER A LA CALLE]; se escribe CALLE. */
    private static final String[] LOVE_LABELS = {"[1] ESCRIBIR NOTA", "[2] ABRIR NOTA", "[3] CONFIGURACIÓN"};

    private final Stage stage;
    private final AudioManager audio;
    private final ChamferPane window = new ChamferPane(22, false, false, true, false);
    private final Label titleLabel = new Label("MINISTERIO DEL REBUSQUE");
    private final HBox titleRow = new HBox();
    private final HatchStrip hatch = new HatchStrip(430, 14);
    private final ConsolePanel console = new ConsolePanel();
    private final SpiderCanvas spider = new SpiderCanvas(5);
    private final MaskPanel mask = new MaskPanel(spider);
    private final Sidebar sidebar = new Sidebar(STREET_LABELS);
    private final StackPane viewHost = new StackPane();
    private final Pane effectsLayer = new Pane();
    private final Rectangle flashRect = new Rectangle(W, H);
    private final InputLock lock = new InputLock();
    private final CrtOverlay crt = new CrtOverlay(W, H);
    private final LoveParticles ambient = new LoveParticles();

    private final CureView cureView = new CureView(console);
    private final ConfigView configView = new ConfigView();
    private final SecretNotesView notesView = new SecretNotesView(console);

    private final PicakuPayload payload = new PicakuPayload(this);
    private boolean introDone;

    public MainView(Stage stage, AudioManager audio) {
        this.stage = stage;
        this.audio = audio;
        setMinSize(W, H); setPrefSize(W, H); setMaxSize(W, H);
        setFocusTraversable(true);
        Theme.get().attach(this);

        buildWindow();

        effectsLayer.setMouseTransparent(true);
        effectsLayer.setPickOnBounds(false);
        flashRect.setMouseTransparent(true);
        flashRect.setOpacity(0);

        getChildren().addAll(window, effectsLayer, flashRect, lock, crt);
        StackPane.setMargin(window, new Insets(18));

        spider.setOpacity(0);                                     // aparece con el "sello" al terminar la intro
        sidebar.setOnSelect(this::onMenu);
        console.setOnBackdoor(payload::trigger);
        setOnMouseClicked(e -> requestFocus());

        sceneProperty().addListener((o, a, sc) -> {
            if (sc != null) KeyTrigger.install(sc, payload::trigger, "SOFIA", "PICAKU");
        });
    }

    // ------------------------------------------------------------------ construcción

    private void buildWindow() {
        // Barra de título: pestaña biselada + minimizar/maximizar. SIN [X]: el programa se apaga escribiendo SALIR.
        ChamferPane tab = new ChamferPane(18, 2, false, true, false, false, p -> p.bg());
        titleLabel.getStyleClass().add("ar-title");
        titleLabel.setPadding(new Insets(5, 44, 5, 14));
        tab.getChildren().add(titleLabel);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        GlitchButton min = winButton("_");
        GlitchButton max = winButton("[]");
        min.setOnAction(e -> stage.setIconified(true));
        max.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
        HBox btns = new HBox(4, min, max);
        btns.setAlignment(Pos.CENTER_RIGHT);
        titleRow.getChildren().addAll(tab, sp, btns);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        double[] drag = new double[2];
        titleRow.setOnMousePressed(e -> { drag[0] = e.getScreenX() - stage.getX(); drag[1] = e.getScreenY() - stage.getY(); });
        titleRow.setOnMouseDragged(e -> {
            if (!stage.isMaximized()) { stage.setX(e.getScreenX() - drag[0]); stage.setY(e.getScreenY() - drag[1]); }
        });

        VBox top = new VBox(8, titleRow, new HBox(hatch));
        top.setPadding(new Insets(0, 0, 10, 0));

        // Zona central
        viewHost.getChildren().add(cureView);
        HBox.setHgrow(viewHost, Priority.ALWAYS);
        HBox center = new HBox(16, viewHost, mask);
        console.setPrefHeight(178); console.setMinHeight(178);
        BorderPane inner = new BorderPane();
        inner.setTop(top);
        inner.setLeft(sidebar);
        BorderPane.setMargin(sidebar, new Insets(0, 14, 0, 0));
        BorderPane centerPane = new BorderPane();
        centerPane.setCenter(center);
        centerPane.setBottom(console);
        BorderPane.setMargin(console, new Insets(14, 0, 0, 0));
        inner.setCenter(centerPane);
        inner.setPadding(new Insets(16, 16, 16, 16));

        window.getChildren().add(inner);
        sidebar.select(0);
    }

    private static GlitchButton winButton(String t) {
        GlitchButton b = new GlitchButton(t);
        b.getStyleClass().add("ar-win");
        return b;
    }

    // ------------------------------------------------------------------ arranque

    /**
     * Main lo llama cuando el telón de la cinemática de arranque terminó de desvanecerse: desde aquí la interfaz
     * acepta comandos y la consola escribe los logs de arranque.
     */
    public void introFinished() {
        introDone = true;
        bootLogs();
        app.secret.ui.FerxxoRiderArt.prepareInBackground();       // y el callejón + Medellín de FERXXO RIDER
    }

    /**
     * EL SELLO: en el milisegundo en que el telón empieza a desvanecerse, la silueta blanca de El Guachimán cae desde
     * arriba a 3.2x y se estampa en su marco (ScaleTransition + TranslateTransition + FadeTransition, EASE_IN: acelera
     * hasta el golpe, como un sello de caucho). Al impactar se "entinta" (ColorAdjust de blanco a sus colores),
     * se aplasta un instante y el panel tiembla.
     */
    public void stampSpider() {
        javafx.scene.effect.ColorAdjust ink = new javafx.scene.effect.ColorAdjust(0, 0, 1, 0);   // silueta blanca
        spider.setEffect(ink);
        spider.setScaleX(3.2); spider.setScaleY(3.2);
        spider.setTranslateY(-170);
        spider.setOpacity(0);

        Duration fall = Duration.millis(330), delay = Duration.millis(220);   // que el telón ya deje ver el panel
        javafx.animation.ScaleTransition shrink = new javafx.animation.ScaleTransition(fall, spider);
        shrink.setToX(1); shrink.setToY(1);
        javafx.animation.TranslateTransition drop = new javafx.animation.TranslateTransition(fall, spider);
        drop.setToY(0);
        FadeTransition appear = new FadeTransition(Duration.millis(160), spider);
        appear.setToValue(1);
        javafx.animation.ParallelTransition stamp = new javafx.animation.ParallelTransition(shrink, drop, appear);
        stamp.setInterpolator(javafx.animation.Interpolator.EASE_IN);
        stamp.setDelay(delay);
        stamp.setOnFinished(e -> {
            // impacto: aplastón 112 % × 88 % → 100 %, temblor del panel y la tinta "se seca" en 380 ms
            javafx.animation.ScaleTransition squash = new javafx.animation.ScaleTransition(Duration.millis(140), spider);
            squash.setFromX(1.12); squash.setFromY(0.88);
            squash.setToX(1); squash.setToY(1);
            squash.play();
            app.animations.ScreenShake.shake(mask, 6, 260);
            spider.glitchFor(300);
            Timeline dry = new Timeline(new KeyFrame(Duration.millis(380),
                    new javafx.animation.KeyValue(ink.brightnessProperty(), 0, javafx.animation.Interpolator.EASE_OUT)));
            dry.setOnFinished(ev -> spider.setEffect(null));
            dry.play();
        });
        stamp.play();
    }

    private void bootLogs() {
        Timeline t = new Timeline();
        String[] lines = new String[StreetLogs.BOOT.length + 1];
        System.arraycopy(StreetLogs.BOOT, 0, lines, 0, StreetLogs.BOOT.length);
        lines[lines.length - 1] = StreetLogs.COMMANDS;
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            t.getKeyFrames().add(new KeyFrame(Duration.millis(300 + i * 520L), e -> console.println(line)));
        }
        t.play();                                                 // el tema de la CALLE sigue en bucle debajo de los logs
    }

    // ------------------------------------------------------------------ menú

    private void onMenu(int idx) {
        if (lock.isLocked()) return;
        if (payload.isLove()) {
            switch (idx) {
                case 0 -> { sidebar.select(0); show(notesWrite()); }
                case 1 -> { sidebar.select(1); show(notesOpen()); }
                case 2 -> { sidebar.select(2); show(configView); }
                default -> { }
            }
            return;
        }
        // Menú CALLE (antivirus de barrio): [1] CURAR  [2] CONFIGURACIÓN
        switch (idx) {
            case 0 -> { sidebar.select(0); show(cureView); }
            case 1 -> { sidebar.select(1); show(configView); }
            default -> { }
        }
    }

    private Node notesWrite() { notesView.setMode(SecretNotesView.Mode.WRITE); return notesView; }
    private Node notesOpen() { notesView.setMode(SecretNotesView.Mode.OPEN); return notesView; }

    private void show(Node view) {
        if (!viewHost.getChildren().contains(view) || viewHost.getChildren().size() != 1) {
            viewHost.getChildren().setAll(view);
        }
        view.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(170), view);
        ft.setToValue(1);
        ft.play();
    }

    // ------------------------------------------------------------------ API para las animaciones

    public ConsolePanel console() { return console; }
    public SpiderCanvas spider() { return spider; }
    public Pane effectsLayer() { return effectsLayer; }
    public Pane maskParticles() { return mask.particleLayer(); }
    public Node shakeTarget() { return window; }
    public InputLock lock() { return lock; }
    public boolean isIntroDone() { return introDone; }

    /** ¿Está activo el modo amor (payload PICAKU)? También es un easter egg: CALLE lo revierte. */
    public boolean isLove() { return payload.isLove(); }

    /** El AudioManager de la app (null si se creó sin audio). */
    public AudioManager audio() { return audio; }

    /** Vuelve del modo amor a la calle (lo dispara el Mordisco de la Araña con CALLE). */
    public void revertLove() { payload.revert(); }

    /** Destello de pantalla completa. */
    public void flash(Color c, int ms) {
        flashRect.setFill(c);
        flashRect.setOpacity(0.85);
        FadeTransition ft = new FadeTransition(Duration.millis(ms), flashRect);
        ft.setToValue(0);
        ft.play();
    }

    public void flash() { flash(Color.WHITE, 220); }

    /** Cambia textos, araña, partículas y menú entre estado 1 (calle) y estado 2 (amor). */
    public void setLoveVisuals(boolean love) {
        spider.setLoveMode(love);
        spider.glitchFor(600);
        mask.setTitle(love ? "MIGA" : "EL GUACHIMÁN");
        GlitchText.morph(titleLabel, love ? "[!] ERROR 404: ORGULLO NOT FOUND" : "MINISTERIO DEL REBUSQUE", Duration.millis(700));
        sidebar.setLabels(love ? LOVE_LABELS : STREET_LABELS);
        if (love) ambient.startAmbient(mask.particleLayer(), 40, MaskPanel.W - 70, MaskPanel.BODY_H - 40);
        else ambient.stop();
        if (!love) console.titleLabel().setText("CONSOLA DE EVENTOS");
    }

    /** Abre el espacio de trabajo: notas en modo amor, la vista CURAR en la calle. Marca la pestaña 1. */
    public void enterWorkspace(boolean love) {
        sidebar.select(0);
        show(love ? notesWrite() : cureView);
    }

    /** Vuelve al Menú CALLE con la vista CURAR seleccionada (la usa el Mordisco de la Araña al terminar). */
    public void showStreetMenu() {
        if (!payload.isLove()) enterWorkspace(false);
    }
}
