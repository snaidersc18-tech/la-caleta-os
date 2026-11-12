package app.secret.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

/**
 * Ondas suaves dibujadas con el espectro REAL de la canción (ver {@link MusicReactor}): tres capas de curvas que suben
 * y bajan con graves (izquierda) y agudos (derecha). Las bandas se reparten en escala logarítmica, como las oye el oído,
 * y cada capa se mece con su propia fase lenta para que el movimiento sea orgánico y no un ecualizador de barras.
 * Un solo Canvas y ~150 segmentos de curva por frame: barato.
 */
public final class SpectrumWave extends Canvas {

    private static final int POINTS = 40;
    /** Bandas que se dibujan (de las 128 de AudioManager: 0 a ~11 kHz; más arriba casi no hay energía musical). */
    static final int USED_BANDS = 64;

    private final Color[] layers;
    private final double[] xs = new double[POINTS], ys = new double[POINTS];

    /** @param layers colores de las capas, del fondo hacia el frente. */
    public SpectrumWave(double w, double h, Color... layers) {
        super(w, h);
        this.layers = layers;
        setMouseTransparent(true);
    }

    /** Redibuja con las bandas suavizadas (0..1) y el tiempo {@code t} en segundos (fase de la mecedura). */
    void draw(float[] bands, double t) {
        double w = getWidth(), h = getHeight();
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        for (int k = 0; k < layers.length; k++) {
            double gain = 1 - k * 0.2, speed = 0.9 + k * 0.55, shift = k * 1.9;
            for (int i = 0; i < POINTS; i++) {
                double u = i / (POINTS - 1.0);
                // escala logarítmica: la mitad izquierda son graves y medios, donde la canción tiene casi toda la energía
                double pos = Math.pow(u, 1.8) * (USED_BANDS - 1);
                int b0 = (int) pos;
                double f = pos - b0, a = bands[b0] * (1 - f) + bands[Math.min(USED_BANDS - 1, b0 + 1)] * f;
                a = Math.pow(a, 1.3);                                 // contraste: el silencio queda casi plano
                double taper = Math.sqrt(Math.sin(Math.PI * u));     // los extremos se desvanecen hacia el borde
                double sway = 0.72 + 0.28 * Math.sin(u * 9 + t * speed + shift);
                xs[i] = u * w;
                ys[i] = h - 6 - (h - 14) * a * sway * gain * taper;
            }
            Color c = layers[k];
            g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, c.deriveColor(0, 1, 1, 0.55)), new Stop(1, c.deriveColor(0, 1, 1, 0))));
            g.beginPath();
            g.moveTo(0, h);
            g.lineTo(xs[0], ys[0]);
            curve(g);
            g.lineTo(w, h);
            g.closePath();
            g.fill();
            g.setStroke(c.deriveColor(0, 1, 1.15, 0.9));
            g.setLineWidth(2 - k * 0.4);
            g.setLineCap(StrokeLineCap.ROUND);
            g.beginPath();
            g.moveTo(xs[0], ys[0]);
            curve(g);
            g.stroke();
        }
    }

    /** Curva suave por los puntos: cada punto es control de una cuadrática que termina en el punto medio siguiente. */
    private void curve(GraphicsContext g) {
        for (int i = 1; i < POINTS - 1; i++) {
            g.quadraticCurveTo(xs[i], ys[i], (xs[i] + xs[i + 1]) / 2, (ys[i] + ys[i + 1]) / 2);
        }
        g.lineTo(xs[POINTS - 1], ys[POINTS - 1]);
    }
}
