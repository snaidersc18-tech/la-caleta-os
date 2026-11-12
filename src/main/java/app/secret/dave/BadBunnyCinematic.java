package app.secret.dave;

import app.secret.audio.MusicTrack;
import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.Glow;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Random;

/**
 * Egg 2 — Verano sin descifrar: estática, spotlight a la silueta del Corazón Triste tropical, tres franjas, estallido de
 * atardecer que se corrompe con un glitch RGB. Sin ambiente: solo la canción oficial, con el estallido en su drop.
 */
final class BadBunnyCinematic {
    private BadBunnyCinematic() {}

    private static final Random R = new Random();

    static Animation build(CineStage s) {
        // 1) Estática: barras horizontales blancas/negras
        Group bars = new Group();
        Timeline stat = new Timeline(new KeyFrame(Duration.millis(60), e -> {
            bars.getChildren().clear();
            double y = 0;
            while (y < Cine.H) {
                double h = 6 + R.nextDouble() * 60;
                Rectangle r = new Rectangle(0, y, Cine.W, h); r.setFill(R.nextBoolean() ? Color.WHITE : Color.BLACK);
                r.setOpacity(0.55 + R.nextDouble() * 0.45);
                bars.getChildren().add(r);
                y += h;
            }
        }));
        stat.setCycleCount(14);
        s.hud.getChildren().add(bars);

        // 2) Spotlight + silueta
        TropicalHeartArt zombie = new TropicalHeartArt();
        zombie.setTranslateX(Cine.W / 2); zombie.setTranslateY(Cine.H / 2 + 10); zombie.setScaleX(1.5); zombie.setScaleY(1.5);
        zombie.silhouette(true); zombie.setOpacity(0);
        Spotlight spot = new Spotlight();
        spot.dark.set(0.96); spot.radius.set(10); spot.setOpacity(0);
        s.world.getChildren().add(zombie);
        s.hud.getChildren().add(0, spot);

        // 3) Tres franjas verticales
        double sw = 410, gap = 12, top = 40, ph = 640;
        Cine.Panel p1 = new Cine.Panel(24, top, sw, ph, 60);
        Cine.Panel p2 = new Cine.Panel(24 + sw + gap, top, sw, ph, 60);
        Cine.Panel p3 = new Cine.Panel(24 + 2 * (sw + gap), top, sw, ph, 60);
        for (Cine.Panel p : new Cine.Panel[]{p1, p2, p3}) p.put(new Rectangle(sw + 40, ph + 20, Color.web("#140a1f")));

        SunglassesArt glasses = new SunglassesArt();
        glasses.setScaleX(0.8); glasses.setScaleY(0.8); glasses.setLayoutX(26); glasses.setLayoutY(250);
        p1.put(glasses);

        Group rain = new Group();
        Timeline rainT = new Timeline();
        for (int c = 0; c < 18; c++) {
            Group col = new Group();
            for (int r = 0; r < 14; r++) {
                Text t = new Text(R.nextBoolean() ? "1" : "0");
                t.setFont(Font.font("Monospaced", FontWeight.BOLD, 26));
                t.setFill(R.nextBoolean() ? Color.web("#ff9f1c") : Color.web("#00e5ff"));
                t.setOpacity(1.0 - r * 0.06); t.setY(r * 30); col.getChildren().add(t);
            }
            col.setLayoutX(c * 24 + 4);
            rain.getChildren().add(col);
            rainT.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO, new KeyValue(col.translateYProperty(), -R.nextInt(500))),
                    new KeyFrame(Duration.millis(900 + R.nextInt(500)), new KeyValue(col.translateYProperty(), 720)));
        }
        rainT.setCycleCount(Animation.INDEFINITE);
        s.keep(rainT);
        p2.put(rain);

        TropicalHeartArt grin = new TropicalHeartArt();
        grin.grin(true); grin.setScaleX(1.5); grin.setScaleY(1.5); grin.setLayoutX(sw / 2 + 10); grin.setLayoutY(310);
        grin.faceOnly().setTranslateY(-70);
        p3.put(grin);

        ParticleCanvas fx = new ParticleCanvas();
        s.onDispose(fx::stop);
        s.hud.getChildren().add(fx);

        // 4) Estallido cálido + glitch
        Color sunset = Color.web("#ff6a3d");
        Node splitR = TropicalHeartArt.splitCopy(Color.web("#ff0040")), splitC = TropicalHeartArt.splitCopy(Color.web("#00e5ff"));
        Group splitG = new Group(splitR, splitC); splitG.setOpacity(0);
        splitG.setTranslateX(Cine.W / 2); splitG.setTranslateY(Cine.H / 2 + 10); splitG.setScaleX(1.5); splitG.setScaleY(1.5);
        s.world.getChildren().add(splitG);

        Text title = new Text("UN VERANO SIN DESCIFRAR");
        title.setFont(Font.font("Impact", FontWeight.BOLD, 70)); title.setFill(Color.web("#fff3e0"));
        title.setStroke(Color.BLACK); title.setStrokeWidth(4); title.setEffect(new Glow(0.6)); title.setOpacity(0);
        title.setX(Cine.W / 2 - title.getLayoutBounds().getWidth() / 2); title.setY(Cine.H - 60);
        s.hud.getChildren().add(title);

        Timeline glitch = new Timeline();
        for (int i = 0; i < 12; i++) {
            double dx = (R.nextDouble() - 0.5) * 50;
            glitch.getKeyFrames().add(new KeyFrame(Duration.millis(i * 55), e -> {
                splitR.setTranslateX(dx); splitC.setTranslateX(-dx); splitR.setTranslateY(dx / 4); splitC.setTranslateY(-dx / 4);
                bars.getChildren().clear();
                for (int k = 0; k < 4; k++) {
                    Rectangle r = new Rectangle(0, R.nextDouble() * Cine.H, Cine.W, 6 + R.nextDouble() * 30); r.setFill(R.nextBoolean() ? Color.web("#00e5ff") : Color.web("#ff0040"));
                    r.setOpacity(0.7); bars.getChildren().add(r);
                }
            }));
        }
        glitch.getKeyFrames().add(new KeyFrame(Duration.millis(12 * 55), e -> bars.getChildren().clear()));

        PauseTransition pad = new PauseTransition(Duration.millis(150));
        Animation shatter = new ParallelTransition(glasses.shatter(), Cine.shake(p1, 380, 10), Cine.hold(380));
        Animation burst = new ParallelTransition(Cine.flash(s.hud, Color.web("#ffd166"), 260), Cine.shake(s.world, 380, 14),
                Cine.rings(s.hud, Cine.W / 2, Cine.H / 2, 3, Color.web("#ff9f1c"), 8));
        SequentialTransition main = new SequentialTransition(
                Cine.cue(() -> stat.play()),
                Cine.hold(14 * 60 + 40),
                Cine.cue(() -> { bars.getChildren().clear(); spot.setOpacity(1); zombie.setOpacity(1); }),
                new ParallelTransition(new Timeline(new KeyFrame(Duration.millis(900),
                        new KeyValue(spot.radius, 330, Cine.SOFT))), Cine.scale(zombie, 900, 1.35, 1.5, Cine.SOFT)),
                pad,
                // 3 franjas en cortes secos
                Cine.cue(() -> { s.hud.getChildren().addAll(p1, p2, p3); rainT.play(); }),
                p1.slideIn(0, -760, 150),
                shatter,
                p2.slideIn(0, 760, 150),
                p3.slideIn(0, -760, 150),
                new ParallelTransition(Cine.scale(grin, 600, 1.5, 1.72, Cine.SOFT), Cine.hold(650)),
                // Revelación
                new ParallelTransition(p1.slideOut(-500, 0, 200), p2.slideOut(0, 600, 200), p3.slideOut(500, 0, 200)),
                Cine.cue(() -> { s.hud.getChildren().removeAll(p1, p2, p3); rainT.stop(); zombie.silhouette(false); zombie.grin(true); spot.setOpacity(0); zombie.setScaleX(2.8); zombie.setScaleY(2.8); }),
                new ParallelTransition(Cine.scale(zombie, 700, 2.8, 0.95, Cine.SOFT),
                        new Timeline(new KeyFrame(Duration.millis(5), e -> s.bg.setFill(sunset)))),
                Cine.cue(() -> fx.burst(Cine.W / 2, Cine.H / 2, 140, ParticleCanvas.Kind.SPARK, 700, 0.3, 1.3, 12,
                        Color.web("#ff9f1c"), Color.web("#ff5d8f"), Color.web("#ffd166"), Color.web("#ff6a3d"))),
                // EL DROP: destello cálido + temblor + ondas justo cuando el grave explota
                burst,
                Cine.hold(280),
                // corrupción cibernética
                Cine.cue(() -> splitG.setOpacity(1)),
                glitch,
                Cine.cue(() -> splitG.setOpacity(0)),
                Cine.fade(title, 260, 0, 1),
                Cine.hold(800)
        );

        // ---- RHYTHM SYNC con la canción oficial: el destello del atardecer cae EXACTO en el drop (17.912 s, compás 8)
        // y las gafas estallan un número entero de beats antes (se ajusta la pausa tras el spotlight).
        double beat = MusicTrack.BAD_BUNNY_THEME.beatMs();
        double impact = DaveTheDiverAnimations.Playback.offsetOf(main, burst);
        double hitGap = impact - DaveTheDiverAnimations.Playback.offsetOf(main, shatter);
        double extra = (beat - hitGap % beat) % beat;
        pad.setDuration(Duration.millis(150 + extra));
        s.syncImpact(MusicTrack.BAD_BUNNY_THEME, impact + extra);
        return main;
    }
}
