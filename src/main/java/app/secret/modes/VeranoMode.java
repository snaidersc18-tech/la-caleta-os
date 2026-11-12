package app.secret.modes;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.MusicTrack;
import app.secret.dave.DaveTheDiverAnimations;
import app.secret.view.TakeoverView;
import app.secret.view.VeranoCinematicView;
import app.secret.view.VeranoTropicalView;
import app.ui.Theme;

import java.util.List;
import java.util.function.Supplier;

/**
 * MODO 2 — UN VERANO SIN CIFRAR (BADBUNNY / CONCHO). Preludio del Corazón Triste tropical-ciberpunk (su estallido cae
 * en el drop de la canción) → playa: Concho camina al beat, apunta la cámara y ¡CLICK! en el primer tiempo de un
 * compás → UI tropical. Suena SOLO la canción oficial de Bad Bunny, en bucle.
 */
public final class VeranoMode extends BaseMode {

    @Override public String id() { return "verano-sin-cifrar"; }
    @Override public String[] triggers() { return new String[]{"BADBUNNY", "CONCHO"}; }

    @Override public boolean canActivate(SecretContext c) {
        return c.ui().isIntroDone() && !c.ui().lock().isLocked() && !Theme.get().isLove();
    }

    @Override protected List<Supplier<Cinematic>> cinematics() {
        return List.of(() -> DaveTheDiverAnimations.badBunny(ctx.audio()), () -> new VeranoCinematicView(ctx));
    }

    @Override protected TakeoverView createInterface(Runnable exit) { return new VeranoTropicalView(ctx, exit); }

    @Override protected MusicTrack interfaceMusic() { return MusicTrack.BAD_BUNNY_THEME; }
}
