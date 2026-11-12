package app.narrative;

import app.narrative.ScriptLine.Fx;

import java.util.List;

/**
 * Guion del "Payload Picaku" (estado 2). Tono: poesía urbana divertida, estilo Aerophon.
 * Textos originales: no reproducen letras de nadie.
 */
public final class AerophonScript {
    private AerophonScript() {}

    public static List<ScriptLine> takeover() {
        return List.of(
                ScriptLine.of(">>> INTERVENCIÓN DEL SISTEMA INICIADA...", Fx.GLITCH, 38, 350, 450),
                ScriptLine.of(">>> Quién escribe ahora: un hacker de barrio con la gorra al revés y el corazón en root.", Fx.NONE, 24, 120, 380),
                ScriptLine.of(">>> Bypasseando las defensas... El algoritmo no pudo calcular esta sonrisa.", Fx.SHAKE, 28, 80, 420),
                ScriptLine.of(">>> Inyectando saxofón en la terminal...", Fx.NOTES, 42, 120, 700),
                ScriptLine.of(">>> Compilando rimas en do menor... 0 errores, 1 corazón con warnings.", Fx.HEARTS, 26, 100, 380),
                ScriptLine.of(">>> sudo apt install ternura --force", Fx.NONE, 30, 80, 320),
                ScriptLine.of(">>> [##########] 100%  paquete 'abrazo-largo' instalado sin dependencias rotas.", Fx.FLASH, 14, 60, 420),
                ScriptLine.of(">>> Desactivando el modo 'serio': la ciudad es dura pero hoy hay verso.", Fx.NONE, 26, 100, 380),
                ScriptLine.of(">>> Para Sofia (Mi Picaku Piojosa): La calle está dura, pero mi código por ti no falla.", Fx.BIG_HEARTS, 46, 220, 900),
                ScriptLine.of(">>> ACCESO CONCEDIDO. Bienvenida a tu guarida. Notas secretas habilitadas.", Fx.GLITCH, 30, 160, 700)
        );
    }

    public static final String[] REVERT_LINES = {
            ">>> Aerophon.dll descargado. Vuelve Alcolirykoz.exe, con su mal genio de siempre.",
            ">>> La calle reclama su asfalto. El secreto sigue a salvo."
    };
}
