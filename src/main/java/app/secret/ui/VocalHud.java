package app.secret.ui;

import app.secret.audio.AudioManager;
import app.secret.audio.MusicTrack;
import app.secret.audio.VocalCues;
import app.secret.audio.VocalCues.Word;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.function.Consumer;

/**
 * "HARDER · BETTER · FASTER · STRONGER" en pantalla EXACTAMENTE cuando las voces de {@code daft_punk_theme.mp3} lo
 * dicen. Capa transparente al ratón que se pone encima de cualquier vista del modo Daft Punk.
 * <p>
 * Todo lo que se ve aquí es función de la posición de la canción ({@code currentTime}), no de un Timeline:
 * <ul>
 *   <li>{@link VocalCues.Tracker} dispara la palabra cuando el audio cruza su marca.</li>
 *   <li>La envolvente (golpe de escala, destello estroboscópico, caída) se calcula como {@code ms − marca}: si el
 *       audio va más rápido o más lento, la animación también.</li>
 * </ul>
 * Abajo, un marcador de cuatro casillas tipo LED muestra cuál de las cuatro palabras está sonando.
 */
public final class VocalHud extends Pane {

    private static final double HOLD_MS = 330, FADE_MS = 300, PUNCH_MS = 70, STROBE_MS = 110;
    private final double w, h;
    private final Text big = new Text();
    private final Rectangle strobe;
    private final Text[] slots = new Text[4];
    private final VocalCues.Tracker tracker;
    private Word word;
    private double cueMs = Double.NaN;
    private Consumer<Word> onWord = w -> { };
    private Runnable detach = () -> { };

    public VocalHud(double w, double h) {
        this.w = w;
        this.h = h;
        setPrefSize(w, h);
        setMouseTransparent(true);
        setPickOnBounds(false);

        // Destello a pantalla completa con mezcla NORMAL: un BlendMode.ADD de 1280x720 obliga a una pasada extra de
        // composición de toda la escena en cada frame (medido: −15 FPS). Además se oculta en cuanto se apaga.
        strobe = new Rectangle(w, h, Color.WHITE);
        strobe.setOpacity(0);
        strobe.setVisible(false);

        big.setFont(Font.font("Impact", FontWeight.BLACK, 168));
        big.setTextOrigin(VPos.CENTER);
        big.setTextAlignment(TextAlignment.CENTER);
        big.setStroke(Color.BLACK);
        big.setStrokeWidth(4);
        big.setOpacity(0);
        big.setCache(true);
        big.setCacheHint(CacheHint.SCALE);              // la escala del golpe reutiliza la textura cacheada

        getChildren().addAll(strobe, big);
        Font slotFont = Font.font("Monospaced", FontWeight.BOLD, 22);
        double slotW = 190, x0 = w / 2 - slotW * 2;
        for (int i = 0; i < 4; i++) {
            Text s = new Text(Word.values()[i].name());
            s.setFont(slotFont);
            s.setFill(Color.web(Word.values()[i].hex));
            s.setTextAlignment(TextAlignment.CENTER);
            s.setWrappingWidth(slotW);
            s.setX(x0 + i * slotW);
            s.setY(h - 22);
            s.setOpacity(0.18);
            slots[i] = s;
            getChildren().add(s);
        }
        tracker = new VocalCues.Tracker(this::trigger);
    }

    /** Engancha el HUD al reloj de la canción. Devuelve this para encadenar. Llamar a {@link #stop()} al cerrar. */
    public VocalHud syncTo(AudioManager audio) {
        detach.run();
        detach = audio.onMusicTime(MusicTrack.DAFT_PUNK_THEME, this::update);
        return this;
    }

    /** Aviso extra por palabra (p. ej. para que el escenario destelle sus propios estrobos). */
    public VocalHud onWord(Consumer<Word> c) { this.onWord = c; return this; }

    /** Formas del escenario (p. ej. las pantallas triangulares) que destellan con el color de cada palabra. */
    public VocalHud strobeAlso(javafx.scene.shape.Shape... shapes) { this.extra = shapes; return this; }
    private javafx.scene.shape.Shape[] extra = {};

    public void stop() { detach.run(); detach = () -> { }; }

    private static final boolean DEBUG = Boolean.getBoolean("rebusque.debug");
    private final DropShadow[] glows = java.util.Arrays.stream(Word.values())
            .map(x -> new DropShadow(28, Color.web(x.hex))).toArray(DropShadow[]::new);
    private double lastMs;

    private void trigger(Word w, double at) {
        if (DEBUG) System.err.printf(java.util.Locale.ROOT, "[voz] %-8s marca %9.1f ms · audio %9.1f ms · desfase %+5.1f ms%n",
                w, at, lastMs, lastMs - at);
        Color c = Color.web(w.hex);
        if (word != w) {                                  // texto + resplandor solo cambian si cambia la palabra
            big.setText(w.name());
            big.setFill(c);
            big.setEffect(glows[w.ordinal()]);
        }
        word = w;
        cueMs = at;
        big.setX(this.w / 2 - big.getLayoutBounds().getWidth() / 2);
        big.setY(h * 0.42);
        strobe.setFill(c);
        for (javafx.scene.shape.Shape s : extra) s.setFill(c);
        onWord.accept(w);
    }

    /** Llamado por el reloj de la canción (listener de currentTime + afinador por frame). */
    private void update(double ms) {
        lastMs = ms;
        tracker.update(ms);
        if (word == null) return;
        double t = ms - cueMs;
        if (t < -200 || t > 4000) {                       // bucle de la canción o salto: apagar
            word = null;
            big.setVisible(false); strobe.setVisible(false);
            for (javafx.scene.shape.Shape s : extra) s.setVisible(false);
            for (Text s : slots) s.setOpacity(0.18);
            return;
        }
        t = Math.max(0, t);
        double alpha = t < HOLD_MS ? 1 : Math.max(0, 1 - (t - HOLD_MS) / FADE_MS);
        double punch = 1 + 0.38 * Math.exp(-t / PUNCH_MS);
        big.setVisible(alpha > 0);
        big.setOpacity(alpha);
        big.setScaleX(punch);
        big.setScaleY(punch);
        double flash = Math.exp(-t / STROBE_MS);
        boolean lit = flash > 0.02;                       // invisible = fuera del render (nada de pasadas a opacidad ~0)
        strobe.setVisible(lit);
        strobe.setOpacity(0.38 * flash);
        for (javafx.scene.shape.Shape s : extra) { s.setVisible(lit); s.setOpacity(Math.min(1, 1.6 * flash)); }
        for (int i = 0; i < 4; i++) {
            double o = i == word.ordinal() ? 0.18 + 0.82 * Math.max(alpha, 0.35) : 0.18;
            if (slots[i].getOpacity() != o) slots[i].setOpacity(o);
        }
    }
}
