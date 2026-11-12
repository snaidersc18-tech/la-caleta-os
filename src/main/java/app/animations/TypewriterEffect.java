package app.animations;

import app.narrative.ScriptLine;
import app.ui.ConsolePanel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Efecto máquina de escritura de la consola "tomada".
 *
 * Todo el guion se precalcula como una línea de tiempo de eventos (tiempo, acción) y se
 * vuelca en UN solo Timeline:
 *   - ritmo irregular por carácter (jitter) y pausas extra en puntuación
 *   - errores de tecleo humanos: teclea mal, "se da cuenta", borra y corrige
 *   - efectos (shake, corazones, notas...) disparados justo antes de cada línea
 */
public final class TypewriterEffect {
    private static final Random R = new Random();
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";

    private TypewriterEffect() {}

    public static Timeline play(ConsolePanel console, List<ScriptLine> script,
                                Function<ScriptLine.Fx, Runnable> fxMapper, Runnable onDone) {
        Timeline tl = new Timeline();
        double t = 0;

        for (ScriptLine line : script) {
            t += line.pauseBefore();
            Runnable fx = line.fx() == ScriptLine.Fx.NONE ? null : fxMapper.apply(line.fx());
            if (fx != null) at(tl, t, fx);

            String s = line.text();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                double base = line.charMs() * (0.55 + R.nextDouble() * 0.9);

                boolean canTypo = line.typos() && i > 6 && Character.isLetter(c) && R.nextDouble() < 0.028;
                if (canTypo) {
                    char wrong = LETTERS.charAt(R.nextInt(LETTERS.length()));
                    at(tl, t, () -> console.typeChar(wrong));
                    t += 150 + R.nextDouble() * 120;      // "se da cuenta"
                    at(tl, t, console::backspace);
                    t += 80;
                }
                final char cc = c;
                at(tl, t, () -> console.typeChar(cc));

                t += base;
                if (c == ',' || c == ';') t += 110;
                else if (c == '.' || c == '!' || c == '?') t += 220;
                else if (c == ':') t += 140;
            }
            t += line.pauseAfter();
            at(tl, t, console::newLine);
            t += 40;
        }
        at(tl, t + 120, onDone);
        tl.play();
        return tl;
    }

    private static void at(Timeline tl, double ms, Runnable r) {
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(ms), e -> r.run()));
    }
}
