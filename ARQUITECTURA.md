# Ministerio Del Rebusque — notas técnicas

Resumen corto de cómo está armado hoy. El detalle para estudiar está en `EXPLICACION_CODIGO.md` y los diagramas en
`diagrama-clases-ministerio-del-rebusque.puml`.

## Capas
| Capa | Clases clave | Responsabilidad |
|---|---|---|
| Arranque | `Main`, `Launcher` | `StackPane` con dos capas: la UI y un **telón negro** encima desde el frame 0. Pone el icono a la ventana. |
| Núcleo | `MainView`, `CureView`, `StegoVault`, `CryptoService` | Curar archivos: AES-256-GCM (PBKDF2 ×210.000) + esteganografía LSB en PNG. |
| Comandos | `EasterEggManager`, `TriggerHook`, `TakeoverStateMachine`, `SecretContext` | Zero-UI: las palabras tecleadas son la navegación. Un estado a la vez (las transiciones ilegales se rechazan). |
| Cinemáticas | `UndergroundIntro`, `SpiderBite`, `CafecitoExit`, `FerxxoDriftCinematic`, `BbyWowCinematicView`, `BaseMode` + modos | Intro, CALLE (mordisco + péndulo), SALIR (el cafecito) y los cinco modos secretos. |
| Audio | `AudioManager`, `SoundEffect`, `MusicTrack`, `BeatSync` | SFX precargados (`AudioClip`); temas en streaming en bucle infinito (Daft Punk, Bad Bunny y Feid: SOLO sus MP3 oficiales). Cambio de pista y CALLE = `stop()` + `dispose()` inmediatos. Rhythm sync: `MusicTrack` guarda BPM/primer beat/drop medidos; `beats()` lee el `currentTime` real; las cinemáticas ponen su impacto en el drop y `BeatSync` mantiene en fase las animaciones cíclicas. Voces de Daft Punk: `VocalCues` (marcas medidas de HARDER/BETTER/FASTER/STRONGER) + `AudioManager.onMusicTime` (listener en `currentTimeProperty`) → `VocalHud`. |
| Sincronía | `SongCues`, `MusicReactor`, `SpectrumWave`, `LyricFireworks` | Cues en ms de canción (`currentTimeProperty`), interfaz que reacciona al espectro en vivo (`AudioSpectrumListener`) y fuegos que escriben frases. |
| Rendimiento | `RenderTuning`, `FpsProbe` | Caché de GPU para efectos y medición real de FPS por escena. |

## Comandos
`DAFTPUNK` · `BADBUNNY`/`CONCHO` · `FEID`/`FERXXO` · `SOFIA` · `BBYWOW` abren su modo; `PICAKU` el modo amor del núcleo; `CALLE` vuelve al menú
desde cualquiera de ellos; `SALIR` cierra la app tras la despedida. `ESC` suelta un campo de texto para poder teclear
comandos. Durante la intro, el mordisco y la despedida los comandos se ignoran.

## SALIR: el cafecito (≈33 s)
La UI (congelada, en caché de GPU) se funde en una cafetería de tonos verdes cálidos con un emblema propio (taza
humeante; no reproduce ningún logo real) mientras suena `cafecito_theme.mp3` ("Bonito, Bonito"). Todo se dispara con
`SongCues` sobre el `currentTime` real del MediaPlayer: lámparas con la voz, el emblema late con los graves, baja un
hilo y, en las tres frases (≈20.65 / 25.5 / 30.0 s), El Guachimán entra con un frappé en las patas, sonríe mirando a
cámara y abraza el vaso. La araña se compone en cada frame sobre una rejilla de 84×80 (cuerpo del sprite sin el par
delantero + patas-brazos interpoladas + vaso + cara). A los 32.7 s fundido a negro de 0.3 s y `Platform.exit()`.

## BBYWOW: el concierto (≈70 s)
`BbyWowMode` → `BbyWowCinematicView` (sin interfaz: al final pide la calle). Un `AnimationTimer` director lee el
`currentTime` de `bbywow_theme.mp3` (120 BPM, medido): 1 s de negro, descenso de El Guachimán "bichota"
(`WigSpider`: coletas azules con ganchos de estrella, ojos "^ ^", sonrisa, gargantilla) hasta el quiebre (28.9 s), flash
blanco en el drop (31.86 s) y estadio: láseres `BlendMode.ADD` que parpadean en cada golpe (`FadeTransition`), pantallas
gigantes con el espectro en vivo (`SpectrumWave`), público saltando a tempo, seis mini arañas y `LyricFireworks`: por
cada frase sale un cohete del público `FLIGHT_MS` antes y explota en su milisegundo revelando el texto neón
(`ScaleTransition` + `FadeTransition`). Las frases salen de `lyrics/bbywow.lrc` si existe. A los 69.6 s el bajo se
apaga: apagón, la música se corta y `ctx.requestStreet()` lanza El Mordisco.

Optimización medida (`-Drebusque.fps=true`): de 45 a 98 FPS de media (mínimo 72). Los 10 láseres eran `Polygon`
que cambiaban de puntos en cada frame (re-rasterizados en CPU) → rectángulos que solo GIRAN (quads en GPU); el público
eran ~600 trazos vectoriales por frame → ~120 `drawImage` de 12 texturas pre-dibujadas; las chispas usan alfa global
(cero `Color` nuevos por frame) y ningún texto lleva efecto (el resplandor es una copia con trazo grueso detrás).

## Rendimiento (medido, Intel UHD, pantalla ~72 Hz)
Medir: `java -Drebusque.fps=true -jar target/ministerio-del-rebusque-1.0.0-fat.jar` → `[fps] t=… escena N fps peor frame X ms`.

| Escena | Antes | Ahora | Qué se hizo |
|---|---|---|---|
| Intro | 3–4 | 65–73 | Un solo `AnimationTimer`; el tag se rasteriza una vez; atlas de glifos; la UI no se dibuja bajo el telón |
| Daft Punk (preludio) | 14–32 | 44–73 | `RenderTuning.cacheEffects`: cada nodo con Glow/DropShadow va a caché de GPU; `SpeedLines` cacheadas; un Glow para todo el ecualizador |
| Daft Punk (concierto) | 47–70 | 64–72 | Igual que arriba |
| CALLE | 48–51 | 57–73 | Efectos en caché |
| SALIR (cafecito) | (nuevo) | 96–106 | Cafetería y emblema pintados una vez; por frame solo la araña (rejilla en tramos), el vapor y las partículas; UI congelada en caché y oculta cuando ya no se ve |
| FEID (cinemática) | (nueva) | (incluida en FERXXO RIDER) | Un `AnimationTimer` director; callejón pintado en segundo plano; la moto cambia entre 4 imágenes precalculadas; el cono de luz (capa ADD) sale de escena con el zoom |
| BBYWOW | (nuevo) | media 98, mín. 72 | Ver la sección BBYWOW |
| FERXXO RIDER | (nuevo) | media 108, mín. 98 | 3 capas parallax = imágenes que se trasladan (sin redibujar); el neón son 2 capas más que solo cambian de opacidad; el Metro es un `ImageView`; obstáculos y Cafexito = `ImageView` de imágenes compartidas; solo el polvo usa `Canvas` |

## Daft Punk: las palabras EN la voz
"HARDER / BETTER / FASTER / STRONGER" salen en pantalla cuando el vocoder las canta, no con un `Timeline`:
- `VocalCues` guarda 55 marcas por vuelta de la canción, en **beats** de la rejilla medida (123.48 BPM, primer golpe
  194 ms). Se midieron con la letra sincronizada (orden de las frases, ±0.3 s) y el residuo espectral del MP3 frente al
  instrumental: en la estrofa las palabras caen en los beats 123/125/127/129 (59.96 s…62.88 s); en los estribillos, en
  los beats impares `p+1, p+3, p+5, p+7` de cada frase de 16 beats.
- `AudioManager.onMusicTime(track, sink)` cuelga un listener de `mediaPlayer.currentTimeProperty()`. JavaFX actualiza
  esa propiedad cada 100 ms con UN `Platform.runLater` (`MediaTimerTask`, javafx-media 21): el listener corre dentro de
  ese runLater y solo compara números → nada extra encolado en el hilo FX. Para precisión de frame, un
  `AnimationTimer` afina entre ticks con `getCurrentTime()` (lectura en vivo del reproductor nativo, sin runLater).
- `VocalCues.Tracker` dispara cada palabra la primera vez que la posición la cruza (tolera jitter, bucle y saltos) y
  `VocalHud` dibuja la palabra, el estrobo y los triángulos del escenario como función de `ms − marca`: si el audio
  se acelera o se frena, la animación también.

## FEID / FERXXO RIDER
El modo Feid ya no trae utilidades: es una sorpresa. `FeidMode` → `FerxxoDriftCinematic` (callejón de Medellín en neón
verde: la moto entra, derrapa con humo verde y marcas de llanta, aplasta a El Guachimán con temblor de cámara, Feid se
baja de un salto, la cámara le busca la cara y el flash de los grills de oro deja la pantalla en blanco) →
`FerxxoRiderView`, un endless runner: ESPACIO/↑ = pique (salto variable con buffer de 120 ms), vallas/conos/huecos
generados por distancia, colisiones con `getBoundsInParent().intersects(...)` entre cajas invisibles, kilómetros
recorridos arriba a la derecha, `feid_theme` a todo volumen en bucle y una mini-consola transparente abajo:
`CALLE` + Enter congela el juego, corta la música y lanza El Mordisco (`SecretContext.requestStreet()`).
Dificultad progresiva: velocidad = `520 × 1.07^(km/0.5)` (tope ×3.2) para asfalto, fondo y obstáculos; la gravedad y
el impulso escalan con `(v/v0)^0.35` (misma altura de pique, menos tiempo en el aire). **El Cafexito** (vaso de tinto
pixel art) flota sobre algunos obstáculos a la altura de la cima del salto: `playAt(CAFEXITO)` con paneo estéreo según
su X, 3 s de invencibilidad (Feid parpadea en verde neón y revienta lo que toque) y km ×2.
El arte es pixel art generado por código (`FerxxoRiderArt`: moto 60×64 con 4 cuadros de rueda, piloto separable,
obstáculos, Cafexito, letreros de neón, viaducto + tren del Metro y los fondos 320×180; `FeidSprite` v2 para el cuerpo
de pie, la cabeza de perfil compartida con el piloto y el busto con grills: piel en 4 tonos, luz de borde neón,
mandíbula en V, barba tramada y gafas de escudo con reflejo).

Lecciones: (1) JavaFX **no** descarta nodos tapados: si algo opaco cubre la UI y la pantalla cambia, la UI se repinta
igual → ocultarla. (2) Un efecto (`Glow`, `DropShadow`) por nodo pequeño = una pasada de render por nodo y frame →
caché o un solo efecto en el grupo. (3) Textos grandes con trazo grueso se rasterizan en CPU → pasarlos a imagen.
(4) Los picos aislados de 60–170 ms al aparecer un elemento nuevo son la primera rasterización a caché.

## Icono
`javafx.scene.image.Image` no lee `.ico`: la ventana usa `images/icono-16…256.png` (los PNG que van dentro de
`icono.ico`). El `.ico` solo lo usa `jpackage` para el icono del `.exe` y vive en `src/main/jpackage/`.

## Compilar
`compilar-ejecutable.bat` → `dist\MinisterioDelRebusque\MinisterioDelRebusque.exe` (usa Maven/jpackage del PATH o los
de Apache NetBeans; empaqueta solo el fat-jar y borra `target\` al terminar).
