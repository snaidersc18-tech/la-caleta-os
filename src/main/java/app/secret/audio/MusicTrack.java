package app.secret.audio;

/**
 * Pistas largas en bucle (MediaPlayer, {@code MediaPlayer.INDEFINITE}: streaming, no se cargan enteras en memoria).
 * Cada una se busca en {@code src/main/resources/audio/<archivo>} probando {@code .mp3 → .m4a → .wav → .aiff}.
 * <p>
 * Los temas de Daft Punk, Bad Bunny y Feid son los MP3 oficiales que entregó el usuario, y llevan su mapa de ritmo
 * MEDIDO sobre el audio (análisis de onsets del PCM decodificado: peine fino de periodo + fase sobre toda la canción):
 * {@link #bpm}, {@link #firstBeatMs} (dónde cae el primer golpe) y {@link #dropMs} (el impacto al que se ancla la
 * cinemática del modo). Con eso {@link AudioManager#beats()} convierte el currentTime real en "número de beat".
 */
public enum MusicTrack {
    /** DAFTPUNK: "Harder, Better, Faster, Stronger". 123.48 BPM; bombo y bajo entran en el compás 10 (19.630 s). */
    DAFT_PUNK_THEME("daft_punk_theme", 0.9, 123.48, 194, 19_630),
    /** BADBUNNY / CONCHO: 108 BPM; el drop (−23 → −12 dB, el grave explota) cae en el compás 8 (17.912 s). */
    BAD_BUNNY_THEME("bad_bunny_theme", 0.85, 108.00, 134, 17_912),
    /** FEID / FERXXO: 97 BPM; el drop (el grave sube +31 dB) cae en el compás 13 (32.555 s). A todo volumen. */
    FEID_THEME("feid_theme", 1.0, 97.00, 390, 32_555),
    /**
     * SOFIA / PICAKU: Rauw Alejandro, "Algo Mágico" (MP3 del usuario; cinemática e Interfaz de Sofía sin cortes).
     * 99.75 BPM (hi-hats y mezcla completa; el pico a 74.85 es el 3-3-2 del dembow). Los acordes de la intro caen
     * en la rejilla desde el primero (45 ms): beat 8 = 4.87 s, beat 16 = 9.67 s. La entrada completa (agudos +17 dB)
     * es a los 10.240 s: ahí termina la cinemática. El bajo de la canción entra a los 28.16 s.
     */
    SOFIA_THEME("sofia_theme", 0.75, 99.75, 45, 10_240),
    /**
     * SALIR: Thiago Navarro, "Bonito, Bonito" (MP3 del usuario). Bombo constante a ≈99 BPM (compás ≈2.42 s). Las tres
     * frases que coreografía {@code CafecitoExit} empiezan en compases alternos tras los respiros de la voz (19.6–20.6 s,
     * 25.0–25.4 s, 29.7–29.9 s): ≈20.65 / 25.5 / 30.0 s, y la tercera termina ≈33.0 s. {@link #dropMs} = primera frase.
     */
    CAFECITO_THEME("cafecito_theme", 0.9, 99.0, 100, 20_650),
    /**
     * BBYWOW: KAROL G, Judeline, rusowsky, "BbY WOW" (MP3 del usuario). 120.0 BPM, golpes en x.4 / x.9 s. A los 28.6 s
     * se cortan los hi-hats (el quiebre de la voz); a los 31.80 s toda la mezcla calla 60 ms y a los 31.860 s entra el
     * DROP ({@link #dropMs}): ahí el flash revela el concierto. Re-drops tras los cortes de bajo: 47.9 s y 62.9 s.
     * A los 69.6 s el bajo se apaga de golpe (≈1:09): fin del concierto.
     */
    BBYWOW_THEME("bbywow_theme", 0.9, 120.0, 400, 31_860),
    /**
     * CALLE (menú principal): Alcolirykoz, "El Salón De La Injusticia". 0–14.06 s = intro hablada de Jaime Garzón
     * (dos bloques de voz: 0.0–5.9 s y 6.3–14.0 s); a los 14.060 s cae el boom-bap (el grave 30–110 Hz salta de
     * −27 a −6 dB en 40 ms). ≈71 BPM por autocorrelación del grave. La intro arranca la pista en 0 y revela la
     * interfaz en {@link #dropMs}; los bucles y el regreso con CALLE empiezan directo en el beat (ver
     * {@link #loopsFromDrop}).
     */
    CALLE_THEME("alcolirykoz_injusticias", 0.85, 71.10, 14_060, 14_060);

    final String file;
    final double volume;
    /** Pulsos por minuto medidos (0 = pista sin mapa de ritmo). */
    public final double bpm;
    /** Milisegundos del primer beat dentro del archivo. */
    public final double firstBeatMs;
    /** Milisegundos del drop / impacto principal (el ancla de la cinemática). */
    public final double dropMs;

    MusicTrack(String file, double volume, double bpm, double firstBeatMs, double dropMs) {
        this.file = file; this.volume = volume; this.bpm = bpm; this.firstBeatMs = firstBeatMs; this.dropMs = dropMs;
    }

    /** true = al repetirse, la pista vuelve a {@link #dropMs} y no al ms 0 (la intro hablada suena una sola vez). */
    public boolean loopsFromDrop() { return this == CALLE_THEME; }

    /** Duración de un beat en ms. */
    public double beatMs() { return 60_000.0 / bpm; }
}
