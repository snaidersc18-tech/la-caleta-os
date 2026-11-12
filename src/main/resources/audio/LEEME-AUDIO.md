# Audio del Ministerio Del Rebusque

## Los temas oficiales de los modos (solo esta música)
En Daft Punk, Bad Bunny y Feid suena **únicamente** su canción oficial, en bucle infinito (`MediaPlayer.INDEFINITE`).
No hay ambiente, ni beats de respaldo, ni pistas secundarias. `AudioManager` busca la música en el orden
`.mp3 → .m4a → .wav → .aiff`.

| Archivo | Modo | BPM medido | 1.er beat | Drop (ancla de la cinemática) |
|---|---|---|---|---|
| `daft_punk_theme.mp3` | DAFTPUNK ("Harder, Better, Faster, Stronger") | 123.48 | 194 ms | 19.630 s: entran bombo y bajo (compás 10) |
| `bad_bunny_theme.mp3` | BADBUNNY / CONCHO | 108.00 | 134 ms | 17.912 s: el drop (−23 → −12 dB, compás 8) |
| `feid_theme.mp3` | FEID / FERXXO (a todo volumen) | 97.00 | 390 ms | 32.555 s: el grave sube +31 dB (compás 13) |
| `sofia_theme.mp3` | SOFIA y modo amor PICAKU ("Algo Mágico", Rauw Alejandro) | 99.75 | 45 ms | 10.240 s: entrada completa (agudos +17 dB); fin de la cinemática. Golpes de la intro en `SofiaCinematic` (2.605 / 4.870 / 7.865 / 8.820 / 9.670 s). El retrato, las ondas y los corazones reaccionan al espectro en vivo (`MusicReactor`) |
| `cafecito_theme.mp3` | SALIR ("Bonito, Bonito", Thiago Navarro) | ≈99 | — | 20.65 s: primera frase. Frases ≈20.65 / 25.5 / 30.0 s (respiros de la voz alineados a compases); fundido a negro 32.7 s y `Platform.exit()` a los 33.0 s (`CafecitoExit`) |
| `bbywow_theme.mp3` | BBYWOW ("BbY WOW", KAROL G, Judeline, rusowsky) | 120.0 | 400 ms | 31.860 s: la mezcla calla 60 ms y revienta (flashazo). Quiebre (hi-hats fuera) 28.6 s → fin del descenso 28.9 s; re-drops 47.9 / 62.9 s; a los 69.6 s el bajo se apaga: fin del concierto (`BbyWowCinematicView`). Las frases (archivo opcional `lyrics/bbywow.lrc`) explotan como fuegos artificiales en su milisegundo (`LyricFireworks`) |

- El mapa de ritmo vive en `MusicTrack` (`bpm`, `firstBeatMs`, `dropMs`). Se midió sobre el PCM de cada canción:
  flujo espectral (más peso a < 150 Hz), peine fino de periodo y fase sobre toda la pista y energía por compás.
  **Si cambias una canción, vuelve a medir y actualiza esos tres números.**
- Los archivos que llegaron como `.m4a` eran MP4 fragmentados (DASH) con AAC; el JavaFX de Windows no abre ese
  contenedor ni trae decodificador AAC en todos los equipos, así que se transcodificaron a MP3 320 kbps con Media
  Foundation de Windows. El MP3 de Feid se usa tal cual.

## Sincronía audio ↔ animación
- `AudioManager.beats()` convierte el `currentTime` REAL del MediaPlayer en número de beat (fase 0 = el bombo).
- Cada preludio arranca su canción en `dropMs − instante del impacto en el guion` y ancla el guion al audio; un
  seguidor corrige la velocidad del guion cada frame. El impacto (DROP! de Daft Punk, estallido del atardecer de Bad
  Bunny) cae en el drop, medido a +9…+22 ms (un frame). En Feid, el reloj del callejón ES la posición de la canción:
  el aplastón cae 8 beats antes del drop y el blanco puro, en el drop.
- `BeatSync` mantiene en fase las animaciones cíclicas (cabeceo de los Daft-Bots con `TranslateTransition` de un beat,
  estrobos, baile de la mascota y de Concho) y las recoloca si se desvían más de 18 ms. `LightShow` cambia de color en
  cada golpe y la terminal teclea una palabra por beat.
- **Voces de Daft Punk**: `VocalCues` tiene el instante de cada "harder / better / faster / stronger" del MP3 (en beats
  de la rejilla de arriba) y `VocalHud` las muestra con su estrobo leyendo `currentTimeProperty()` vía
  `AudioManager.onMusicTime`. **Si cambias `daft_punk_theme.mp3`, hay que volver a medir esas marcas.**
- CALLE o cambio de modo: `stop()` + `dispose()` inmediato de la música y de los efectos (`AudioManager.stopAll()`);
  dos canciones nunca se enciman.

## Otra música (MediaPlayer, en bucle)
| Archivo | Cuándo suena |
|---|---|
| `alcolirykoz_injusticias` | CALLE (menú principal), "El Salón De La Injusticia" (MP3 del usuario). La intro de arranque la dispara al terminar el graffiti; la voz de Jaime Garzón va de 0 a 14.06 s y el beat cae a los 14.060 s (medido: `MusicTrack.CALLE_THEME.dropMs`). Bucle infinito desde el beat; al volver con CALLE arranca directo en el beat. **Si cambias el MP3, vuelve a medir el drop.** |

## Efectos (AudioClip: cortos, se cargan enteros en memoria; orden `.wav → .mp3 → .aiff`)
| Archivo | Cuándo suena |
|---|---|
| `key_click` | Interfaz base: cada tecla y cada letra del efecto máquina de escribir (tono aleatorio ±8 %) |
| `camera_flash` | BADBUNNY/CONCHO: ¡CLICK! de Concho (cae en el primer tiempo de un compás) y cada "Safaera" exitosa |
| `moto_engine` | FEID: la moto de motocross que se acerca por el callejón |
| `moto_drift` / `moto_crush` | FEID: el derrape y la rueda que aplasta a El Guachimán |
| `grill_shine` | FEID: el destello que cruza los grills de oro |
| `moto_rev` | FERXXO RIDER: el "¡Rrrnnn!" de cada pique (ESPACIO / ↑) |
| `moto_crash` | FERXXO RIDER: frenada y choque ("¡Paila mor, te caíste!") |
| `cafexito` | FERXXO RIDER: agarrar El Cafexito (`playAt`: suena desde la X del vaso en estéreo) |
| `spray_shake` / `spray_paint` | Intro underground: la lata agitándose y el siseo pintando el tag |
| `spider_web_drop` / `spider_bite` | CALLE: el hilo de la araña y el mordisco |

Si falta un archivo, ese sonido se omite en silencio (aviso `[audio]` en la consola de errores) y nada se rompe.

## Cómo están hechos los WAV
Los `.wav` son **sintetizados por código** (sin samples de terceros) con `herramientas/SoundGen.java`: mono,
16 bit, 44.1 kHz, y los bucles empalman sin clic. Regenerarlos:
`javac -d out herramientas/SoundGen.java` y luego `java -cp out SoundGen src/main/resources/audio`.
Los sonidos retirados (temas sintéticos de Daft Punk / Bad Bunny / Feid, olas, gaviotas, arranque robótico, vocoder,
pisotón) se siguen sintetizando en memoria SIN escribirse, para que la semilla 42 avance igual y el resto salga
idéntico byte a byte.

| Archivo | Síntesis |
|---|---|
| `moto_*` | motor de dos tiempos: seno saturado + 2.º armónico de sierra + golpe de ruido por ciclo de encendido; el derrape y la frenada son ruido en bandas estrechas (2.3–3.4 kHz); el choque, campanas inarmónicas + piezas rebotando |
| `grill_shine` | campanas a 2.6 y 3.9 kHz + barrido de brillo 3 → 9 kHz con eco |
| `cafexito` | sorbo (ruido pasa-banda 700 → 2400 Hz) + arpegio de power-up en La mayor |
| `guitar_farewell` | (ya no se usa en la app: SALIR suena con `cafecito_theme.mp3`) La menor (Am–F–C–Am), Karplus-Strong + melodía aditiva |
| `boom_bap_loop` | (ya no se usa en la app: la intro suena con el tema de la CALLE) 90 BPM con swing, bombo pesado, caja + palmada, piano "de vinilo" |
| `spider_bite` | dos crujidos, golpe sub 120 → 32 Hz y cuadradas bitcrush (veneno) |
| `spider_web_drop` | glissando 1.6 kHz → 260 Hz con trémolo y rebote elástico |
| `spray_shake` / `spray_paint` | bola metálica en la lata / "psst" de válvula + siseo a 6.2 kHz en tres trazos |
| `key_click`, `camera_flash` | ver comentarios en `SoundGen.java` (`sofia_magic_sparkle` ya no se usa: el modo Sofía solo suena con su canción) |
