package app.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.util.Duration;

/**
 * Bloqueo total de entrada durante la animación: un panel transparente que se come
 * el ratón + un filtro de teclado en la escena. Muestra un letrero parpadeante.
 */
public final class InputLock extends StackPane {

    private final Label banner = new Label();
    private final Timeline blink;
    private boolean locked;

    public InputLock() {
        setVisible(false);
        setPickOnBounds(true);
        setCursor(Cursor.CROSSHAIR);
        banner.getStyleClass().add("ar-lock-banner");
        StackPane.setAlignment(banner, Pos.TOP_CENTER);
        banner.setTranslateY(30);
        getChildren().add(banner);
        blink = new Timeline(new KeyFrame(Duration.millis(380), e -> banner.setOpacity(banner.getOpacity() > 0.5 ? 0.25 : 1)));
        blink.setCycleCount(Animation.INDEFINITE);

        sceneProperty().addListener((o, old, sc) -> {
            if (sc != null) sc.addEventFilter(KeyEvent.ANY, e -> { if (locked) e.consume(); });
        });
    }

    public void lock(String message) {
        locked = true;
        banner.setText(message);
        banner.setVisible(true);
        setVisible(true);
        blink.play();
        Scene sc = getScene();
        if (sc != null && sc.getRoot() != null) sc.getRoot().requestFocus();
    }

    public void unlock() {
        locked = false;
        blink.stop();
        setVisible(false);
    }

    public boolean isLocked() { return locked; }

    /** Oculta el letrero sin soltar el bloqueo (p. ej. para no tapar una cinemática a pantalla completa). */
    public void hideBanner() {
        blink.stop();
        banner.setVisible(false);
    }
}
