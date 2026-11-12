package app.secret.audio;

/**
 * Efectos cortos (se cargan enteros en memoria con AudioClip: latencia mínima y se pueden solapar).
 * El archivo se busca en {@code src/main/resources/audio/<archivo>.wav} (o .mp3 / .aiff).
 */
public enum SoundEffect {
    /** Interfaz base: tecla mecánica (se reproduce con tono aleatorio para que no suene a ametralladora). */
    KEY_CLICK("key_click", 0.35),
    /** Bad Bunny: obturador + flash real de cámara cuando Concho toma la foto. */
    CAMERA_FLASH("camera_flash", 1.0),
    /** Intro underground: lata de aerosol agitándose (la bola metálica golpeando). */
    SPRAY_SHAKE("spray_shake", 0.9),
    /** Intro underground: siseo del aerosol mientras se pinta el tag. */
    SPRAY_PAINT("spray_paint", 0.75),
    /** CALLE: el hilo de telaraña vibrando mientras El Guachimán baja. */
    SPIDER_WEB_DROP("spider_web_drop", 0.7),
    /** CALLE: el mordisco (crujido + golpe sub + veneno digital). */
    SPIDER_BITE("spider_bite", 1.0),
    /** FEID: destello de los grills de oro. */
    GRILL_SHINE("grill_shine", 0.85),
    /** FEID: moto de motocross de dos tiempos que se acerca por el callejón y sube de revoluciones. */
    MOTO_ENGINE("moto_engine", 0.95),
    /** FEID: el derrape (chillido de llanta + gravilla) al cruzar la moto. */
    MOTO_DRIFT("moto_drift", 0.85),
    /** FEID: la rueda pasa por encima de El Guachimán (golpe sub + crujido + glitch). */
    MOTO_CRUSH("moto_crush", 1.0),
    /** FERXXO RIDER: el "¡Rrrnnn!" del pique en cada salto. */
    MOTO_REV("moto_rev", 0.8),
    /** FERXXO RIDER: frenada y choque (chillido + golpe metálico + piezas rodando). */
    MOTO_CRASH("moto_crash", 1.0),
    /** FERXXO RIDER: agarrar "El Cafexito" (sorbo + arpegio de power-up). */
    CAFEXITO("cafexito", 0.9);

    final String file;
    final double volume;

    SoundEffect(String file, double volume) { this.file = file; this.volume = volume; }
}
