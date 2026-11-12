import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Sintetizador procedural de los audios PROVISIONALES del Ministerio Del Rebusque (mono, 16 bit, 44.1 kHz).
 * Los bucles se renderizan en un búfer circular: todo lo que "se sale" por el final (colas, ecos) vuelve a entrar
 * por el principio, así el loop empalma sin clic.
 *   java SoundGen <carpeta-destino>
 */
public class SoundGen {
    static final int SR = 44100;
    static final Random RND = new Random(42);

    // ------------------------------------------------------------------ búfer
    static final class Buf {
        final float[] d; final boolean loop;
        Buf(double sec, boolean loop) { d = new float[(int) Math.round(sec * SR)]; this.loop = loop; }
        int len() { return d.length; }
        void add(long i, double v) {
            if (loop) d[(int) Math.floorMod(i, (long) d.length)] += (float) v;
            else if (i >= 0 && i < d.length) d[(int) i] += (float) v;
        }
        void mix(Buf o, double g) { for (int i = 0; i < d.length; i++) d[i] += (float) (o.d[i] * g); }
    }

    static double mtof(double m) { return 440 * Math.pow(2, (m - 69) / 12.0); }
    static double noise() { return RND.nextDouble() * 2 - 1; }
    static double saw(double ph) { return 2 * (ph - Math.floor(ph + 0.5)); }
    static double sq(double ph) { return (ph - Math.floor(ph)) < 0.5 ? 1 : -1; }
    static long at(double t) { return Math.round(t * SR); }

    /** Biquad pasa-banda (RBJ, ganancia de pico 0 dB). */
    static final class BP {
        double b0, b2, a1, a2, x1, x2, y1, y2;
        BP(double f, double q) { set(f, q); }
        void set(double f, double q) {
            double w = 2 * Math.PI * f / SR, al = Math.sin(w) / (2 * q), a0 = 1 + al;
            b0 = al / a0; b2 = -al / a0; a1 = -2 * Math.cos(w) / a0; a2 = (1 - al) / a0;
        }
        double p(double x) { double y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2; x2 = x1; x1 = x; y2 = y1; y1 = y; return y; }
    }

    /** Pasa-bajos de un polo: y += a·(x − y), a = 1 − e^(−2πf/SR). */
    static final class LP {
        double y, a;
        LP(double f) { set(f); }
        void set(double f) { a = 1 - Math.exp(-2 * Math.PI * Math.min(f, SR * 0.45) / SR); }
        double p(double x) { return y += a * (x - y); }
    }

    // ------------------------------------------------------------------ instrumentos
    static void kick(Buf b, double t, double amp, double decay) {
        double ph = 0;
        for (int i = 0; i < SR * decay * 1.6; i++) {
            double s = (double) i / SR, f = 45 + 110 * Math.exp(-s / 0.035);
            ph += f / SR;
            double v = Math.sin(2 * Math.PI * ph) * Math.exp(-s / decay) + (i < 90 ? noise() * 0.3 * (1 - i / 90.0) : 0);
            b.add(at(t) + i, v * amp);
        }
    }

    static void snare(Buf b, double t, double amp, double decay, double tone) {
        BP bp = new BP(tone, 0.9);
        for (int i = 0; i < SR * decay * 4; i++) {
            double s = (double) i / SR;
            double v = bp.p(noise()) * 1.8 * Math.exp(-s / decay) + Math.sin(2 * Math.PI * 190 * s) * 0.4 * Math.exp(-s / 0.04);
            b.add(at(t) + i, v * amp);
        }
    }

    static void hat(Buf b, double t, double amp, double decay) {
        LP lp = new LP(7000);
        for (int i = 0; i < SR * decay * 5; i++) {
            double n = noise(), v = (n - lp.p(n)) * Math.exp(-(double) i / SR / decay);
            b.add(at(t) + i, v * amp);
        }
    }

    /** Bajo sierra con filtro que se cierra (envolvente de corte exponencial). */
    static void sawBass(Buf b, double t, double dur, double f, double amp) {
        LP lp = new LP(2000); double ph = 0;
        for (int i = 0; i < SR * (dur + 0.05); i++) {
            double s = (double) i / SR;
            lp.set(220 + 1600 * Math.exp(-s / 0.09));
            ph += f / SR;
            double env = Math.min(1, s / 0.004) * (s < dur ? 1 : Math.exp(-(s - dur) / 0.015));
            b.add(at(t) + i, lp.p(saw(ph)) * env * amp);
        }
    }

    /** Pad: 3 sierras desafinadas ±7 cents por nota, ataque lento, filtro suave. */
    static void pad(Buf b, double t, double dur, int[] notes, double amp) {
        for (int n : notes) {
            double f = mtof(n);
            for (double cents : new double[]{-7, 0, 7}) {
                double fd = f * Math.pow(2, cents / 1200), ph = RND.nextDouble();
                LP lp = new LP(1400);
                for (int i = 0; i < SR * (dur + 0.4); i++) {
                    double s = (double) i / SR;
                    ph += fd / SR;
                    double env = Math.min(1, s / 0.35) * (s < dur ? 1 : Math.exp(-(s - dur) / 0.15));
                    b.add(at(t) + i, lp.p(saw(ph)) * env * amp / 3);
                }
            }
        }
    }

    /** Piano eléctrico tipo Rhodes: fundamental + 2º armónico + parcial de campana que muere rápido, con trémolo. */
    static void rhodes(Buf b, double t, double dur, int[] notes, double amp) {
        for (int n : notes) {
            double f = mtof(n);
            for (int i = 0; i < SR * (dur + 0.6); i++) {
                double s = (double) i / SR;
                double env = Math.min(1, s / 0.006) * Math.exp(-s / 1.6) * (s < dur ? 1 : Math.exp(-(s - dur) / 0.2));
                double trem = 1 + 0.18 * Math.sin(2 * Math.PI * 4.5 * s);
                double v = Math.sin(2 * Math.PI * f * s) + 0.25 * Math.sin(4 * Math.PI * f * s)
                        + 0.35 * Math.sin(2 * Math.PI * f * 7.1 * s) * Math.exp(-s / 0.05);
                b.add(at(t) + i, v * env * trem * amp / notes.length);
            }
        }
    }

    /** "Saxo": sierra + cuadrada, filtro que abre con el soplido, vibrato que entra tarde y ruido de aliento. */
    static void sax(Buf b, double t, double dur, double midi, double amp) {
        double f = mtof(midi), ph = 0;
        LP lp = new LP(1200); BP breath = new BP(2400, 1.2);
        for (int i = 0; i < SR * (dur + 0.12); i++) {
            double s = (double) i / SR;
            double vib = 1 + 0.006 * Math.sin(2 * Math.PI * 5.2 * s) * Math.min(1, Math.max(0, (s - 0.25) / 0.3));
            ph += f * vib / SR;
            double env = Math.min(1, s / 0.06) * (s < dur ? 1 - 0.15 * s / dur : (1 - 0.15) * Math.exp(-(s - dur) / 0.05));
            lp.set(700 + 1600 * env);
            double v = lp.p(0.7 * saw(ph) + 0.3 * sq(ph)) + breath.p(noise()) * 0.12 * env;
            b.add(at(t) + i, v * env * amp);
        }
    }

    /** Campana: parciales inarmónicos (1, 2.76, 5.4) con caídas cada vez más cortas. */
    static void bell(Buf b, double t, double f, double amp, double decay) {
        double[] ratio = {1, 2.76, 5.4}, gain = {1, 0.45, 0.2}, dec = {decay, decay * 0.5, decay * 0.25};
        for (int i = 0; i < SR * decay * 4; i++) {
            double s = (double) i / SR, v = 0;
            for (int k = 0; k < 3; k++) v += gain[k] * Math.sin(2 * Math.PI * f * ratio[k] * s) * Math.exp(-s / dec[k]);
            b.add(at(t) + i, v * Math.min(1, s / 0.002) * amp);
        }
    }

    // ------------------------------------------------------------------ efectos sobre el búfer
    /** Eco con realimentación. En bucle se recorre 3 vueltas para que el estado "dé la vuelta" y el empalme sea limpio. */
    static void echo(Buf b, double sec, double fb, double mix) {
        int d = (int) (sec * SR), n = b.len();
        float[] dry = b.d.clone(), wet = new float[n];
        int passes = b.loop ? 3 : 1;
        // Línea de retardo realimentada: wet[i] = dry[i − d] + fb·wet[i − d]  (cada repetición pierde un factor fb)
        for (int p = 0; p < passes; p++)
            for (int i = 0; i < n; i++) {
                int j = i - d;
                if (j < 0 && !b.loop) continue;
                int k = j < 0 ? j + n : j;
                wet[i] = (float) (dry[k] + fb * wet[k]);
            }
        for (int i = 0; i < n; i++) b.d[i] = (float) (dry[i] + mix * wet[i]);
    }

    static void lowpass(Buf b, double f) {
        LP lp = new LP(f);
        int passes = b.loop ? 2 : 1;                                // 2ª vuelta: el filtro arranca con el estado del final
        float[] out = new float[b.len()];
        for (int p = 0; p < passes; p++) for (int i = 0; i < b.len(); i++) out[i] = (float) lp.p(b.d[i]);
        System.arraycopy(out, 0, b.d, 0, out.length);
    }

    /** Polvo de vinilo: siseo suave + chasquidos aleatorios. */
    static void vinyl(Buf b, double hiss, double cracklesPerSec) {
        LP lp = new LP(3000);
        for (int i = 0; i < b.len(); i++) {
            b.d[i] += (float) (lp.p(noise()) * hiss);
            if (RND.nextDouble() < cracklesPerSec / SR) {
                double a = 0.05 + RND.nextDouble() * 0.12, sgn = RND.nextBoolean() ? 1 : -1;
                for (int k = 0; k < 40; k++) b.add(i + k, sgn * a * Math.exp(-k / 6.0));
            }
        }
    }

    /** Saturación suave (tanh) + normalización al pico objetivo. Los bucles fuerzan además media cero. */
    static byte[] finish(Buf b, double peak) {
        double mean = 0;
        if (b.loop) { for (float v : b.d) mean += v; mean /= b.len(); }
        double max = 1e-9;
        for (int i = 0; i < b.len(); i++) { b.d[i] = (float) Math.tanh((b.d[i] - mean) * 1.2); max = Math.max(max, Math.abs(b.d[i])); }
        if (!b.loop) {                                              // micro-fundido de 3 ms al final de los SFX
            int f = (int) (0.003 * SR);
            for (int i = 0; i < f; i++) b.d[b.len() - 1 - i] *= (float) i / f;
        }
        ByteBuffer bb = ByteBuffer.allocate(b.len() * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : b.d) bb.putShort((short) Math.round(Math.max(-1, Math.min(1, v / max * peak)) * 32767));
        return bb.array();
    }

    static void wav(Path dir, String name, Buf b, double peak) throws IOException {
        byte[] pcm = finish(b, peak);
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(36 + pcm.length).put("WAVE".getBytes())
                .put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(SR).putInt(SR * 2)
                .putShort((short) 2).putShort((short) 16).put("data".getBytes()).putInt(pcm.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(h.array()); out.write(pcm);
        Files.write(dir.resolve(name + ".wav"), out.toByteArray());
        System.out.printf("%-30s %6.2f s%n", name + ".wav", (double) b.len() / SR);
    }

    // ------------------------------------------------------------------ SFX
    static Buf keyClick() {
        Buf b = new Buf(0.08, false);
        BP click = new BP(3800, 1.5); LP thock = new LP(900);
        for (int i = 0; i < b.len(); i++) {
            double s = (double) i / SR;
            double v = click.p(noise()) * 2.2 * Math.exp(-s / 0.004)                    // contacto del switch
                    + Math.sin(2 * Math.PI * 205 * s) * 0.55 * Math.exp(-s / 0.022)          // "thock" de la tecla
                    + (s > 0.018 ? thock.p(noise()) * 0.8 * Math.exp(-(s - 0.018) / 0.008) : 0); // tope de recorrido
            b.add(i, v);
        }
        return b;
    }

    static Buf daftBoot() {
        Buf b = new Buf(1.9, false);
        double ph = 0, ph2 = 0; LP lp = new LP(400);
        for (int i = 0; i < SR * 1.0; i++) {
            double s = (double) i / SR, f = 70 * Math.pow(320.0 / 70, s);         // barrido exponencial 70 → 320 Hz
            ph += f / SR; ph2 += (900 + 300 * Math.sin(2 * Math.PI * 7 * s)) / SR;
            lp.set(300 + 3500 * s);
            double v = lp.p(saw(ph) + 0.5 * saw(ph * 1.005));
            v = Math.round(v * 12) / 12.0;                                               // bitcrush: 12 niveles
            v += 0.12 * sq(ph2) * s;                                                     // servo
            b.add(i, v * Math.min(1, s / 0.05) * (s > 0.9 ? (1 - s) / 0.1 : 1) * 0.7);
        }
        for (double[] beep : new double[][]{{1.02, 880}, {1.16, 1320}}) {
            double p = 0;
            for (int i = 0; i < SR * 0.08; i++) { p += beep[1] / SR; b.add(at(beep[0]) + i, sq(p) * 0.25 * Math.exp(-(double) i / SR / 0.06)); }
        }
        kick(b, 1.32, 1.0, 0.25);
        pad(b, 1.32, 0.25, new int[]{57, 64, 69}, 0.35);
        return b;
    }

    /** Vocoder: portadora sierra robótica por dos formantes que se mueven entre vocales ("ha-rder, bet-ter, fas-ter"). */
    static Buf daftVocoder() {
        double[][] vowels = {{730, 1090}, {530, 1840}, {530, 1840}, {730, 1090}, {570, 840}, {730, 1090}};
        double[] pitch = {57, 57, 60, 60, 55, 57}, dur = {0.17, 0.2, 0.16, 0.2, 0.17, 0.32};
        double total = 0.15; for (double d : dur) total += d + 0.05;
        Buf b = new Buf(total, false);
        BP f1 = new BP(700, 6), f2 = new BP(1200, 8), ess = new BP(5000, 2);
        double t = 0.04, ph = 0;
        for (int k = 0; k < dur.length; k++) {
            double f = mtof(pitch[k]);
            double[] from = vowels[Math.max(0, k - 1)], to = vowels[k];
            for (int i = 0; i < SR * dur[k]; i++) {
                double s = (double) i / SR, u = Math.min(1, s / 0.05);                // 50 ms de transición entre vocales
                if (i % 32 == 0) { f1.set(from[0] + (to[0] - from[0]) * u, 6); f2.set(from[1] + (to[1] - from[1]) * u, 8); }
                ph += f / SR;
                double carrier = saw(ph) + 0.6 * saw(ph * 2.0) + 0.3 * saw(ph * 1.5);
                double env = Math.min(1, s / 0.015) * Math.min(1, (dur[k] - s) / 0.03);
                double v = (f1.p(carrier) * 1.3 + f2.p(carrier) * 0.9) * env;
                if (s < 0.03) v += ess.p(noise()) * 0.5 * (1 - s / 0.03);              // consonante
                b.add(at(t) + i, v);
            }
            t += dur[k] + 0.05;
        }
        echo(b, 0.11, 0.25, 0.25);
        return b;
    }

    /** Gaviota: "kii-ou" — sube rápido a ~1.9 kHz y cae a ~1.1 kHz, con aspereza y armónicos filtrados. */
    static void gull(Buf b, double t, double amp, double len) {
        BP body = new BP(1800, 1.5); double ph = 0;
        for (int i = 0; i < SR * len; i++) {
            double s = (double) i / SR, u = s / len;
            double f = u < 0.15 ? 1450 + 450 * (u / 0.15) : 1900 - 800 * Math.pow((u - 0.15) / 0.85, 0.8);
            f *= 1 + 0.02 * Math.sin(2 * Math.PI * 38 * s);
            ph += f / SR;
            double env = Math.sin(Math.PI * Math.min(1, u * 1.05)) * (1 + 0.3 * noise());
            b.add(at(t) + i, body.p(saw(ph) + 0.2 * noise()) * env * amp);
        }
    }

    static Buf seagulls() {
        Buf b = new Buf(2.8, false);
        gull(b, 0.05, 1.0, 0.42); gull(b, 0.55, 0.8, 0.36); gull(b, 1.0, 0.45, 0.18); gull(b, 1.22, 0.45, 0.18);
        gull(b, 1.75, 0.3, 0.45);
        LP surf = new LP(500);
        for (int i = 0; i < b.len(); i++) b.add(i, surf.p(noise()) * 0.25 * Math.sin(Math.PI * i / b.len()));
        echo(b, 0.23, 0.2, 0.2);
        return b;
    }

    static Buf cameraFlash() {
        Buf b = new Buf(1.0, false);
        for (double t : new double[]{0.0, 0.065}) {                                   // obturador: clic-clac
            LP lp = new LP(9000);
            for (int i = 0; i < SR * 0.025; i++) { double n = noise(); b.add(at(t) + i, (n - lp.p(n) * 0.5) * 1.1 * Math.exp(-(double) i / SR / 0.004)); }
        }
        kick(b, 0.0, 0.35, 0.03);
        for (int i = 0; i < SR * 0.04; i++) b.add(at(0.07) + i, noise() * 0.9 * Math.exp(-(double) i / SR / 0.01)); // "pop" del flash
        double ph = 0;
        for (int i = 0; i < SR * 0.85; i++) {                                          // recarga del condensador
            double s = (double) i / SR, f = 2500 * Math.pow(9000 / 2500.0, s / 0.85);
            ph += f / SR;
            b.add(at(0.12) + i, Math.sin(2 * Math.PI * ph) * 0.07 * Math.sin(Math.PI * s / 0.85));
        }
        return b;
    }

    static Buf sofiaSparkle() {
        Buf b = new Buf(2.4, false);
        int[] arp = {84, 88, 91, 96, 100, 103, 108};                                   // pentatónica ascendente (Do mayor)
        for (int k = 0; k < arp.length; k++) bell(b, 0.02 + k * 0.065, mtof(arp[k]), 0.32 - k * 0.02, 0.55);
        for (int g = 0; g < 40; g++) {                                                 // purpurina: pings agudos al azar
            double t = 0.05 + RND.nextDouble() * 1.0, f = 4000 + RND.nextDouble() * 5000;
            for (int i = 0; i < SR * 0.05; i++) b.add(at(t) + i, Math.sin(2 * Math.PI * f * i / SR) * 0.05 * Math.exp(-(double) i / SR / 0.012));
        }
        echo(b, 0.18, 0.4, 0.45);
        return b;
    }

    // ------------------------------------------------------------------ música en bucle
    static Buf daftSynthwave() {
        double beat = 0.5, bar = 4 * beat;
        Buf drums = new Buf(4 * bar, true), music = new Buf(4 * bar, true);
        int[][] chords = {{57, 60, 64}, {57, 60, 65}, {55, 60, 64}, {55, 59, 62}};
        int[] roots = {45, 41, 48, 43};
        for (int bi = 0; bi < 4; bi++) {
            double t0 = bi * bar;
            pad(music, t0, bar - 0.05, chords[bi], 0.22);
            for (int e = 0; e < 8; e++) sawBass(music, t0 + e * beat / 2, beat / 2 * 0.8, mtof(roots[bi] + (e % 2 == 1 ? 12 : 0)), 0.45);
            for (int s = 0; s < 16; s++) {
                int n = chords[bi][s % 3] + 12 * (1 + (s / 3) % 2);
                sawBass(music, t0 + s * beat / 4, beat / 4 * 0.6, mtof(n), 0.09);
            }
            for (int k = 0; k < 4; k++) {
                kick(drums, t0 + k * beat, 1.0, 0.28);
                hat(drums, t0 + k * beat + beat / 2, 0.35, 0.03);
                if (k % 2 == 1) snare(drums, t0 + k * beat, 0.7, 0.12, 1700);
            }
        }
        // Sidechain: tras cada bombo, la música se hunde y se recupera: g(t) = 1 − 0.55·e^(−Δt/0.11)
        for (int i = 0; i < music.len(); i++) {
            double dt = ((double) i / SR) % beat;
            music.d[i] *= (float) (1 - 0.55 * Math.exp(-dt / 0.11));
        }
        echo(music, beat * 0.75, 0.3, 0.25);
        drums.mix(music, 1.0);
        return drums;
    }

    static Buf beachWaves() {
        double len = 10;
        Buf b = new Buf(len, true);
        LP deep = new LP(380), mid = new LP(1600);
        double brown = 0;
        // El ruido se genera UNA vez y los filtros lo recorren dos veces: en la 2ª vuelta el estado de los filtros al
        // empezar es exactamente el del final, así el retumbar grave no salta en el empalme.
        double[] white = new double[b.len()], white2 = new double[b.len()];
        for (int i = 0; i < b.len(); i++) { white[i] = noise(); white2[i] = noise(); }
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < b.len(); i++) {
                double s = (double) i / SR;
                // 2 olas por bucle (periodo 5 s): la envolvente sube lenta (swell) y rompe rápido (espuma)
                double u = (s % 5.0) / 5.0, swell = Math.pow(Math.sin(Math.PI * Math.min(1, u / 0.7)), 2);
                double foam = u > 0.45 ? Math.exp(-(u - 0.45) / 0.18) : 0;
                brown = brown * 0.995 + white[i] * 0.05;
                double n = white2[i];
                double v = deep.p(brown) * 2.2 * (0.12 + 0.88 * swell) + (n - mid.p(n)) * 0.45 * foam + mid.p(n) * 0.3 * swell * swell;
                if (pass == 1) b.d[i] = (float) v;
            }
        return b;
    }

    static Buf verano() {
        double beat = 60.0 / 92, step = beat / 4, bar = 16 * step;
        Buf drums = new Buf(4 * bar, true), keys = new Buf(4 * bar, true), bass = new Buf(4 * bar, true);
        int[][] chords = {{53, 56, 60, 63}, {49, 53, 56, 60}, {56, 60, 63, 67}, {51, 55, 58, 63}};
        int[] roots = {29, 25, 32, 27};
        int[] snareSteps = {3, 6, 11, 14};                                             // dembow: "boom-ch-boom-chick"
        for (int bi = 0; bi < 4; bi++) {
            double t0 = bi * bar;
            for (int k = 0; k < 4; k++) kick(drums, t0 + k * 4 * step, 0.95, 0.3);
            for (int s : snareSteps) snare(drums, t0 + s * step, 0.55, 0.08, 2300);
            for (int s = 2; s < 16; s += 4) hat(drums, t0 + s * step, 0.22, 0.025);
            rhodes(keys, t0, 6 * step, chords[bi], 0.55);
            rhodes(keys, t0 + 6 * step, 9 * step, chords[bi], 0.42);
            for (int s : new int[]{0, 3, 8, 11}) {                                     // 808: seno con leve saturación
                double f = mtof(roots[bi] + 12);
                for (int i = 0; i < SR * step * 2.6; i++) {
                    double x = (double) i / SR, v = Math.tanh(1.8 * Math.sin(2 * Math.PI * f * x)) * Math.exp(-x / 0.35) * Math.min(1, x / 0.004);
                    bass.add(at(t0 + s * step) + i, v * 0.55);
                }
            }
        }
        lowpass(keys, 2600);
        drums.mix(keys, 0.9);
        drums.mix(bass, 1.0);
        lowpass(drums, 6500);                                                           // grano lo-fi
        vinyl(drums, 0.02, 7);
        return drums;
    }

    static Buf sofiaLofi() {
        double beat = 0.8, bar = 4 * beat;
        Buf drums = new Buf(4 * bar, true), keys = new Buf(4 * bar, true), lead = new Buf(4 * bar, true);
        int[][] chords = {{53, 57, 60, 64}, {52, 55, 59, 62}, {50, 53, 57, 60}, {48, 52, 55, 59}};
        for (int bi = 0; bi < 4; bi++) {
            double t0 = bi * bar;
            rhodes(keys, t0, bar - 0.1, chords[bi], 0.6);
            double f = mtof(chords[bi][0] - 12);
            for (double bt : new double[]{0, 2.5}) for (int i = 0; i < SR * 1.0; i++) {
                double x = (double) i / SR;
                keys.add(at(t0 + bt * beat) + i, Math.sin(2 * Math.PI * f * x) * 0.35 * Math.exp(-x / 0.6) * Math.min(1, x / 0.01));
            }
            kick(drums, t0, 0.7, 0.25);
            kick(drums, t0 + 2.5 * beat, 0.5, 0.22);
            snare(drums, t0 + beat, 0.25, 0.07, 1400);
            snare(drums, t0 + 3 * beat, 0.25, 0.07, 1400);
            for (int e = 0; e < 8; e++) hat(drums, t0 + e * beat / 2 + (e % 2 == 1 ? 0.06 : 0), e % 2 == 0 ? 0.16 : 0.1, 0.02); // swing
        }
        // melodía de saxo: {pulso de inicio, duración en pulsos, nota MIDI}
        double[][] mel = {{0, 1, 72}, {1, 0.5, 74}, {1.5, 1.5, 76}, {3, 1, 77},
                {4, 1.5, 76}, {5.5, 0.5, 74}, {6, 2, 71},
                {8, 1, 72}, {9, 1, 74}, {10, 0.5, 72}, {10.5, 1.5, 69},
                {12, 1, 67}, {13, 1, 69}, {14, 1.8, 72}};
        for (double[] m : mel) sax(lead, m[0] * beat, m[1] * beat * 0.95, m[2], 0.33);
        echo(lead, 0.3, 0.3, 0.3);
        lowpass(keys, 2200);
        drums.mix(keys, 0.85);
        drums.mix(lead, 1.0);
        lowpass(drums, 7000);
        vinyl(drums, 0.018, 6);
        return drums;
    }

    // ------------------------------------------------------------------ intro underground + mordisco de la araña

    /** Aerosol agitándose: la bola metálica golpea las paredes de la lata dos veces por sacudida (~4.5 sacudidas/s). */
    static Buf sprayShake() {
        Buf b = new Buf(1.25, false);
        double t = 0.02;
        int k = 0;
        while (t < 1.1) {
            double amp = 0.75 + RND.nextDouble() * 0.25;
            BP body = new BP(2600 + RND.nextDouble() * 1400, 3);
            double f1 = 3100 + RND.nextDouble() * 300, f2 = 4700 + RND.nextDouble() * 400;
            for (int i = 0; i < SR * 0.06; i++) {
                double s = (double) i / SR;
                double v = body.p(noise()) * 2.0 * Math.exp(-s / 0.005)                         // clac de la bola
                        + (Math.sin(2 * Math.PI * f1 * s) * 0.35 + Math.sin(2 * Math.PI * f2 * s) * 0.2) * Math.exp(-s / 0.018); // lata que resuena
                b.add(at(t) + i, v * amp);
            }
            LP slosh = new LP(500);                                                            // pintura moviéndose dentro
            for (int i = 0; i < SR * 0.09; i++) b.add(at(t) + i, slosh.p(noise()) * 0.5 * Math.sin(Math.PI * i / (SR * 0.09)));
            t += (k++ % 2 == 0) ? 0.075 + RND.nextDouble() * 0.02 : 0.13 + RND.nextDouble() * 0.03;
        }
        return b;
    }

    /** Aerosol pintando: "psst" de la válvula + siseo agudo en tres trazos, con el vaivén de la mano. */
    static Buf sprayPaint() {
        Buf b = new Buf(2.5, false);
        double[][] strokes = {{0.0, 0.72}, {0.8, 0.78}, {1.66, 0.78}};
        for (double[] st : strokes) {
            LP lp = new LP(9000); BP air = new BP(6200, 0.8);
            for (int i = 0; i < SR * 0.012; i++) b.add(at(st[0]) + i, noise() * 0.9 * Math.exp(-(double) i / SR / 0.003)); // válvula
            for (int i = 0; i < SR * st[1]; i++) {
                double s = (double) i / SR, n = noise();
                double env = Math.min(1, s / 0.03) * Math.min(1, (st[1] - s) / 0.06);
                double hand = 0.8 + 0.2 * Math.sin(2 * Math.PI * 3.1 * s + st[0] * 7);           // distancia de la boquilla al muro
                double v = (n - lp.p(n) * 0.4) * 0.35 + air.p(n) * 1.1;
                b.add(at(st[0]) + i, v * env * hand);
            }
        }
        return b;
    }

    /** Bombo de boom-bap: más cuerpo y sub que el del synthwave, con un "knock" de ataque. */
    static void bapKick(Buf b, double t, double amp) {
        double ph = 0; LP knock = new LP(2500);
        for (int i = 0; i < SR * 0.55; i++) {
            double s = (double) i / SR, f = 48 + 95 * Math.exp(-s / 0.045);
            ph += f / SR;
            double v = Math.tanh(1.6 * Math.sin(2 * Math.PI * ph)) * Math.exp(-s / 0.24)
                    + (i < 300 ? knock.p(noise()) * 0.8 * (1 - i / 300.0) : 0);
            b.add(at(t) + i, v * amp);
        }
    }

    /** Caja gorda: ruido con cuerpo de 200 Hz + palmada en capas (tres ráfagas cortas). */
    static void bapSnare(Buf b, double t, double amp) {
        snare(b, t, amp, 0.15, 1800);
        BP clap = new BP(1300, 1.4);
        for (double off : new double[]{0, 0.011, 0.023}) {
            for (int i = 0; i < SR * 0.12; i++) b.add(at(t + off) + i, clap.p(noise()) * 0.9 * amp * Math.exp(-(double) i / SR / (off > 0.02 ? 0.05 : 0.008)));
        }
    }

    /** 90 BPM, Re menor (Dm9–Bbmaj7–Gm7–A7b9): bombo pesado, caja en 2 y 4, hats con swing, piano oscuro "sampleado" y bajo. */
    static Buf boomBap() {
        double beat = 60.0 / 90, step = beat / 4, bar = 16 * step, swing = step * 0.32;
        Buf drums = new Buf(4 * bar, true), keys = new Buf(4 * bar, true), bass = new Buf(4 * bar, true);
        int[][] chords = {{50, 53, 57, 64}, {46, 50, 53, 57}, {43, 46, 50, 53}, {45, 49, 52, 58}};
        int[] roots = {38, 34, 31, 33};
        int[][] kicks = {{0, 7, 10}, {0, 10}, {0, 7, 10}, {0, 3, 10, 14}};
        for (int bi = 0; bi < 4; bi++) {
            double t0 = bi * bar;
            for (int s : kicks[bi]) bapKick(drums, t0 + s * step + (s % 2 == 1 ? swing : 0), s == 0 ? 1.0 : 0.8);
            bapSnare(drums, t0 + 4 * step, 0.8);
            bapSnare(drums, t0 + 12 * step, 0.85);
            if (bi == 3) snare(drums, t0 + 15 * step + swing, 0.25, 0.05, 1900);       // ghost note antes de la vuelta
            for (int s = 0; s < 16; s += 2) hat(drums, t0 + s * step + (s % 4 == 2 ? swing : 0), s % 4 == 0 ? 0.26 : 0.16, 0.022);
            // piano: un golpe largo en el 1 y un "chop" corto en el 2-y (como sample cortado del vinilo)
            rhodes(keys, t0, 5 * step, chords[bi], 0.7);
            rhodes(keys, t0 + 6 * step + swing, 2 * step, chords[bi], 0.45);
            rhodes(keys, t0 + 14 * step, 1.5 * step, new int[]{chords[bi][3] + 12}, 0.22);
            for (double[] n : new double[][]{{0, 6}, {7, 3}, {10, 6}}) {                     // bajo redondo con leve saturación
                double f = mtof(roots[bi] + (n[0] == 7 ? 12 : 0));
                for (int i = 0; i < SR * n[1] * step; i++) {
                    double x = (double) i / SR, dur = n[1] * step;
                    double env = Math.min(1, x / 0.008) * (x < dur - 0.03 ? 1 : (dur - x) / 0.03);
                    bass.add(at(t0 + n[0] * step + (n[0] == 7 ? swing : 0)) + i, Math.tanh(1.4 * Math.sin(2 * Math.PI * f * x)) * env * 0.5);
                }
            }
        }
        lowpass(keys, 1700);                                                                // piano opaco, de disco viejo
        echo(keys, beat * 0.75, 0.22, 0.18);
        drums.mix(keys, 0.95);
        drums.mix(bass, 1.0);
        lowpass(drums, 7500);
        vinyl(drums, 0.022, 9);
        return drums;
    }

    /** Telaraña: el hilo vibra mientras la araña baja (glissando 1.6 kHz → 260 Hz con trémolo) y frena con un "boing". */
    static Buf spiderWebDrop() {
        Buf b = new Buf(1.35, false);
        double ph = 0;
        for (int i = 0; i < SR * 1.0; i++) {
            double s = (double) i / SR, f = 1600 * Math.pow(260 / 1600.0, Math.pow(s, 0.8));
            ph += f / SR;
            double trem = 0.6 + 0.4 * Math.sin(2 * Math.PI * 23 * s);
            double env = Math.min(1, s / 0.05) * (1 - s * 0.6);
            b.add(i, (Math.sin(2 * Math.PI * ph) * 0.5 + saw(ph) * 0.12) * trem * env);
        }
        double p2 = 0;
        for (int i = 0; i < SR * 0.35; i++) {                                               // rebote elástico al frenar
            double s = (double) i / SR, f = 110 * (1 + 0.35 * Math.sin(2 * Math.PI * 9 * s) * Math.exp(-s / 0.12));
            p2 += f / SR;
            b.add(at(0.98) + i, Math.sin(2 * Math.PI * p2) * 0.7 * Math.exp(-s / 0.12));
        }
        return b;
    }

    /** Mordisco: dos "chomps" crujientes, golpe sub que cae y veneno digital (cuadradas bitcrush que saltan de tono). */
    static Buf spiderBite() {
        Buf b = new Buf(1.3, false);
        for (double t : new double[]{0.0, 0.07}) {
            BP crunch = new BP(900, 1.1); LP lp = new LP(5000);
            for (int i = 0; i < SR * 0.09; i++) {
                double s = (double) i / SR, n = noise();
                b.add(at(t) + i, (crunch.p(n) * 2.4 + lp.p(n) * 0.5) * Math.exp(-s / 0.025) * (t == 0 ? 1 : 0.8));
            }
        }
        double ph = 0;
        for (int i = 0; i < SR * 0.6; i++) {                                                 // impacto sub 120 → 32 Hz
            double s = (double) i / SR, f = 32 + 90 * Math.exp(-s / 0.07);
            ph += f / SR;
            b.add(at(0.02) + i, Math.tanh(2 * Math.sin(2 * Math.PI * ph)) * Math.exp(-s / 0.3) * 0.9);
        }
        double p = 0, f = 300;
        for (int i = 0; i < SR * 0.85; i++) {                                                // veneno que se esparce
            double s = (double) i / SR;
            if (i % (int) (SR * 0.035) == 0) f = 140 + RND.nextDouble() * 900;
            p += f / SR;
            double v = Math.round(sq(p) * (0.6 + 0.4 * Math.sin(2 * Math.PI * 17 * s)) * 4) / 4.0;
            b.add(at(0.12) + i, v * 0.22 * Math.min(1, s / 0.05) * Math.exp(-s / 0.45));
        }
        echo(b, 0.09, 0.3, 0.3);
        return b;
    }

    // ------------------------------------------------------------------ SALIR: el atardecer

    /**
     * Cuerda pulsada Karplus-Strong: una línea de retardo de SR/f muestras arranca con ruido y en cada vuelta se
     * promedia con su vecina (pasa-bajos) y pierde un poco de energía: suena a guitarra de cuerdas de acero.
     */
    static void pluck(Buf b, double t, double f, double amp, double decay) {
        int n = (int) Math.round(SR / f);
        double[] line = new double[n];
        for (int i = 0; i < n; i++) line[i] = noise();
        int len = (int) (SR * 4.0);
        for (int i = 0, k = 0; i < len; i++, k = (k + 1) % n) {
            double v = line[k];
            line[k] = decay * 0.5 * (line[k] + line[(k + 1) % n]);
            b.add(at(t) + i, v * amp * Math.min(1, i / 40.0));
        }
    }

    /**
     * Nota de guitarra acústica por síntesis ADITIVA (a diferencia de Karplus-Strong, permite deslizar la afinación):
     * 9 armónicos de amplitud 1/k^1.25 cuyas caídas se aceleran con k (los agudos mueren antes, como en la cuerda
     * real), ataque de púa (ruido filtrado de 7 ms), "slide" exponencial de {@code from} a {@code to} en
     * {@code slide} s y vibrato de 5.2 Hz que entra tarde (como hace la mano del guitarrista).
     */
    static void guitarNote(Buf b, double t, double from, double to, double slide, double dur, double amp, double vib) {
        double f0 = mtof(from), f1 = mtof(to);
        double[] ph = new double[10];
        BP pick = new BP(2600, 1.2);
        LP body = new LP(3800);
        for (int i = 0; i < SR * (dur + 0.4); i++) {
            double s = (double) i / SR;
            double f = s < slide ? f0 * Math.pow(f1 / f0, s / slide) : f1;
            f *= 1 + vib * 0.006 * Math.sin(2 * Math.PI * 5.2 * s) * Math.min(1, Math.max(0, (s - 0.35) / 0.4));
            double v = 0;
            for (int k = 1; k <= 9; k++) {
                ph[k] += f * k / SR;
                v += Math.sin(2 * Math.PI * ph[k]) / Math.pow(k, 1.25) * Math.exp(-s * (0.9 + 0.55 * k));
            }
            double release = s < dur ? 1 : Math.exp(-(s - dur) / 0.08);
            if (i < SR * 0.007) v += pick.p(noise()) * 1.5 * (1 - i / (SR * 0.007));
            b.add(at(t) + i, body.p(v) * Math.min(1, s / 0.002) * release * amp);
        }
    }

    /**
     * SALIR — solo de guitarra acústica, melancólico y lento (~64 BPM, La menor: Am – F – C – Am). Bajos y arpegios
     * suaves con cuerda pulsada (Karplus-Strong) y encima la melodía con slides y vibrato. 8.4 s con fundido final:
     * la app se cierra cuando termina.
     */
    static Buf guitarFarewell() {
        Buf b = new Buf(8.4, false);
        int[][] chords = {{45, 52, 57, 60}, {41, 48, 53, 57}, {48, 52, 55, 60}, {45, 52, 57, 64}};
        double[] at = {0.0, 2.05, 4.1, 6.15};
        for (int c = 0; c < 4; c++) {                                                      // acompañamiento fingerpicking
            pluck(b, at[c], mtof(chords[c][0] - 12), 0.34, 0.9975);
            for (int k = 1; k < 4; k++) pluck(b, at[c] + 0.32 * k + RND.nextDouble() * 0.01, mtof(chords[c][k]), 0.12, 0.996);
        }
        // melodía: {inicio, nota desde, nota hasta, duración del slide, duración, vibrato}
        double[][] mel = {
                {0.35, 64, 64, 0.0, 1.05, 1}, {1.45, 67, 69, 0.11, 1.15, 1}, {2.70, 72, 72, 0.0, 0.48, 0},
                {3.20, 71, 71, 0.0, 0.42, 0}, {3.65, 69, 69, 0.0, 1.25, 1}, {5.00, 67, 67, 0.0, 0.48, 0},
                {5.50, 64, 64, 0.0, 0.46, 0}, {6.00, 62, 64, 0.16, 2.0, 1.4}};
        for (double[] m : mel) guitarNote(b, m[0], m[1], m[2], m[3], m[4], 0.5, m[5]);
        echo(b, 0.29, 0.3, 0.22);                                                           // sala pequeña de madera
        for (int i = 0; i < b.len(); i++) {
            double s = (double) i / SR;
            b.d[i] *= (float) (Math.min(1, s / 0.05) * Math.min(1, (8.4 - s) / 1.6));
        }
        return b;
    }

    // ------------------------------------------------------------------ FEID / FERXXOCALIPSIS

    /**
     * Respaldo de {@code feid_theme}: dembow de estadio a 96 BPM en Sol menor (Gm – Eb – Bb – F). Bombo en 1 y 3,
     * caja "dembow", 808 con glissando entre notas, lead de sierra brillante con sidechain y rumor de público.
     * Si se pone {@code feid_theme.mp3} en la carpeta, el AudioManager lo prefiere a este WAV.
     */
    static Buf feidTheme() {
        double beat = 60.0 / 96, step = beat / 4, bar = 16 * step;
        Buf drums = new Buf(4 * bar, true), synth = new Buf(4 * bar, true), bass = new Buf(4 * bar, true), crowd = new Buf(4 * bar, true);
        int[][] chords = {{55, 58, 62}, {51, 55, 58}, {58, 62, 65}, {53, 57, 60}};
        int[] roots = {31, 27, 34, 29};
        int[] snareSteps = {3, 6, 11, 14};
        for (int bi = 0; bi < 4; bi++) {
            double t0 = bi * bar;
            bapKick(drums, t0, 1.0);
            bapKick(drums, t0 + 8 * step, 0.95);
            for (int s : snareSteps) snare(drums, t0 + s * step, 0.6, 0.07, 2400);
            for (int s = 0; s < 16; s += 2) hat(drums, t0 + s * step, s % 4 == 2 ? 0.3 : 0.18, 0.02);
            // 808: notas largas que se deslizan a la siguiente fundamental (el "slide" del reggaetón)
            double fFrom = mtof(roots[bi] + 12), fTo = mtof(roots[(bi + 1) % 4] + 12), ph = 0;
            for (int i = 0; i < SR * bar; i++) {
                double x = (double) i / SR, u = x / bar;
                double f = u < 0.88 ? fFrom : fFrom * Math.pow(fTo / fFrom, (u - 0.88) / 0.12);
                ph += f / SR;
                double env = Math.min(1, x / 0.005) * (0.55 + 0.45 * Math.exp(-((x % (8 * step)) / 0.5)));
                bass.add(at(t0) + i, Math.tanh(1.7 * Math.sin(2 * Math.PI * ph)) * env * 0.5);
            }
            // lead: arpegio de sierra en corcheas, una octava arriba en la segunda mitad del compás
            for (int s = 0; s < 16; s += 2) {
                int n = chords[bi][(s / 2) % 3] + (s >= 8 ? 24 : 12);
                sawBass(synth, t0 + s * step, step * 1.6, mtof(n), 0.14);
            }
            pad(synth, t0, bar - 0.05, chords[bi], 0.12);
        }
        for (int i = 0; i < synth.len(); i++) {                                            // sidechain con el bombo
            double dt = ((double) i / SR) % (8 * step);
            synth.d[i] *= (float) (1 - 0.5 * Math.exp(-dt / 0.13));
        }
        echo(synth, beat * 0.75, 0.3, 0.25);
        BP roar = new BP(900, 0.6);                                                        // rumor de estadio
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < crowd.len(); i++) {
                double s = (double) i / SR, v = roar.p(noise()) * (0.5 + 0.2 * Math.sin(2 * Math.PI * s / bar));
                if (pass == 1) crowd.d[i] = (float) (v * 0.18);
            }
        drums.mix(bass, 1.0);
        drums.mix(synth, 0.9);
        drums.mix(crowd, 1.0);
        return drums;
    }

    /** El pisotón del Ferxxo: golpe sub en el escenario, crujido de araña aplastada y el estadio que explota. */
    static Buf feidStomp() {
        Buf b = new Buf(2.0, false);
        bapKick(b, 0.0, 1.0);
        double ph = 0;
        for (int i = 0; i < SR * 0.7; i++) {                                              // sub del escenario de madera
            double s = (double) i / SR, f = 38 + 70 * Math.exp(-s / 0.06);
            ph += f / SR;
            b.add(i, Math.tanh(2.2 * Math.sin(2 * Math.PI * ph)) * Math.exp(-s / 0.35) * 0.8);
        }
        BP crunch = new BP(1500, 1.5);
        for (int i = 0; i < SR * 0.08; i++) b.add(at(0.01) + i, crunch.p(noise()) * 1.6 * Math.exp(-(double) i / SR / 0.02));
        BP roar = new BP(1100, 0.5); LP soft = new LP(4000);
        for (int i = 0; i < SR * 1.7; i++) {                                              // el público: sube y se sostiene
            double s = (double) i / SR, env = Math.min(1, s / 0.35) * Math.min(1, (1.7 - s) / 0.6);
            b.add(at(0.15) + i, soft.p(roar.p(noise())) * 1.4 * env * (0.85 + 0.15 * Math.sin(2 * Math.PI * 7 * s)));
        }
        return b;
    }

    /** Destello de los grills de oro: "ting" metálico brillante + barrido de brillo hacia arriba. */
    static Buf grillShine() {
        Buf b = new Buf(1.2, false);
        bell(b, 0.0, 2637, 0.5, 0.35);
        bell(b, 0.04, 3951, 0.3, 0.3);
        double ph = 0;
        for (int i = 0; i < SR * 0.45; i++) {
            double s = (double) i / SR, f = 3000 * Math.pow(3, s / 0.45);
            ph += f / SR;
            b.add(at(0.02) + i, Math.sin(2 * Math.PI * ph) * 0.08 * Math.sin(Math.PI * s / 0.45));
        }
        echo(b, 0.12, 0.35, 0.35);
        return b;
    }

    // ------------------------------------------------------------------ FERXXO RIDER: la moto

    /**
     * Una muestra de motor de dos tiempos con fase {@code ph} (ciclos a la frecuencia de encendido): seno saturado
     * (el "bap" grueso), segundo armónico de sierra (el zumbido metálico) y un golpe de ruido al inicio de cada ciclo
     * (la explosión que sale por el escape). Eso es lo que le da el "rrrrr" granulado.
     */
    static double engine(double ph, double growl) {
        double frac = ph - Math.floor(ph);
        double fire = Math.tanh(3.2 * Math.sin(2 * Math.PI * ph)) * 0.55 + saw(ph * 2) * 0.25;
        return fire + Math.exp(-frac * 9) * noise() * growl;
    }

    /** Moto que se acerca por el callejón (2.3 s): revoluciones 55 → 150 Hz, cambio de marcha a 1.0 s y a tope. */
    static Buf motoEngine() {
        Buf b = new Buf(2.3, false);
        LP lp = new LP(600);
        double ph = 0;
        for (int i = 0; i < SR * 2.3; i++) {
            double s = (double) i / SR, f;
            if (s < 1.0) f = 55 + 95 * Math.pow(s, 0.7);
            else if (s < 1.07) f = 150 - 55 * (s - 1.0) / 0.07;                                // corte al meter el cambio
            else f = 95 + 90 * Math.pow(Math.min(1, (s - 1.07) / 0.95), 0.8);
            ph += f / SR;
            lp.set(380 + 3400 * Math.min(1, s / 1.8));                                           // de lejos suena apagada
            double amp = (0.12 + 0.88 * Math.min(1, Math.pow(s / 1.75, 1.6))) * Math.min(1, (2.3 - s) / 0.15);
            b.add(i, lp.p(engine(ph, 0.6)) * amp);
        }
        return b;
    }

    /** El derrape (0.95 s): chillido de llanta (ruido en bandas estrechas + tono que tiembla), gravilla y el motor que baja. */
    static Buf motoDrift() {
        Buf b = new Buf(1.0, false);
        BP sq1 = new BP(2300, 6), sq2 = new BP(3400, 8);
        LP grit = new LP(1200);
        double ph = 0, pe = 0;
        for (int i = 0; i < SR * 0.95; i++) {
            double s = (double) i / SR, env = Math.min(1, s / 0.03) * Math.pow(Math.max(0, 1 - s / 0.95), 0.8);
            double f = 1900 + 160 * Math.sin(2 * Math.PI * 11 * s) - 500 * s;
            ph += f / SR;
            double n = noise();
            double screech = sq1.p(n) * 2.2 + sq2.p(n) * 1.4 + Math.sin(2 * Math.PI * ph) * 0.18;
            double gravel = (RND.nextDouble() < 0.004 ? noise() * 0.9 : 0) + grit.p(n) * 0.25;
            pe += (160 - 90 * Math.min(1, s / 0.8)) / SR;
            b.add(i, (screech * 0.6 + gravel + engine(pe, 0.4) * 0.35 * (1 - s)) * env);
        }
        return b;
    }

    /** La rueda pasa sobre El Guachimán: golpe sub, doble crujido y el glitch digital que se dispersa. */
    static Buf motoCrush() {
        Buf b = new Buf(0.9, false);
        bapKick(b, 0.0, 0.9);
        double ph = 0;
        for (int i = 0; i < SR * 0.5; i++) {
            double s = (double) i / SR, f = 36 + 64 * Math.exp(-s / 0.06);
            ph += f / SR;
            b.add(i, Math.tanh(2.2 * Math.sin(2 * Math.PI * ph)) * Math.exp(-s / 0.25) * 0.75);
        }
        for (double[] c : new double[][]{{0.0, 1400}, {0.05, 800}}) {
            BP crunch = new BP(c[1], 1.3);
            for (int i = 0; i < SR * 0.09; i++) b.add(at(c[0]) + i, crunch.p(noise()) * 2.0 * Math.exp(-(double) i / SR / 0.025));
        }
        double p = 0, f = 400;
        for (int i = 0; i < SR * 0.35; i++) {
            double s = (double) i / SR;
            if (i % (int) (SR * 0.03) == 0) f = 200 + RND.nextDouble() * 1200;
            p += f / SR;
            b.add(at(0.08) + i, Math.round(sq(p) * 3) / 3.0 * 0.18 * Math.exp(-s / 0.18));
        }
        return b;
    }

    /** "¡Rrrnnn!" del pique (0.5 s): el acelerador a fondo, 80 → 215 Hz en 0.16 s, y suelta. */
    static Buf motoRev() {
        Buf b = new Buf(0.5, false);
        LP lp = new LP(2600);
        double ph = 0;
        for (int i = 0; i < SR * 0.5; i++) {
            double s = (double) i / SR;
            double f = s < 0.16 ? 80 + 135 * Math.pow(s / 0.16, 0.6) : 215 - 50 * (s - 0.16) / 0.34;
            ph += f / SR;
            double env = Math.min(1, s / 0.015) * Math.min(1, (0.5 - s) / 0.2);
            b.add(i, lp.p(engine(ph, 0.7)) * env);
        }
        return b;
    }

    /** El choque (1.3 s): frenazo, golpe metálico (campanas inarmónicas + bombo), piezas rodando y el motor que se apaga. */
    static Buf motoCrash() {
        Buf b = new Buf(1.3, false);
        BP sq = new BP(2600, 7);
        for (int i = 0; i < SR * 0.28; i++) {
            double s = (double) i / SR;
            b.add(i, sq.p(noise()) * 1.8 * Math.min(1, s / 0.02));
        }
        bapKick(b, 0.27, 1.0);
        bell(b, 0.27, 420, 0.35, 0.25);
        bell(b, 0.27, 977, 0.28, 0.18);
        bell(b, 0.28, 1630, 0.22, 0.12);
        LP lp = new LP(3000);
        for (int i = 0; i < SR * 0.25; i++) b.add(at(0.27) + i, lp.p(noise()) * 1.2 * Math.exp(-(double) i / SR / 0.06));
        for (int k = 0; k < 10; k++) {                                                      // piezas rebotando
            double t = 0.38 + RND.nextDouble() * 0.75;
            BP tick = new BP(1800 + RND.nextDouble() * 2500, 4);
            double amp = 0.9 * (1 - (t - 0.38) / 0.9);
            for (int i = 0; i < SR * 0.04; i++) b.add(at(t) + i, tick.p(noise()) * amp * Math.exp(-(double) i / SR / 0.008));
        }
        double ph = 0;
        for (int i = 0; i < SR * 0.6; i++) {                                                // el motor muere
            double s = (double) i / SR;
            ph += (120 - 90 * s / 0.6) / SR;
            b.add(at(0.3) + i, engine(ph, 0.5) * 0.3 * (1 - s / 0.6));
        }
        return b;
    }

    /**
     * FERXXO RIDER: "El Cafexito" (0.5 s). Un sorbo corto (ruido pasa-banda que sube 700 → 2400 Hz, como aire por un
     * pitillo) y encima un arpegio brillante de power-up en La mayor (A5–C#6–E6–A6, campanitas de 2 parciales).
     */
    static Buf cafexito() {
        Buf b = new Buf(0.55, false);
        BP slurp = new BP(700, 5);
        for (int i = 0; i < SR * 0.14; i++) {
            double s = (double) i / SR;
            slurp.set(700 + 1700 * s / 0.14, 5);
            double env = Math.min(1, s / 0.01) * (1 - s / 0.14);
            b.add(i, slurp.p(noise()) * 1.6 * env);
        }
        double[] notes = {81, 85, 88, 93};
        for (int k = 0; k < notes.length; k++) {
            double t = 0.07 + k * 0.055, f = mtof(notes[k]);
            for (int i = 0; i < SR * 0.3; i++) {
                double s = (double) i / SR, env = Math.min(1, s / 0.004) * Math.exp(-s / (k == 3 ? 0.16 : 0.07));
                double v = Math.sin(2 * Math.PI * f * s) + 0.35 * Math.sin(2 * Math.PI * f * 2.01 * s) + 0.12 * sq(f * s * 0.5);
                b.add(at(t) + i, v * 0.32 * env);
            }
        }
        return b;
    }

    public static void main(String[] a) throws IOException {
        Path dir = Path.of(a[0]);
        Files.createDirectories(dir);
        wav(dir, "key_click", keyClick(), 0.9);
        // Retirados (Daft Punk, Bad Bunny y Feid suenan SOLO con sus MP3 oficiales): se siguen sintetizando sin escribir
        // el archivo para que la semilla 42 avance igual y el resto de WAV salgan idénticos byte a byte.
        daftBoot();
        daftVocoder();
        seagulls();
        wav(dir, "camera_flash", cameraFlash(), 0.89);
        wav(dir, "sofia_magic_sparkle", sofiaSparkle(), 0.85);
        daftSynthwave();
        beachWaves();
        verano();
        wav(dir, "sofia_theme", sofiaLofi(), 0.78);
        // nuevos al FINAL: el generador comparte la semilla 42, así los 10 de arriba salen idénticos
        wav(dir, "spray_shake", sprayShake(), 0.85);
        wav(dir, "spray_paint", sprayPaint(), 0.7);
        wav(dir, "boom_bap_loop", boomBap(), 0.82);
        wav(dir, "spider_web_drop", spiderWebDrop(), 0.75);
        wav(dir, "spider_bite", spiderBite(), 0.9);
        wav(dir, "guitar_farewell", guitarFarewell(), 0.82);
        feidTheme();
        feidStomp();   // feid_stomp.wav se retiró con el estadio; se sigue sintetizando para no mover la semilla 42
        wav(dir, "grill_shine", grillShine(), 0.8);
        wav(dir, "moto_engine", motoEngine(), 0.85);
        wav(dir, "moto_drift", motoDrift(), 0.8);
        wav(dir, "moto_crush", motoCrush(), 0.9);
        wav(dir, "moto_rev", motoRev(), 0.85);
        wav(dir, "moto_crash", motoCrash(), 0.9);
        wav(dir, "cafexito", cafexito(), 0.85);
    }
}
