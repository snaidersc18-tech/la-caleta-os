package app.secret.dave;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Arte de referencia de cada modo (src/main/resources/art), cargado UNA vez y reutilizado por cinemáticas y vistas.
 * Los PNG ya vienen con el borde "emplumado" (alfa elíptico), así su fondo oscuro se funde con el de la app.
 * <p>
 * El nodo queda CENTRADO en su origen (0,0): las cinemáticas lo colocan con translateX/Y y lo escalan alrededor de
 * su centro, igual que hacían con el antiguo arte vectorial.
 */
public final class ArtSprite extends Group {

    public enum Art {
        SOFIA("sofia_portrait.png", Color.web("#ff9ac8"));

        final String file;
        public final Color glow;

        Art(String file, Color glow) { this.file = file; this.glow = glow; }
    }

    private static final Map<Art, Image> CACHE = new EnumMap<>(Art.class);

    private final Art art;
    private final ImageView view;
    private final DropShadow aura;
    private Timeline idle;

    /** @param width ancho en pantalla; el alto sale de la proporción de la imagen. */
    public ArtSprite(Art art, double width) {
        this.art = art;
        view = fitted(art, width);
        view.setX(-width / 2);
        view.setY(-view.getFitHeight() / 2);
        aura = new DropShadow(26, art.glow);
        aura.setSpread(0.12);
        view.setEffect(aura);
        getChildren().add(view);
    }

    /** Imagen cacheada. Image(InputStream) decodifica de forma síncrona: solo la primera llamada paga ese coste. */
    public static synchronized Image image(Art art) {
        return CACHE.computeIfAbsent(art, a -> {
            try (InputStream in = ArtSprite.class.getResourceAsStream("/art/" + a.file)) {
                if (in != null) return new Image(in);
            } catch (Exception ignored) { /* cae al marcador vacío */ }
            System.err.println("[art] Falta /art/" + a.file);
            return new WritableImage(1, 1);                     // nunca null: la cinemática sigue aunque falte el PNG
        });
    }

    /** ImageView a un ancho dado conservando proporción. smooth=false mantiene nítidos los píxeles del pixel art. */
    public static ImageView fitted(Art art, double width) {
        Image img = image(art);
        ImageView v = new ImageView(img);
        v.setPreserveRatio(true);
        v.setSmooth(false);
        v.setFitWidth(width);
        v.setFitHeight(width * img.getHeight() / Math.max(1, img.getWidth()));
        return v;
    }

    public Art art() { return art; }
    public ImageView view() { return view; }

    /**
     * Respiración: el cuerpo sube 8 px y el aura de neón pasa de radio 22 a 40 en 1.3 s, ida y vuelta (EASE_BOTH ≈
     * seno). Ambas propiedades viven en el mismo Timeline, así el "latido" de luz va pegado al movimiento.
     */
    public void idle() {
        stop();
        idle = new Timeline(
                new KeyFrame(javafx.util.Duration.ZERO, new KeyValue(view.translateYProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(aura.radiusProperty(), 22, Interpolator.EASE_BOTH)),
                new KeyFrame(javafx.util.Duration.millis(1300), new KeyValue(view.translateYProperty(), -8, Interpolator.EASE_BOTH),
                        new KeyValue(aura.radiusProperty(), 40, Interpolator.EASE_BOTH)));
        idle.setAutoReverse(true);
        idle.setCycleCount(Animation.INDEFINITE);
        idle.play();
    }

    public void stop() { if (idle != null) idle.stop(); }

    /** Golpe de "drop": se aplasta a 0.86 con SLAM (lento → brusco) y rebota a 1.0 con OVERSHOOT. */
    public Animation drop() {
        return new SequentialTransition(Cine.scale(this, 90, 1, 0.86, Cine.SLAM), Cine.scale(this, 260, 0.86, 1.0, Cine.OVERSHOOT));
    }

    /**
     * Contraluz: ColorAdjust(brillo −0.92) apaga casi todo el color y el DropShadow encadenado (con esa entrada)
     * dibuja el borde de luz naranja alrededor. off = vuelve el aura de neón normal.
     */
    public void silhouette(boolean on) {
        if (on) {
            DropShadow rim = new DropShadow(22, Color.web("#ff9f1c"));
            rim.setSpread(0.3);
            rim.setInput(new ColorAdjust(0, -0.4, -0.92, 0));
            view.setEffect(rim);
        } else view.setEffect(aura);
    }
}
