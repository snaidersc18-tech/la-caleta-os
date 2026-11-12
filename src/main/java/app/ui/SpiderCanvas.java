package app.ui;

import app.animations.LoveParticles;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Random;

/**
 * Renderiza el sprite de la araña a escala entera (pixel-perfect, sin suavizado).
 * Un solo ticker de 70 ms mueve: patas (idle), latido de los corazones y glitch de filas.
 */
public final class SpiderCanvas extends Canvas {

    private final double px;
    private final int[][][] frames = {SpiderSprite.generate(0), SpiderSprite.generate(1)};
    private final int[] rowOffset = new int[SpiderSprite.H];
    private final Random rnd = new Random();
    private final Timeline ticker;

    private boolean love;
    private int tick;
    private long glitchUntil;

    public SpiderCanvas(double pixelSize) {
        super(SpiderSprite.W * pixelSize, SpiderSprite.H * pixelSize);
        this.px = pixelSize;
        ticker = new Timeline(new KeyFrame(Duration.millis(70), e -> step()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
        Theme.get().paletteProperty().addListener(paletteListener);
        redraw();
    }

    private final javafx.beans.value.ChangeListener<Theme.Palette> paletteListener = (o, a, b) -> redraw();

    /** Para sprites temporales (p. ej. el Mordisco): detiene el ticker y suelta el oyente del tema. */
    public void dispose() {
        ticker.stop();
        Theme.get().paletteProperty().removeListener(paletteListener);
    }

    public double pixelSize() { return px; }

    public void setLoveMode(boolean on) { this.love = on; redraw(); }

    /** Desplaza bandas de filas aleatoriamente durante {@code ms} milisegundos. */
    public void glitchFor(int ms) { glitchUntil = System.currentTimeMillis() + ms; }

    private void step() {
        tick++;
        if (System.currentTimeMillis() < glitchUntil) {
            java.util.Arrays.fill(rowOffset, 0);
            int bands = 2 + rnd.nextInt(3);
            for (int b = 0; b < bands; b++) {
                int y = rnd.nextInt(SpiderSprite.H), h = 1 + rnd.nextInt(6), off = rnd.nextInt(13) - 6;
                for (int i = y; i < Math.min(SpiderSprite.H, y + h); i++) rowOffset[i] = off;
            }
        } else if (rowOffset[0] != 0 || anyOffset()) {
            java.util.Arrays.fill(rowOffset, 0);
        }
        redraw();
    }

    private boolean anyOffset() {
        for (int v : rowOffset) if (v != 0) return true;
        return false;
    }

    private void redraw() {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());
        Theme.Palette p = Theme.get().palette();
        Color bone = p.fg();
        Color ink = p.bg();
        Color gray = love ? p.accent() : p.dim();
        Color white = love ? Color.web("#fff7fd") : Color.WHITE;

        int[][] grid = frames[(tick / 6) % 2];
        for (int y = 0; y < SpiderSprite.H; y++) {
            int off = rowOffset[y];
            int x = 0;
            while (x < SpiderSprite.W) {
                int v = grid[y][x];
                if (v == 0) { x++; continue; }
                int run = 1;
                while (x + run < SpiderSprite.W && grid[y][x + run] == v) run++;
                g.setFill(switch (v) {
                    case SpiderSprite.BONE -> bone;
                    case SpiderSprite.INK -> ink;
                    case SpiderSprite.GRAY -> gray;
                    default -> white;
                });
                g.fillRect((x + off) * px, y * px, run * px, px);
                x += run;
            }
        }
        if (love) drawHeartEyes(g, p);
    }

    private void drawHeartEyes(GraphicsContext g, Theme.Palette p) {
        double pulse = 1.0 + 0.28 * (0.5 + 0.5 * Math.sin(tick * 0.45));
        double unit = px * 0.9 * pulse;
        int[][] eyes = {SpiderSprite.EYE_L, SpiderSprite.EYE_R};
        for (int[] e : eyes) {
            double cx = e[0] * px, cy = e[1] * px;
            LoveParticles.drawHeart(g, cx - 3.5 * unit, cy - 3 * unit, unit, p.accent());
        }
    }
}
