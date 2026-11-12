package app.ui;

import app.animations.GlitchText;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.util.Duration;

import java.util.Random;

/**
 * Botón con glitch al pasar el mouse: invierte colores (clase CSS "glitch") y corrompe
 * el texto durante unos milisegundos, con jitter horizontal. Respeta Settings.glitch.
 */
public class GlitchButton extends Button {

    private static final Random R = new Random();
    private String baseText;
    private Timeline running;

    public GlitchButton(String text) {
        super(text);
        this.baseText = text;
        getStyleClass().add("ar-button");
        setMinWidth(USE_PREF_SIZE);
        setFocusTraversable(false);
        setOnMouseEntered(e -> glitch());
    }

    public void setBaseText(String t) {
        this.baseText = t;
        setText(t);
    }

    public String baseText() { return baseText; }

    public void glitch() {
        if (!Settings.get().glitch.get() || isDisabled()) return;
        if (running != null) running.stop();
        running = new Timeline();
        int frames = 7;
        for (int i = 0; i < frames; i++) {
            final boolean last = i == frames - 1;
            running.getKeyFrames().add(new KeyFrame(Duration.millis(i * 28), e -> {
                if (last) {
                    setText(baseText);
                    setTranslateX(0);
                    getStyleClass().remove("glitch");
                } else {
                    setText(GlitchText.corrupt(baseText, 0.28));
                    setTranslateX(R.nextInt(5) - 2);
                    if (!getStyleClass().contains("glitch") && R.nextBoolean()) getStyleClass().add("glitch");
                    else getStyleClass().remove("glitch");
                }
            }));
        }
        running.play();
    }
}
