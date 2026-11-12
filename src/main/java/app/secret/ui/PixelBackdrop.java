package app.secret.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;

/**
 * Escena pixel art animada de fondo: PLAYA (delfines que saltan) o ESCENARIO DE PIRÁMIDE (pantallas LED que
 * parpadean). Capa base estática + capa animada, ambas a escala entera (sin suavizado).
 */
public final class PixelBackdrop extends StackPane {

    public enum Kind { BEACH, PYRAMID }

    private final Kind kind;
    private final int gw, gh;
    private final double px;
    private final Canvas base, anim;
    private PyramidStage.Result pyramid;
    private final Color[] led = new Color[8];
    private Timeline loop;
    private int tick;

    public static PixelBackdrop beach(int gw, int gh, double px) { return new PixelBackdrop(Kind.BEACH, gw, gh, px); }
    public static PixelBackdrop pyramid(int gw, int gh, double px) { return new PixelBackdrop(Kind.PYRAMID, gw, gh, px); }

    private PixelBackdrop(Kind kind, int gw, int gh, double px) {
        this.kind = kind; this.gw = gw; this.gh = gh; this.px = px;
        double w = gw * px, h = gh * px;
        setPrefSize(w, h); setMinSize(w, h); setMaxSize(w, h);
        setMouseTransparent(true);
        base = new Canvas(w, h);
        anim = new Canvas(w, h);
        getChildren().addAll(base, anim);
        for (int i = 0; i < 8; i++) led[i] = Color.web(String.format("#%06x",
                (i & 1) == 0 ? PyramidStage.LED_ON[i >> 1] : PyramidStage.LED_DIM[i >> 1]));
        drawBase();
        drawAnim();
    }

    private void drawBase() {
        GraphicsContext g = base.getGraphicsContext2D();
        if (kind == Kind.BEACH) {
            PixelArt.draw(g, AlbumBeach.background(gw, gh).g, AlbumBeach.PALETTE, 0, 0, px);
        } else {
            pyramid = PyramidStage.build(gw, gh);
            PixelArt.draw(g, pyramid.base.g, PyramidStage.PALETTE, 0, 0, px);
        }
    }

    private void drawAnim() {
        GraphicsContext g = anim.getGraphicsContext2D();
        g.clearRect(0, 0, anim.getWidth(), anim.getHeight());
        if (kind == Kind.BEACH) {
            PixelArt.draw(g, AlbumBeach.dolphins(gw, gh, (tick % 48) / 48.0).g, AlbumBeach.PALETTE, 0, 0, px);
        } else {
            List<List<int[]>> screens = pyramid.screens;
            for (int s = 0; s < screens.size(); s++)
                for (int[] c : screens.get(s)) {
                    g.setFill(led[PyramidStage.ledIndex(s, c[0], c[1], tick)]);
                    g.fillRect(c[0] * px, c[1] * px, px, px);
                }
        }
    }

    public void start() {
        stop();
        loop = new Timeline(new KeyFrame(Duration.millis(kind == Kind.BEACH ? 140 : 120), e -> { tick++; drawAnim(); }));
        loop.setCycleCount(Animation.INDEFINITE);
        loop.play();
    }

    public void stop() { if (loop != null) { loop.stop(); loop = null; } }
}
