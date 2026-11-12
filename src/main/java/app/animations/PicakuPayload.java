package app.animations;

import app.narrative.AerophonScript;
import app.narrative.ScriptLine;
import app.narrative.StreetLogs;
import app.secret.audio.MusicTrack;
import app.ui.MainView;
import app.ui.Theme;
import javafx.animation.PauseTransition;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Orquestador del easter egg ("Payload Picaku"). Máquina de estados lineal:
 *
 *   trigger() -> bloqueo de entrada
 *             -> ErrorCascade (ventanitas de error, temblor, colapso, flash)
 *             -> mutate()   (paleta verde/rojo -> neón/magenta con estroboscopio, corazones en los ojos)
 *             -> takeover() (la consola escribe sola el guion de Aerophon)
 *             -> finish()   (desbloqueo y entorno de notas secretas)
 *
 * revert() deshace la mutación y devuelve la app al estado 1.
 */
public final class PicakuPayload {

    private final MainView ui;
    private boolean running;
    private boolean love;

    public PicakuPayload(MainView ui) { this.ui = ui; }

    public boolean isLove() { return love; }

    public void trigger() {
        if (running || love || ui.lock().isLocked() || !ui.isIntroDone()) return;
        running = true;
        ui.lock().lock("!! SISTEMA COMPROMETIDO :: ENTRADA BLOQUEADA !!");
        ui.console().println(">>> ALERTA: intrusión detectada en el MINISTERIO DEL REBUSQUE...");
        ui.console().println(">>> origen: desconocido. firma: S.P.");

        ErrorCascade cascade = new ErrorCascade(ui.effectsLayer(), MainView.W, MainView.H,
                amp -> ScreenShake.shake(ui.shakeTarget(), amp, 260), ui::flash);
        PauseTransition beat = new PauseTransition(Duration.millis(450));
        beat.setOnFinished(e -> cascade.play(this::mutate));
        beat.play();
    }

    // ------------------------------------------------------------------ fase 2

    private void mutate() {
        ui.lock().lock("// MUTANDO INTERFAZ //");
        Theme.get().strobeTo(1.0);
        ui.setLoveVisuals(true);
        // la interfaz muta a modo amor y suena "Algo Mágico" (playMusic hace stop() + dispose() de la CALLE antes)
        if (ui.audio() != null) ui.audio().playMusic(MusicTrack.SOFIA_THEME);
        ScreenShake.shake(ui.shakeTarget(), 6, 420);
        ui.flash(Color.web("#ff2bd6"), 380);
        ui.console().clear();

        PauseTransition settle = new PauseTransition(Duration.millis(1100));
        settle.setOnFinished(e -> takeover());
        settle.play();
    }

    // ------------------------------------------------------------------ fase 3

    private void takeover() {
        ui.lock().lock(">> CONSOLA TOMADA :: AEROPHON.DLL TIENE EL CONTROL <<");
        GlitchText.morph(ui.console().titleLabel(), "CONSOLA TOMADA POR: AEROPHON.DLL", Duration.millis(900));
        TypewriterEffect.play(ui.console(), AerophonScript.takeover(), this::fxFor, this::finish);
    }

    private Runnable fxFor(ScriptLine.Fx fx) {
        return switch (fx) {
            case SHAKE -> () -> ScreenShake.shake(ui.shakeTarget(), 5, 300);
            case GLITCH -> () -> { ui.spider().glitchFor(500); ScreenShake.shake(ui.shakeTarget(), 3, 200); };
            case HEARTS -> () -> LoveParticles.burst(ui.maskParticles(), 180, 230, 10, 120, false);
            case NOTES -> () -> LoveParticles.notes(ui.effectsLayer(), 640, 560, 12, 300);
            case FLASH -> () -> ui.flash(Color.web("#00f5ff"), 240);
            case BIG_HEARTS -> () -> {
                LoveParticles.burst(ui.effectsLayer(), 640, 420, 28, 460, true);
                ui.flash(Color.web("#ff2bd6"), 420);
            };
            case NONE -> null;
        };
    }

    // ------------------------------------------------------------------ fase 4

    private void finish() {
        ui.lock().unlock();
        love = true;
        running = false;
        ui.enterWorkspace(true);
        ui.console().println(">>> [ NOTAS SECRETAS ] Escribe, ponle clave y la escondo en una foto de neón.");
        ui.console().println(">>> Menú: [1] ESCRIBIR NOTA  [2] ABRIR NOTA  ·  escribe CALLE para volver");
    }

    /** Vuelta al estado 1. */
    public void revert() {
        if (!love || running) return;
        running = true;
        ui.lock().lock("// RESTAURANDO ASFALTO //");
        Theme.get().strobeTo(0.0);
        ui.setLoveVisuals(false);
        ScreenShake.shake(ui.shakeTarget(), 5, 350);
        ui.console().clear();
        PauseTransition p = new PauseTransition(Duration.millis(700));
        p.setOnFinished(e -> {
            for (String s : AerophonScript.REVERT_LINES) ui.console().println(s);
            ui.console().println(StreetLogs.BOOT[3]);
            love = false;
            running = false;
            ui.lock().unlock();
            ui.enterWorkspace(false);
        });
        p.play();
    }
}
