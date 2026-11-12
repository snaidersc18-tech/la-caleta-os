package app.narrative;

/**
 * Una línea del guion que la consola "escribe sola".
 *
 * @param text        texto a teclear
 * @param fx          efecto visual que se dispara justo antes de teclear
 * @param charMs      milisegundos base por carácter
 * @param pauseBefore pausa antes de empezar la línea
 * @param pauseAfter  pausa al terminar (antes del salto de línea)
 * @param typos       si true, de vez en cuando se "equivoca" y corrige (más humano, más cómico)
 */
public record ScriptLine(String text, Fx fx, int charMs, int pauseBefore, int pauseAfter, boolean typos) {

    public enum Fx { NONE, SHAKE, GLITCH, HEARTS, NOTES, FLASH, BIG_HEARTS }

    public static ScriptLine of(String text, Fx fx, int charMs, int pauseBefore, int pauseAfter) {
        return new ScriptLine(text, fx, charMs, pauseBefore, pauseAfter, true);
    }
}
