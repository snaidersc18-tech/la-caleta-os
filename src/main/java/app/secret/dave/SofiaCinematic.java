package app.secret.dave;

import app.secret.audio.AudioManager;
import app.secret.audio.MusicTrack;
import app.secret.audio.SongCues;
import app.secret.ui.MusicReactor;
import app.secret.ui.SpectrumWave;
import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Random;

/**
 * Preludio SOFIA sobre su retrato de referencia: cielo nocturno, luz celestial, el retrato baja flotando como un
 * dibujo de luz (monocromo y brillante) y se "materializa" en color entre corazones y estrellas; aureola, nombre y
 * dedicatoria.
 * <p>
 * Coreografiado sobre "Algo Mágico": cada entrada es un cue de {@link SongCues} anclado al currentTime REAL del
 * MediaPlayer, en los golpes medidos de la intro. Mientras tanto un {@link MusicReactor} hace latir el retrato con los
 * graves, mueve las ondas del pie con el espectro y suelta corazones en cada bombo.
 */
final class SofiaCinematic {
    private SofiaCinematic() {}

    private static final double SIZE = 440;

    // Golpes MEDIDOS de "Algo Mágico" (ms dentro del MP3; rejilla de 99.75 BPM desde el primer acorde a 45 ms)
    private static final MusicTrack SONG = MusicTrack.SOFIA_THEME;
    private static final double CHORD_1 = 45;                     // primer acorde (beat 0)
    private static final double MELODY = 2_605;                   // entra la melodía
    private static final double CHORD_2 = 4_870;                  // cambio de acorde (beat 8)
    private static final double HALO = 7_865;                     // beat 13
    private static final double RISE = 8_820;                     // la subida (el volumen sube 17 dB)
    private static final double CHORD_3 = 9_670;                  // beat 16
    private static final double ENTRY = SONG.dropMs;              // entrada completa: 10.240 s

    /** Un nodo no puede ser clip de dos nodos a la vez: cada ImageView recibe su propio rectángulo. */
    static Rectangle roundedClip() {
        Rectangle r = new Rectangle(SIZE, SIZE);
        r.setArcWidth(64); r.setArcHeight(64);
        return r;
    }

    static Animation build(CineStage s) {
        Random r = new Random(3);
        s.bg.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#070a1c")), new Stop(1, Color.web("#2a1240"))));

        // Estrellas: cada una parpadea con periodo y desfase aleatorios para que el cielo no "respire" al unísono.
        Group stars = new Group();
        for (int i = 0; i < 80; i++) {
            Circle c = new Circle(r.nextDouble() * Cine.W, r.nextDouble() * Cine.H, 0.8 + r.nextDouble() * 2.0, Color.WHITE);
            c.setOpacity(0.2);
            FadeTransition f = new FadeTransition(Duration.millis(500 + r.nextInt(900)), c);
            f.setFromValue(0.1); f.setToValue(1); f.setAutoReverse(true); f.setCycleCount(Animation.INDEFINITE); f.setDelay(Duration.millis(r.nextInt(600)));
            s.keep(f); f.play();
            stars.getChildren().add(c);
        }
        s.world.getChildren().add(stars);

        // Luz celestial: un cono que baja desde fuera de cuadro, blanco cálido arriba → transparente abajo.
        Polygon beam = new Polygon(Cine.W / 2 - 70, -20, Cine.W / 2 + 70, -20, Cine.W / 2 + 330, Cine.H, Cine.W / 2 - 330, Cine.H);
        beam.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.96, 0.85, 0.55)), new Stop(1, Color.color(1, 0.8, 0.9, 0))));
        beam.setOpacity(0);
        Circle backlight = new Circle(Cine.W / 2, 300, 300, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.75, 0.88, 0.55)), new Stop(1, Color.color(1, 0.6, 0.8, 0))));
        backlight.setOpacity(0);

        // Retrato: dos ImageView de la MISMA imagen. Arriba el "dibujo de luz" (sin saturación, brillo +0.55, Glow),
        // debajo el color real. Ambos recortados por un rectángulo redondeado (el PNG trae esquinas negras).
        ImageView color = ArtSprite.fitted(ArtSprite.Art.SOFIA, SIZE);
        ImageView light = ArtSprite.fitted(ArtSprite.Art.SOFIA, SIZE);
        Glow lightGlow = new Glow(0.9);
        lightGlow.setInput(new ColorAdjust(0, -1, 0.55, 0.25));    // (tono, saturación, brillo, contraste)
        light.setEffect(lightGlow);
        color.setClip(roundedClip());
        light.setClip(roundedClip());
        color.setOpacity(0);
        light.setOpacity(0);
        Rectangle frame = new Rectangle(-14, -14, SIZE + 28, SIZE + 28);
        frame.setArcWidth(70); frame.setArcHeight(70);
        frame.setFill(Color.web("#140a26", 0.85));
        frame.setStroke(Color.web("#ffe08a")); frame.setStrokeWidth(4);
        Group portrait = new Group(frame, color, light);
        portrait.setEffect(new DropShadow(46, Color.web("#ff9ac8")));
        portrait.setLayoutX(Cine.W / 2 - SIZE / 2);
        portrait.setLayoutY(64);
        portrait.setOpacity(0);

        SofiaArt.Halo halo = SofiaArt.halo(130, 0.18, 10);
        halo.setLayoutX(Cine.W / 2);
        halo.setLayoutY(24);
        halo.setOpacity(0);
        RotateTransition haloSpin = s.keep(new RotateTransition(Duration.seconds(9), halo.orbit));
        haloSpin.setByAngle(360); haloSpin.setCycleCount(Animation.INDEFINITE); haloSpin.setInterpolator(Interpolator.LINEAR);

        // Flotación tras materializarse: seno suave de ±6 px (EASE_BOTH ≈ curva sinusoidal entre extremos).
        TranslateTransition breathe = s.keep(new TranslateTransition(Duration.millis(1800), portrait));
        breathe.setFromY(0); breathe.setToY(-6); breathe.setAutoReverse(true); breathe.setCycleCount(Animation.INDEFINITE);
        breathe.setInterpolator(Interpolator.EASE_BOTH);

        ParticleCanvas fx = new ParticleCanvas();
        s.onDispose(fx::stop);
        Timeline hearts = s.keep(new Timeline(new KeyFrame(Duration.millis(480),   // ambiente; los bombos sueltan más
                e -> fx.floatHearts(1, Color.web("#ff5d8f"), Color.web("#ffb3c7"), Color.web("#ffd166"), Color.WHITE))));
        hearts.setCycleCount(Animation.INDEFINITE);

        Text name = new Text("Sofía");
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 96)); name.setFill(Color.WHITE);
        name.setStroke(Color.web("#c2185b")); name.setStrokeWidth(3); name.setEffect(new Glow(0.8));
        name.setX(Cine.W / 2 - name.getLayoutBounds().getWidth() / 2); name.setY(640); name.setOpacity(0);
        Text sub = new Text("Mi Picaku Piojosa ♥");
        sub.setFont(Font.font("Georgia", FontWeight.NORMAL, 30)); sub.setFill(Color.web("#ffb3c7"));
        sub.setX(Cine.W / 2 - sub.getLayoutBounds().getWidth() / 2); sub.setY(690); sub.setOpacity(0);

        // El latido va en un envoltorio: el retrato ya usa su propia escala (0.9 → 1 al posarse) y su traslación (flotar)
        Group beat = new Group(portrait);
        // Ondas del espectro al pie, detrás del nombre
        SpectrumWave wave = new SpectrumWave(Cine.W, 190, Color.web("#7b2cbf"), Color.web("#ff5d8f"), Color.web("#ffd166"));
        wave.setLayoutY(Cine.H - 190);
        wave.setOpacity(0);

        s.hud.getChildren().addAll(beam, backlight, wave, beat, halo, fx, name, sub);
        stars.setOpacity(0);

        // ---- la música mueve la escena: latido con los graves, ondas con el espectro, corazones con cada bombo
        AudioManager audio = s.audio();
        MusicReactor reactor = new MusicReactor(audio, SONG)
                .heartbeat(beat, 0.028)
                .wave(wave)
                .onKick(() -> fx.floatHearts(2, Color.web("#ff5d8f"), Color.web("#ffb3c7"), Color.web("#ffd166")));
        reactor.start();
        s.onDispose(reactor::stop);

        // ---- coreografía anclada a la canción: cada entrada cae en un golpe MEDIDO de la intro (ver MusicTrack)
        double headX = Cine.W / 2, headY = 64 + SIZE * 0.45;
        Runnable stopCues = new SongCues()
                // primer acorde: se abre el cielo
                .at(CHORD_1, () -> {
                    run(s, Cine.fade(stars, 1600, 0, 1));
                    run(s, Cine.fade(beam, 1400, 0, 1));
                    run(s, Cine.fade(backlight, 2000, 0, 1));
                    run(s, Cine.fade(wave, 2400, 0, 1));
                })
                // entra la melodía: la silueta de luz baja 40 px y crece de 0.9 a 1 con curva SOFT (sale rápido y se posa
                // despacio) durante toda la frase, mientras su opacidad sube: parece que "aterriza" en el siguiente acorde
                .at(MELODY, () -> run(s, new ParallelTransition(
                        Cine.fade(portrait, 900, 0, 1),
                        Cine.fade(light, 1400, 0, 1),
                        Cine.move(portrait, 2200, 0, 40, 0, 0, Cine.SOFT),
                        Cine.scale(portrait, 2200, 0.9, 1.0, Cine.SOFT))))
                // cambio de acorde (beat 8): destello mágico y materialización. La luz (encima) se apaga MÁS LENTO de lo
                // que el color (debajo) aparece: nunca hay un cuadro en que se vea el fondo a través del retrato
                .at(CHORD_2, () -> {
                    fx.burst(headX, headY, 46, ParticleCanvas.Kind.STAR, 380, 0.05, 1.6, 16, Color.WHITE, Color.web("#ffe08a"), Color.web("#ffd6e7"));
                    fx.burst(headX, headY, 22, ParticleCanvas.Kind.HEART, 260, -0.4, 2.2, 18, Color.web("#ff5d8f"), Color.web("#ffb3c7"));
                    fx.floatHearts(20, Color.web("#ff5d8f"), Color.web("#ffb3c7"), Color.web("#ffd166"));
                    hearts.play();
                    run(s, new ParallelTransition(Cine.fade(color, 900, 0, 1), Cine.fade(light, 1500, 1, 0)));
                })
                // beat 13: aureola
                .at(HALO, () -> { haloSpin.play(); breathe.play(); run(s, Cine.fade(halo, 600, 0, 1)); })
                // la subida antes del acorde: el nombre
                .at(RISE, () -> run(s, new ParallelTransition(Cine.fade(name, 800, 0, 1), Cine.move(name, 800, 0, 18, 0, 0, Cine.SOFT))))
                // beat 16: la dedicatoria
                .at(CHORD_3, () -> run(s, Cine.fade(sub, 500, 0, 1)))
                // entrada completa: estallido; la cinemática termina aquí y se disuelve sobre la interfaz
                .at(ENTRY, () -> {
                    fx.burst(headX, headY, 60, ParticleCanvas.Kind.HEART, 520, -0.2, 1.8, 20, Color.web("#ff5d8f"), Color.web("#ffb3c7"), Color.web("#ffd166"));
                    run(s, Cine.flash(s.hud, Color.web("#ffd6e7"), 420));
                })
                .start(audio, SONG, 0);
        s.onDispose(stopCues);

        // El guion maestro solo marca la duración: el Playback lo ata al currentTime (songStart = dropMs − ENTRY = 0)
        s.syncImpact(SONG, ENTRY);
        return new PauseTransition(Duration.millis(ENTRY + 150));
    }

    private static void run(CineStage s, Animation a) { s.keep(a).play(); }
}
