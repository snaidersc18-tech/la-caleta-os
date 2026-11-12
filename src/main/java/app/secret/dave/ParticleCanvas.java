package app.secret.dave;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Partículas en UN solo Canvas (chispas, esquirlas, estrellas, corazones). */
public final class ParticleCanvas extends Canvas {
    public enum Kind { SPARK, SHARD, STAR, HEART }

    private static final class P { double x, y, vx, vy, g, life, max, size, rot, vr; Kind k; Color c; }

    private final List<P> ps = new ArrayList<>();
    private final Random r = new Random();
    private long last;
    private final AnimationTimer timer = new AnimationTimer() {
        @Override public void handle(long now) {
            double dt = last == 0 ? 0.016 : Math.min(0.05, (now - last) / 1e9);
            last = now;
            step(dt);
        }
    };

    public ParticleCanvas() {
        super(Cine.W, Cine.H);
        setMouseTransparent(true);
        timer.start();
    }

    public void stop() { timer.stop(); ps.clear(); }

    public void burst(double x, double y, int n, Kind k, double speed, double gravity, double life, double size, Color... cols) {
        for (int i = 0; i < n; i++) {
            P p = new P();
            double a = r.nextDouble() * Math.PI * 2, s = speed * (0.35 + r.nextDouble() * 0.65);
            p.x = x; p.y = y; p.vx = Math.cos(a) * s; p.vy = Math.sin(a) * s; p.g = gravity;
            p.max = p.life = life * (0.6 + r.nextDouble() * 0.6);
            p.size = size * (0.5 + r.nextDouble()); p.k = k; p.c = cols[r.nextInt(cols.length)];
            p.rot = r.nextDouble() * 360; p.vr = (r.nextDouble() - 0.5) * 400;
            ps.add(p);
        }
    }

    /** Corazones que suben en "gravedad cero" desde el borde inferior. */
    public void floatHearts(int n, Color... cols) {
        for (int i = 0; i < n; i++) {
            P p = new P();
            p.x = r.nextDouble() * Cine.W; p.y = Cine.H + 20 + r.nextDouble() * 120;
            p.vx = (r.nextDouble() - 0.5) * 30; p.vy = -(40 + r.nextDouble() * 70); p.g = -4;
            p.max = p.life = 7 + r.nextDouble() * 3; p.size = 10 + r.nextDouble() * 22;
            p.k = Kind.HEART; p.c = cols[r.nextInt(cols.length)]; p.rot = (r.nextDouble() - 0.5) * 30; p.vr = (r.nextDouble() - 0.5) * 20;
            ps.add(p);
        }
    }

    private void step(double dt) {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, Cine.W, Cine.H);
        for (int i = ps.size() - 1; i >= 0; i--) {
            P p = ps.get(i);
            p.life -= dt;
            if (p.life <= 0) { ps.remove(i); continue; }
            p.vy += p.g * dt * 60 * (p.g < 0 ? 0.02 : 1);
            p.x += p.vx * dt; p.y += p.vy * dt; p.rot += p.vr * dt;
            double a = Math.min(1, p.life / p.max * 2);
            g.save();
            g.translate(p.x, p.y); g.rotate(p.rot);
            g.setGlobalAlpha(a);
            g.setFill(p.c);
            double s = p.size;
            switch (p.k) {
                case SPARK -> g.fillOval(-s / 2, -s / 2, s, s);
                case SHARD -> g.fillPolygon(new double[]{-s, s, 0}, new double[]{-s / 2, -s / 3, s}, 3);
                case STAR -> g.fillPolygon(new double[]{0, s * 0.25, s, s * 0.25, 0, -s * 0.25, -s, -s * 0.25},
                        new double[]{-s, -s * 0.25, 0, s * 0.25, s, s * 0.25, 0, -s * 0.25}, 8);
                case HEART -> {
                    g.beginPath();
                    g.moveTo(0, s * 0.35);
                    g.bezierCurveTo(-s, -s * 0.3, -s * 0.5, -s, 0, -s * 0.4);
                    g.bezierCurveTo(s * 0.5, -s, s, -s * 0.3, 0, s * 0.35);
                    g.fill();
                }
            }
            g.restore();
        }
    }
}
