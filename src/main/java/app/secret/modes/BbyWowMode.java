package app.secret.modes;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.MusicTrack;
import app.secret.view.BbyWowCinematicView;
import app.secret.view.TakeoverView;
import app.ui.Theme;

import java.util.List;
import java.util.function.Supplier;

/**
 * MODO 5 — BBYWOW. Concierto en vivo de El Guachimán con peluca azul sobre "BbY WOW": apagón, descenso por la
 * telaraña, flashazo en el drop, estadio con láseres, fuegos artificiales, público y mini arañas bailarinas, y al
 * llegar a ≈1:09 un apagón que devuelve a la calle con el péndulo de siempre (mismo flujo que teclear CALLE).
 * Todo se ancla al currentTime REAL de la canción (ver {@link BbyWowCinematicView}).
 * <p>
 * El modo no tiene interfaz propia: el concierto termina pidiendo la calle antes de que la cinemática acabe.
 */
public final class BbyWowMode extends BaseMode {

    @Override public String id() { return "bbywow"; }
    @Override public String[] triggers() { return new String[]{"BBYWOW"}; }

    @Override public boolean canActivate(SecretContext c) {
        return c.ui().isIntroDone() && !c.ui().lock().isLocked() && !Theme.get().isLove();
    }

    @Override protected List<Supplier<Cinematic>> cinematics() {
        return List.of(() -> new BbyWowCinematicView(ctx));
    }

    /** Red de seguridad: si la cinemática terminara sola, una vista vacía (CALLE sigue funcionando). */
    @Override protected TakeoverView createInterface(Runnable exit) { return new TakeoverView() { }; }

    // la canción suena desde el mismo pulso en que se teclea BBYWOW (playMusic corta cualquier otra pista)
    @Override protected MusicTrack cinematicMusic() { return MusicTrack.BBYWOW_THEME; }
}
