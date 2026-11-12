package app.secret;

import app.secret.audio.AudioManager;
import app.secret.modes.BbyWowMode;
import app.secret.modes.DaftPunkMode;
import app.secret.modes.FeidMode;
import app.secret.modes.SectorSpMode;
import app.secret.modes.VeranoMode;
import app.ui.MainView;
import javafx.scene.Scene;
import javafx.stage.WindowEvent;

import java.util.List;

/**
 * Registro y arbitraje de los modos secretos y de los COMANDOS de navegación (Zero-UI: no hay botones de escape).
 * Instala el {@link TriggerHook} (palabras clave a nivel de ventana) y delega en la {@link TakeoverStateMachine}: un
 * disparador solo se atiende en IDLE, así que nunca puede haber dos takeovers ni dos interfaces a la vez.
 * <ul>
 *   <li><b>DAFTPUNK · BADBUNNY/CONCHO · FEID/FERXXO · SOFIA</b>: abren su easter egg.</li>
 *   <li><b>CALLE</b>: desde cualquier easter egg (también el modo amor PICAKU) → El Mordisco + péndulo de vuelta.</li>
 *   <li><b>SALIR</b>: desde la consola principal → El Cafecito (cafetería + "Bonito, Bonito" con El Guachimán y su frappé) y, solo al final, Platform.exit().</li>
 * </ul>
 */
public final class EasterEggManager {

    private final SecretContext ctx;
    private final List<SecretMode> modes = List.of(new SectorSpMode(), new DaftPunkMode(), new VeranoMode(), new FeidMode(), new BbyWowMode());
    private SecretMode active;
    private boolean wired;
    private boolean busy;                                           // Mordisco o Despedida en curso: nada más se atiende

    private EasterEggManager(MainView ui, AudioManager audio) {
        this.ctx = new SecretContext(ui, audio);
        ctx.onStreetCommand(this::backToStreet);                    // CALLE + Enter desde la consola de FERXXO RIDER
    }

    /** Compatibilidad: crea su propio AudioManager (la intro quedaría muda). */
    public static EasterEggManager install(MainView ui) {
        AudioManager audio = new AudioManager();
        audio.preload();
        return install(ui, audio);
    }

    /** Main: {@code EasterEggManager.install(view, audio);} — el mismo AudioManager que usa la intro. */
    public static EasterEggManager install(MainView ui, AudioManager audio) {
        EasterEggManager m = new EasterEggManager(ui, audio);
        if (ui.getScene() != null) m.wire(ui.getScene());
        else ui.sceneProperty().addListener((o, a, s) -> { if (s != null) m.wire(s); });
        return m;
    }

    private void wire(Scene scene) {
        if (wired) return;
        wired = true;
        TriggerHook hook = new TriggerHook()
                // tecleo mecánico solo en la interfaz base: las vistas secretas tienen su propia banda sonora
                .onKeystroke(() -> { if (ctx.fsm().isIdle()) ctx.audio().keyClick(); })
                // eco de la línea de comandos en la consola principal ("rebusque@calle:~$ CAL")
                .onBuffer(s -> ctx.console().setCommandLine(s));
        for (SecretMode m : modes) for (String w : m.triggers()) hook.on(w, () -> trigger(m));
        hook.on("CALLE", this::backToStreet);
        hook.on("SALIR", this::farewell);
        hook.install(scene);
        if (scene.getWindow() != null) releaseAudioOnClose(scene.getWindow());
        else scene.windowProperty().addListener((o, a, w) -> { if (w != null && a == null) releaseAudioOnClose(w); });
    }

    private void releaseAudioOnClose(javafx.stage.Window w) {
        w.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> ctx.audio().shutdown());
    }

    private void trigger(SecretMode m) {
        if (busy || !ctx.fsm().isIdle()) return;                 // ya hay un flujo en curso: se ignora (fix del solape)
        if (!m.canActivate(ctx)) {
            if (ctx.ui().isIntroDone() && !ctx.ui().lock().isLocked()) {
                ctx.log("Sector ocupado: escribe CALLE antes de cambiar de modo.");
            }
            return;
        }
        active = m;
        app.animations.FpsProbe.scene(m.id());
        m.activate(ctx);
    }

    /**
     * "CALLE" dentro de cualquier easter egg (Daft Punk, Bad Bunny, Sofía; en cinemática o en interfaz) o en el modo
     * amor PICAKU: El Guachimán baja, muerde, la infección cubre la pantalla y SOLO entonces se cierra el modo
     * (onCovered); después se balancea y aterriza en su marco mientras reaparece el Ministerio.
     */
    private void backToStreet() {
        if (busy) return;
        boolean inMode = active != null && active.isActive();
        boolean inLove = !inMode && ctx.fsm().isIdle() && ctx.ui().isLove() && !ctx.ui().lock().isLocked();
        if (!inMode && !inLove) return;
        SecretMode closing = active;
        busy = true;
        if (inMode) ctx.audio().stopAll();                       // la canción del modo se corta en seco: el mordisco suena solo
        app.animations.FpsProbe.scene("calle");
        ctx.console().setCommandLine("");
        SpiderBite.play(ctx.overlay(), inMode ? ctx.layer() : ctx.ui().shakeTarget(), ctx.ui().spider(), ctx.audio(),
                () -> {
                    if (inMode) {
                        closing.deactivate();                    // idempotente
                        if (active == closing) active = null;
                        ctx.ui().showStreetMenu();
                    } else {
                        ctx.ui().revertLove();
                    }
                },
                () -> {
                    busy = false;
                    ctx.audio().playStreetThemeFromDrop();       // vuelve Alcolirykoz, directo en el beat (sin la intro hablada)
                    app.animations.FpsProbe.scene("menu");
                    ctx.log("El Guachimán mordió y volvió a su puesto. Sector cerrado: vuelve el asfalto de siempre.");
                });
    }

    /**
     * "SALIR" en la consola principal (sin ningún easter egg abierto): La Despedida. La ventana NO se cierra aquí:
     * CafecitoExit llama a Platform.exit() al terminar la tercera frase del cafecito (fundido a negro, ≈33 s).
     */
    private void farewell() {
        if (busy || !ctx.fsm().isIdle() || !ctx.ui().isIntroDone() || ctx.ui().lock().isLocked()) return;
        busy = true;
        ctx.console().setCommandLine("");
        ctx.log("Orden recibida: SALIR. El Guachimán se despide... cerrando el Ministerio Del Rebusque.");
        ctx.ui().lock().lock("");                                // bloquea la entrada; el letrero no se muestra
        CafecitoExit.play(ctx.overlay(), ctx.ui().spider(), ctx.ui().shakeTarget(), ctx.audio(),
                () -> ctx.ui().lock().hideBanner(),              // la cafetería ocupa toda la pantalla
                () -> ctx.audio().shutdown());
    }
}
