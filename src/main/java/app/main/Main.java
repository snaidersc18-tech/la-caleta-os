package app.main;

import app.animations.FpsProbe;
import app.animations.UndergroundIntro;
import app.secret.EasterEggManager;
import app.secret.audio.AudioManager;
import app.ui.MainView;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Ministerio Del Rebusque: arranque de la aplicación JavaFX.
 * <p>
 * GARANTÍA DE FRAME CERO. La raíz de la escena es un {@link StackPane} con dos capas:
 * <pre>
 *   [arriba]  telón: StackPane NEGRO SÓLIDO con la cinemática de arranque (lienzo 1280x720 escalado)
 *   [abajo ]  la interfaz (MainView, escalada igual)
 * </pre>
 * El telón ya está en la escena ANTES de {@code stage.show()}, así que el primer cuadro que pinta la ventana es negro:
 * la interfaz nunca se asoma, ni siquiera un frame. La cinemática arranca en el mismo pulso que show() (sin
 * Platform.runLater) y, al terminar, un FadeTransition lleva el telón a opacidad 0 y lo retira de la escena.
 */
public final class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Ministerio Del Rebusque");
        setIcons(stage);

        // Un solo AudioManager para todo: intro (aerosol + tema de la CALLE), easter eggs, mordisco y exterminio.
        AudioManager audio = new AudioManager();
        audio.preload();                                   // decodifica los SFX en segundo plano antes de la intro
        MainView view = new MainView(stage, audio);
        EasterEggManager.install(view, audio);             // modos secretos + comandos CALLE (mordisco) y SALIR (exterminio)

        // Capa inferior: la interfaz.
        Pane uiLayer = letterbox(view);
        uiLayer.setStyle("-fx-background-color: black;");

        // Capa superior: telón negro sólido con el lienzo de la cinemática.
        Pane introCanvas = new Pane();
        introCanvas.setMinSize(MainView.W, MainView.H);
        introCanvas.setPrefSize(MainView.W, MainView.H);
        introCanvas.setMaxSize(MainView.W, MainView.H);
        StackPane curtain = new StackPane(letterbox(introCanvas));
        curtain.setStyle("-fx-background-color: black;");  // opaco: tapa la UI desde el frame 0

        StackPane root = new StackPane(uiLayer, curtain);
        root.setStyle("-fx-background-color: black;");
        Scene scene = new Scene(root, MainView.W, MainView.H, Color.BLACK);
        scene.getStylesheets().add(getClass().getResource("/css/arana.css").toExternalForm());

        stage.setMinWidth(640);
        stage.setMinHeight(360);
        stage.setScene(scene);
        stage.show();
        FpsProbe.startIfEnabled();                         // -Drebusque.fps=true: FPS reales por escena en stderr

        // Rendimiento: mientras el telón tape todo, la interfaz NO se dibuja. JavaFX no descarta nodos tapados; como la
        // lluvia ensucia la pantalla entera en cada frame, la UI de abajo se repintaría completa 60 veces por segundo.
        uiLayer.setVisible(false);

        // Frame 0: la cinemática empieza en este mismo pulso. En su flash final se desvanece el telón entero
        // y, en el mismo milisegundo, El Guachimán se "sella" en su marco del panel derecho.
        UndergroundIntro.play(introCanvas, audio, () -> {
            uiLayer.setVisible(true);
            view.stampSpider();
            FadeTransition lift = new FadeTransition(Duration.millis(400), curtain);   // de golpe, con el drop
            lift.setToValue(0);
            lift.setOnFinished(e -> {
                root.getChildren().remove(curtain);
                FpsProbe.scene("menu");
                view.requestFocus();
                view.introFinished();
            });
            lift.play();
        });
    }

    /**
     * Icono de la ventana y de la barra de tareas. Sin esto JavaFX pone la taza de café de Java (el .exe de jpackage
     * solo cambia el icono del ARCHIVO, no el de la ventana que abre la JVM).
     * <p>
     * javafx.scene.image.Image NO lee .ico (solo PNG/JPG/GIF/BMP): por eso se cargan los PNG que van DENTRO de
     * icono.ico (mismo arte), en varios tamaños para que Windows elija el adecuado (16 barra de título, 32/48 barra de
     * tareas y Alt+Tab, 256 pantallas HiDPI). Los diálogos nativos (FileChooser) se abren con esta ventana como dueña
     * y por eso heredan el mismo icono.
     */
    static void setIcons(Stage stage) {
        for (int size : new int[]{16, 32, 48, 64, 128, 256}) {
            try (var in = Main.class.getResourceAsStream("/images/icono-" + size + ".png")) {
                if (in != null) stage.getIcons().add(new Image(in));
            } catch (java.io.IOException ignored) { /* sin icono no se rompe nada */ }
        }
    }

    /** Contenedor que escala uniformemente un lienzo lógico de 1280x720 al tamaño de la ventana (con bandas negras). */
    private static Pane letterbox(Region content) {
        return new Pane(content) {
            @Override protected void layoutChildren() {
                double s = Math.min(getWidth() / MainView.W, getHeight() / MainView.H);
                content.resize(MainView.W, MainView.H);
                content.setScaleX(s);
                content.setScaleY(s);
                content.relocate((getWidth() - MainView.W) / 2, (getHeight() - MainView.H) / 2);
            }
        };
    }

    @Override
    public void stop() {
        if (Boolean.getBoolean("rebusque.debug")) System.err.printf("[salir %d ms] Application.stop()%n", System.currentTimeMillis() % 100_000);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
