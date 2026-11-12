package app.secret.modes;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.MusicTrack;
import app.secret.dave.DaveTheDiverAnimations;
import app.secret.view.DaftPunkCinematicView;
import app.secret.view.DaftPunkConsoleView;
import app.secret.view.TakeoverView;
import app.ui.Theme;

import java.util.List;
import java.util.function.Supplier;

/**
 * MODO 1 — HUMAN AFTER ALL (DAFTPUNK). Preludio de la araña-mecha de escenario → escenario piramidal con diálogo RPG
 * → "Consola Ciber-Musical" con pads de DJ. Suena SOLO "Harder, Better, Faster, Stronger", sincronizada al beat.
 */
public final class DaftPunkMode extends BaseMode {

    @Override public String id() { return "human-after-all"; }
    @Override public String[] triggers() { return new String[]{"DAFTPUNK"}; }

    @Override public boolean canActivate(SecretContext c) {
        return c.ui().isIntroDone() && !c.ui().lock().isLocked() && !Theme.get().isLove();
    }

    @Override protected List<Supplier<Cinematic>> cinematics() {
        return List.of(() -> DaveTheDiverAnimations.daftPunk(ctx.audio()), () -> new DaftPunkCinematicView(ctx));
    }

    @Override protected TakeoverView createInterface(Runnable exit) { return new DaftPunkConsoleView(ctx, exit); }

    // La única música es "Harder, Better, Faster, Stronger": la arranca el preludio anclado a su drop y sigue sin
    // cortes por el concierto y la consola (playMusic con la misma pista no la reinicia).
    @Override protected MusicTrack interfaceMusic() { return MusicTrack.DAFT_PUNK_THEME; }
}
