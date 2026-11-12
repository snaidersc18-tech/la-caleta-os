package app.secret.view;

import app.secret.dave.MechaSpiderArt;
import app.secret.dave.TropicalHeartArt;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * "El Guachimán" evolucionado para las interfaces exclusivas. Ya NO superpone accesorios sobre la araña del núcleo:
 * cada skin es un cuerpo completo rediseñado (el mismo arte de las cinemáticas), centrado y escalado a la caja pedida.
 * <ul>
 *   <li>STAGE_MECHA: araña-mecha de escenario (andamios, cables con plug, casco plata/oro con PCB y LEDs).</li>
 *   <li>TROPICAL_HEART: Corazón Triste tropical-ciberpunk (duna de arena, palmeras de neón, streetwear).</li>
 * </ul>
 */
public final class MascotView extends Pane {

    public enum Skin { STAGE_MECHA, TROPICAL_HEART }

    private final Group art;
    private final Group holder = new Group();
    private Animation idle;

    public MascotView(Skin skin, double w, double h) {
        setPrefSize(w, h); setMinSize(w, h); setMaxSize(w, h);
        setPickOnBounds(false);
        if (skin == Skin.STAGE_MECHA) {
            MechaSpiderArt mecha = new MechaSpiderArt();
            mecha.idle();
            art = mecha;
        } else {
            TropicalHeartArt heart = new TropicalHeartArt();
            idle = heart.auraDrift();
            idle.play();
            art = heart;
        }
        holder.getChildren().add(art);
        getChildren().add(holder);
        fit(w, h);
    }

    /**
     * Encaje "contain": escala s = min(w/anchoArte, h/altoArte) para que quepa entero sin deformarse, y luego se
     * traslada para que el CENTRO de los bounds escalados coincida con el centro de la caja.
     */
    private void fit(double w, double h) {
        var b = art.getLayoutBounds();
        double s = Math.min(w / b.getWidth(), h / b.getHeight());
        holder.setScaleX(s); holder.setScaleY(s);
        // setScaleX escala alrededor del centro de los bounds del holder: basta con llevar ese centro al de la caja.
        holder.setLayoutX(w / 2 - (b.getMinX() + b.getWidth() / 2));
        holder.setLayoutY(h / 2 - (b.getMinY() + b.getHeight() / 2));
    }

    public void stop() {
        if (art instanceof MechaSpiderArt m) m.stop();
        if (idle != null) idle.stop();
    }

    /**
     * Mutación de entrada: 6 cuadros de glitch (tono e inversión de brillo alternados cada 60 ms) y aparición con
     * rebote elástico: escala 0.1 → 1.15 (EASE_OUT) → 1.0 (EASE_BOTH).
     */
    public void mutate() {
        ColorAdjust glitch = new ColorAdjust();
        holder.setEffect(glitch);
        Timeline flicker = new Timeline();
        for (int i = 0; i < 6; i++) {
            double hue = i % 2 == 0 ? 0.6 : -0.4, bright = i % 2 == 0 ? 0.3 : -0.2;
            flicker.getKeyFrames().add(new KeyFrame(Duration.millis(i * 60),
                    new KeyValue(glitch.hueProperty(), hue), new KeyValue(glitch.brightnessProperty(), bright)));
        }
        flicker.getKeyFrames().add(new KeyFrame(Duration.millis(360), e -> holder.setEffect(null)));

        setScaleX(0.1); setScaleY(0.1);
        ScaleTransition a = new ScaleTransition(Duration.millis(380), this);
        a.setToX(1.15); a.setToY(1.15); a.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition b = new ScaleTransition(Duration.millis(260), this);
        b.setToX(1.0); b.setToY(1.0); b.setInterpolator(Interpolator.EASE_BOTH);
        flicker.play();
        new SequentialTransition(a, b).play();
    }
}
