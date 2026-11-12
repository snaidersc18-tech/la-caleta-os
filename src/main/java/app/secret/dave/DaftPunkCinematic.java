package app.secret.dave;

import app.secret.audio.MusicTrack;
import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.effect.Glow;
import javafx.scene.paint.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Egg 1 — Araña-mecha de escenario: pisada (andamio + cable + plug) en panel diagonal, close-up al visor con ecualizador,
 * parpadeo rojo/azul y DROP. Anclado a "Harder, Better, Faster, Stronger": el DROP cae cuando entran bombo y bajo,
 * el THUD un número entero de beats antes, y el texto entra palabra por palabra en los golpes.
 */
final class DaftPunkCinematic {
    private DaftPunkCinematic() {}

    static Animation build(CineStage s) {
        Cine.Panel a = new Cine.Panel(40, 50, 640, 620, 90);
        Rectangle floor = new Rectangle(0, 470, 700, 200); floor.setFill(Color.web("#10131c"));
        Line horizon = new Line(0, 470, 700, 470); horizon.setStroke(Color.web("#00e5ff")); horizon.setStrokeWidth(3);
        Group leg = MechaSpiderArt.bigLeg();
        leg.setLayoutX(260); leg.setLayoutY(-120);
        a.put(new Rectangle(700, 700, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1a0b2e")), new Stop(1, Color.web("#06060c")))), floor, horizon, leg);

        Cine.Panel b = new Cine.Panel(600, 110, 640, 500, 90);
        Rectangle visorBg = new Rectangle(0, 0, 700, 500); visorBg.setFill(Color.web("#0b0d14"));
        Rectangle visor = new Rectangle(70, 130, 560, 200); visor.setArcWidth(60); visor.setArcHeight(60);
        visor.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1d2440")), new Stop(1, Color.BLACK)));
        visor.setStroke(Color.web("#c9921a")); visor.setStrokeWidth(10);
        EqBars eq = new EqBars(24, 14, 150, 8);
        eq.setLayoutX(120); eq.setLayoutY(155);
        Group visorG = new Group(visorBg, visor, eq);
        b.put(visorG);
        s.keep(eq.timeline());

        Text thudText = Cine.sfx("THUD!", 96, Color.web("#ffe600"), 360, 560, -8);

        MechaSpiderArt spider = new MechaSpiderArt();
        spider.setTranslateX(Cine.W / 2); spider.setTranslateY(Cine.H / 2 - 20);
        spider.setOpacity(0);
        Spotlight spot = new Spotlight(); spot.radius.set(520); spot.dark.set(0);
        SpeedLines lines = new SpeedLines(Cine.W / 2, Cine.H / 2, 70, Color.color(1, 1, 1, 0.8));
        ParticleCanvas fx = new ParticleCanvas();
        s.onDispose(fx::stop); s.onDispose(spider::stop); s.onDispose(lines::stop);
        s.world.getChildren().addAll(spider);
        s.hud.getChildren().addAll(lines, fx);

        // (HARDER/BETTER/FASTER/STRONGER aparecen luego, cuando las canta el vocoder: ver VocalHud)
        Text title = new Text(">>> HUMAN... AFTER... ALL... ENCALETADO");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 30)); title.setFill(Color.web("#ffe600"));
        title.setStroke(Color.BLACK); title.setStrokeWidth(1.4);
        title.setX(640 - 420); title.setY(660); title.setEffect(new Glow(0.8));
        Text typed = new Text(); typed.setFont(title.getFont()); typed.setFill(title.getFill()); typed.setStroke(Color.BLACK);
        typed.setX(title.getX()); typed.setY(title.getY()); typed.setEffect(new Glow(0.8));
        // El texto entra palabra por palabra EN los golpes (el guion está anclado al audio y el DROP cae en un beat):
        // cada palabra se teclea en los primeros 130 ms de su beat.
        double beat = MusicTrack.DAFT_PUNK_THEME.beatMs();
        Timeline type = new Timeline();
        String full = title.getText();
        String[] words = {">>> HUMAN...", " AFTER...", " ALL...", " ENCALETADO"};
        int shown = 0;
        for (int w = 0; w < words.length; w++) {
            int from = shown, to = shown + words[w].length();
            for (int c = from + 1; c <= to; c++) {
                int n = c;
                double at = (w + 2) * beat + 130.0 * (c - from) / (to - from);   // 2 golpes tras el DROP
                type.getKeyFrames().add(new KeyFrame(Duration.millis(Math.max(1, at)), e -> typed.setText(full.substring(0, n))));
            }
            shown = to;
        }

        Color red = Color.web("#d80027"), blue = Color.web("#0057ff");
        SequentialTransition flick = new SequentialTransition();
        for (int i = 0; i < 6; i++) {
            FillTransition f = new FillTransition(Duration.millis(85), s.bg, i % 2 == 0 ? red : blue, i % 2 == 0 ? blue : red);
            flick.getChildren().add(f);
        }

        Animation boom = new ParallelTransition(Cine.flash(s.hud, Color.WHITE, 140), Cine.rings(s.hud, Cine.W / 2, Cine.H / 2, 4, Color.web("#00e5ff"), 9),
                Cine.shake(s.world, 420, 18), Cine.pop(s.hud, Cine.sfx("DROP!", 140, Color.web("#ff2bd6"), 640, 190, -6), 380),
                Cine.scale(s.world, 520, 1.45, 1.0, Cine.SOFT));
        SequentialTransition drop = new SequentialTransition(
                Cine.cue(() -> { spot.dark.set(0.0); s.bg.setFill(Color.web("#0a0414")); s.hud.getChildren().add(0, spot); spider.setOpacity(1); spider.setScaleX(0.2); spider.setScaleY(0.2); }),
                new ParallelTransition(Cine.scale(spider, 420, 0.2, 1.0, Cine.OVERSHOOT), Cine.fade(spider, 200, 0, 1)),
                Cine.cue(() -> spider.idle()),
                Cine.hold(220),
                // DROP: agacharse, golpe de cámara brutal, ondas, líneas de velocidad, chispas
                new ParallelTransition(spider.drop(), Cine.scale(s.world, 140, 1.0, 1.45, Cine.SLAM)),
                Cine.cue(() -> {
                    lines.setOpacity(1); lines.start();
                    fx.burst(Cine.W / 2, Cine.H / 2, 90, ParticleCanvas.Kind.SPARK, 620, 0.2, 0.9, 10, Color.web("#00e5ff"), Color.web("#ff2bd6"), Color.web("#ffe600"));
                    fx.burst(Cine.W / 2, Cine.H / 2, 30, ParticleCanvas.Kind.STAR, 420, 0.1, 1.1, 18, Color.WHITE, Color.web("#f5c542"));
                }),
                Cine.cue(() -> { s.hud.getChildren().add(typed); type.play(); }),   // arranca EN el impacto
                boom,
                Cine.hold(Math.max(0, (words.length + 2) * beat + 250 - boom.getTotalDuration().toMillis())),
                Cine.fade(lines, 300, 1, 0)
        );

        Animation thud = new ParallelTransition(Cine.shake(a, 260, 16), Cine.pop(s.hud, thudText, 200),
                Cine.rings(s.hud, 330, 470, 2, Color.web("#00e5ff"), 5));
        PauseTransition pad = new PauseTransition(Duration.millis(220));
        SequentialTransition main = new SequentialTransition(
                Cine.hold(250),
                // Panel A: pisada
                Cine.cue(() -> s.hud.getChildren().add(a)),
                a.slideIn(-700, 120, 190),
                Cine.move(leg, 260, 260, -120, 260, 40, Cine.SLAM),
                thud,
                // Panel B: visor + ecualizador (zoom lento)
                Cine.cue(() -> { s.hud.getChildren().add(b); eq.timeline().play(); }),
                new ParallelTransition(b.slideIn(700, -140, 170), Cine.scale(visorG, 700, 1.0, 1.18, Cine.SOFT)),
                pad,
                // parpadeo rojo/azul a ritmo de música electrónica
                new ParallelTransition(flick, a.slideOut(-700, 0, 400), b.slideOut(700, 0, 400)),
                Cine.cue(() -> { s.hud.getChildren().removeAll(a, b); eq.timeline().stop(); }),
                drop
        );

        // ---- RHYTHM SYNC: el golpe "DROP!" cae en el instante en que entran bombo y bajo (19.630 s), y el "THUD!"
        // de la pisada se corre con la pausa del panel B hasta quedar un número ENTERO de beats antes: los dos golpes
        // visuales suenan sobre golpes reales de la canción.
        double impact = DaveTheDiverAnimations.Playback.offsetOf(main, drop) + DaveTheDiverAnimations.Playback.offsetOf(drop, boom);
        double gap = impact - DaveTheDiverAnimations.Playback.offsetOf(main, thud);
        double extra = (beat - gap % beat) % beat;
        pad.setDuration(Duration.millis(220 + extra));
        s.syncImpact(MusicTrack.DAFT_PUNK_THEME, impact + extra);
        return main;
    }
}
