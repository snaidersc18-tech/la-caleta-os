package app.secret.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapa de las VOCES de "Harder, Better, Faster, Stronger" ({@code daft_punk_theme.mp3}, 222.86 s): el instante exacto
 * en que el vocoder canta cada una de las cuatro palabras.
 * <p>
 * <b>Cómo se midió.</b> La letra sincronizada (LRC, versión de 225 s) da el orden de las frases con ±0.3 s; sobre el
 * PCM del MP3 se buscó la energía vocal que NO está en el instrumental de los compases 10–25 (residuo espectral
 * 350–3000 Hz contra la mediana de esos compases): en la estrofa, "harder / better / faster / stronger" caen en los
 * beats 123, 125, 127 y 129 (59.96 s…62.88 s), cada palabra dos golpes después de la anterior y siempre en el 2.º/4.º
 * tiempo del compás. El LRC llega 0.27 s tarde de forma constante; corregido ese desfase, el resto de la canción cae
 * sobre la misma rejilla: en los estribillos ("work it HARDER, make it BETTER…") la sílaba tónica de cada palabra cae en
 * el beat impar {@code p+1, p+3, p+5, p+7} de cada frase de 16 beats.
 * <p>
 * Todo se guarda en BEATS de la rejilla medida en {@link MusicTrack#DAFT_PUNK_THEME} (123.48 BPM, primer golpe en
 * 194 ms), no en milisegundos sueltos: si alguna vez se vuelve a medir el tempo, las palabras se mueven con él.
 */
public final class VocalCues {
    private VocalCues() {}

    public enum Word {
        HARDER("#00f5ff"), BETTER("#ffe94a"), FASTER("#ff2bd6"), STRONGER("#ffffff");
        public final String hex;
        Word(String hex) { this.hex = hex; }
    }

    /** Los fotones salen un frame después de leer el reloj: adelantamos el disparo ~1 frame para que se VEA en el golpe. */
    private static final double VISUAL_LEAD_MS = 16;

    /** Instante (ms dentro del archivo) de cada palabra, ordenado. */
    private static final double[] MS;
    private static final Word[] WORDS;

    static {
        List<double[]> cues = new ArrayList<>();          // {beat, ordinal}
        // Estrofas: una palabra cada 2 beats.
        for (int start : new int[]{123, 187})
            for (int w = 0; w < 4; w++) cues.add(new double[]{start + 2 * w, w});
        // Estribillos: frase de 16 beats que arranca en p; H/B en la primera línea, F/S en la segunda.
        // Cadena = qué palabras canta realmente cada frase (en el último tramo el vocoder se trocea y omite algunas).
        Object[][] chorus = {
                {202, "HBFS"}, {234, "HBFS"}, {250, "HBFS"}, {266, "HBFS"}, {282, "HBFS"}, {298, "HBF"},
                {314, "HBFS"}, {346, "HF"}, {362, "HBF"}, {378, "BFS"}, {394, "BFS"}, {410, "BFS"},
                {426, "HF"}, {442, "HBFS"}};
        for (Object[] c : chorus) {
            int p = (Integer) c[0];
            String which = (String) c[1];
            for (int w = 0; w < 4; w++)
                if (which.indexOf("HBFS".charAt(w)) >= 0) cues.add(new double[]{p + 1 + 2 * w, w});
        }
        cues.sort((a, b) -> Double.compare(a[0], b[0]));
        MusicTrack t = MusicTrack.DAFT_PUNK_THEME;
        MS = new double[cues.size()];
        WORDS = new Word[cues.size()];
        for (int i = 0; i < MS.length; i++) {
            MS[i] = t.firstBeatMs + cues.get(i)[0] * t.beatMs() - VISUAL_LEAD_MS;
            WORDS[i] = Word.values()[(int) cues.get(i)[1]];
        }
    }

    /** Número de palabras en una vuelta de la canción. */
    public static int count() { return MS.length; }
    public static double ms(int i) { return MS[i]; }
    public static Word word(int i) { return WORDS[i]; }

    /** Recibe cada palabra en el instante en que suena: (palabra, ms de la canción en que cae). */
    @FunctionalInterface
    public interface Listener { void onWord(Word w, double cueMs); }

    /**
     * Seguidor de cruces: se alimenta con la posición de la canción ({@code currentTime}) y dispara cada palabra la
     * PRIMERA vez que la posición la alcanza. No acumula reloj propio: si la canción se acelera, se frena, salta o
     * vuelve a empezar (bucle infinito), las palabras siguen al audio.
     * <ul>
     *   <li>Lecturas repetidas o levemente hacia atrás (jitter del reloj nativo): se ignoran.</li>
     *   <li>Salto hacia atrás grande (fin del bucle o seek): se rebobina el índice.</li>
     *   <li>Salto hacia delante grande (seek o la ventana estuvo congelada): no se dispara una ráfaga de palabras
     *       viejas; solo la última si cayó hace menos de {@link #STALE_MS}.</li>
     * </ul>
     * Solo hilo FX (lo alimenta {@link AudioManager#onMusicTime}).
     */
    public static final class Tracker {
        private static final double REWIND_MS = 1500, JUMP_MS = 600, STALE_MS = 150;
        private final Listener listener;
        private double last = Double.NaN;
        private int next;

        public Tracker(Listener listener) { this.listener = listener; }

        public void update(double ms) {
            if (ms < 0) return;
            if (Double.isNaN(last) || ms < last - REWIND_MS || ms - last > JUMP_MS) {
                next = firstAtOrAfter(ms);
                // Primera lectura o salto: si una palabra acaba de sonar (< STALE_MS), se muestra igual.
                if (next > 0 && ms - MS[next - 1] < STALE_MS) listener.onWord(WORDS[next - 1], MS[next - 1]);
                last = ms;
                return;
            }
            if (ms <= last) return;                         // jitter: el reloj nunca retrocede para nosotros
            while (next < MS.length && MS[next] <= ms) {
                listener.onWord(WORDS[next], MS[next]);
                next++;
            }
            last = ms;
        }

        private static int firstAtOrAfter(double ms) {
            int lo = 0, hi = MS.length;
            while (lo < hi) { int mid = (lo + hi) >>> 1; if (MS[mid] < ms) lo = mid + 1; else hi = mid; }
            return lo;
        }
    }
}
