package app.secret.modes;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.MusicTrack;
import app.secret.dave.DaveTheDiverAnimations;
import app.secret.view.SofiaView;
import app.secret.view.TakeoverView;
import app.ui.Theme;

import java.util.List;
import java.util.function.Supplier;

/**
 * MODO 3 — SECTOR S.P. (SOFIA). Retrato de Sofía con fade-in angelical, corazones y estrellas → "Interfaz de Sofía"
 * con el Módulo Cartas (escribir y abrir notas cifradas con el motor AES-256-GCM + LSB del núcleo).
 * <p>
 * "SOFIA" ya no pasa por el núcleo (TriggerHook la reclama antes), así que la cascada/mutación de PicakuPayload no
 * corre en paralelo. El núcleo sigue disponible con "PICAKU" o el backdoor de la consola.
 */
public final class SectorSpMode extends BaseMode {

    @Override public String id() { return "sector-sp"; }
    @Override public String[] triggers() { return new String[]{"SOFIA"}; }

    @Override public boolean canActivate(SecretContext c) {
        return c.ui().isIntroDone() && !c.ui().lock().isLocked() && !Theme.get().isLove();
    }

    @Override protected List<Supplier<Cinematic>> cinematics() {
        return List.of(() -> DaveTheDiverAnimations.sofia(ctx().audio()));
    }

    @Override protected TakeoverView createInterface(Runnable exit) { return new SofiaView(ctx(), exit); }

    // la misma pista acompaña cinemática e interfaz: AudioManager no la reinicia al pasar de fase
    @Override protected MusicTrack cinematicMusic() { return MusicTrack.SOFIA_THEME; }
    @Override protected MusicTrack interfaceMusic() { return MusicTrack.SOFIA_THEME; }
}
