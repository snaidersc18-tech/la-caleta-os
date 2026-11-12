package app.secret.modes;

import app.secret.Cinematic;
import app.secret.SecretContext;
import app.secret.audio.MusicTrack;
import app.secret.view.FerxxoDriftCinematic;
import app.secret.view.FerxxoRiderView;
import app.secret.view.TakeoverView;
import app.ui.Theme;

import java.util.List;
import java.util.function.Supplier;

/**
 * MODO 4 — FERXXO RIDER (FEID / FERXXO). Callejón de Medellín en neón verde → Feid entra derrapando en su moto de
 * motocross y aplasta a El Guachimán → se baja, sonríe y el flash de sus grills de oro ciega la pantalla → el
 * mini-juego FERXXO RIDER (endless runner). Nada de utilidades aquí: es pura sorpresa.
 * La cinemática suena solo con la moto (SFX); feid_theme (el MP3 dedicado si está en audio/, si no el WAV
 * sintetizado) entra a todo volumen y en bucle cuando arranca el juego.
 */
public final class FeidMode extends BaseMode {

    @Override public String id() { return "ferxxo-rider"; }
    @Override public String[] triggers() { return new String[]{"FEID", "FERXXO"}; }

    @Override public boolean canActivate(SecretContext c) {
        return c.ui().isIntroDone() && !c.ui().lock().isLocked() && !Theme.get().isLove();
    }

    @Override protected List<Supplier<Cinematic>> cinematics() {
        return List.of(() -> new FerxxoDriftCinematic(ctx));
    }

    @Override protected TakeoverView createInterface(Runnable exit) { return new FerxxoRiderView(ctx, exit); }

    @Override protected MusicTrack interfaceMusic() { return MusicTrack.FEID_THEME; }
}
