package app.secret.dave;

import app.secret.audio.AudioManager;
import app.secret.audio.MusicTrack;
import app.secret.audio.SoundEffect;
import javafx.animation.Animation;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/** Escenario de una cinemática: fondo, "mundo" (cámara: escala/traslación) y HUD, todo recortado a 1280x720. */
public final class CineStage extends Pane {
    public final Rectangle bg = new Rectangle(Cine.W, Cine.H, Color.BLACK);
    public final Group world = new Group();
    public final Pane hud = new Pane();
    private final List<Animation> anims = new ArrayList<>();
    private final List<Runnable> cleanups = new ArrayList<>();
    private final AudioManager audio;

    public CineStage(AudioManager audio) {
        this.audio = audio;
        setMinSize(Cine.W, Cine.H); setPrefSize(Cine.W, Cine.H); setMaxSize(Cine.W, Cine.H);
        setPickOnBounds(true);
        setClip(new Rectangle(Cine.W, Cine.H));
        world.setManaged(false);
        hud.setPickOnBounds(false); hud.setMouseTransparent(true);
        getChildren().addAll(bg, world, hud);
    }


    public <A extends Animation> A keep(A a) { anims.add(a); return a; }
    public void onDispose(Runnable r) { cleanups.add(r); }

    /** Cue de sonido para insertar en una SequentialTransition (no hace nada si la cinemática va sin audio). */
    public Animation sfx(SoundEffect s) { return Cine.cue(() -> { if (audio != null) audio.play(s); }); }

    // ---- rhythm sync: el impacto del guion (en ms de la línea de tiempo) cae en el drop de la canción
    MusicTrack syncTrack;
    double syncTimelineMs;

    /**
     * Declara que el instante {@code timelineMs} del guion debe sonar EXACTAMENTE en {@link MusicTrack#dropMs} de
     * {@code t}. El Playback arranca la canción en {@code dropMs − timelineMs} y, cuando el audio está sonando de verdad,
     * arranca el guion con {@code playFrom(posición del audio − inicio)}: queda anclado al currentTime, no al reloj.
     */
    public void syncImpact(MusicTrack t, double timelineMs) { syncTrack = t; syncTimelineMs = timelineMs; }

    AudioManager audio() { return audio; }

    public void dispose() {
        for (Animation a : anims) a.stop();
        anims.clear();
        for (Runnable r : cleanups) { try { r.run(); } catch (RuntimeException ignored) { } }
        cleanups.clear();
    }
}
