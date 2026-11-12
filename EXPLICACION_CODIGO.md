# Ministerio Del Rebusque — Explicación del código (para estudiar antes de exponer)

> Documento interno del equipo. Explica **cómo funciona** el proyecto, **cómo leer el diagrama UML** y **qué podría
> preguntar el jurado** y cómo responder. Está escrito "con peras y manzanas": primero la idea con una analogía y
> después el detalle técnico con el archivo exacto donde vive.

---

## 0. La app en un minuto

**Ministerio Del Rebusque** (proyecto Maven `ministerio-del-rebusque`, antes "La Caleta OS") es una app de escritorio en **JavaFX** que hace dos cosas reales:

1. **Cifra** un archivo o un texto con **AES-256-GCM**. La llave sale de tu contraseña con **PBKDF2-HMAC-SHA256** (210.000 iteraciones).
2. **Esconde** el resultado cifrado dentro de una imagen PNG por **esteganografía LSB**: cambia el bit menos significativo de cada color de cada píxel. A simple vista la foto es idéntica.

Encima de ese **núcleo** hay tres **modos secretos** (easter eggs) que se activan tecleando una palabra. Cada uno toma
la pantalla completa, reproduce una cinemática y muestra una interfaz temática con una función de seguridad real:

| Palabra | Modo | Función real |
|---|---|---|
| `DAFTPUNK` | Human After All | Borrar la huella EXIF de una foto y sellar un archivo dentro de ella |
| `BADBUNNY` / `CONCHO` | Un Verano Sin Cifrar | "Safaera": cifrar un archivo y destruir el original (borrado kamikaze) |
| `FEID` / `FERXXO` | Ferxxo Rider | Mini-juego oculto (endless runner en moto). Sin utilidades: `CALLE` + Enter en su consola para volver |
| `SOFIA` | Sector S.P. | Módulo Cartas: escribir y abrir cartas cifradas escondidas en una foto |
| `BBYWOW` | Concierto | Pura sorpresa: El Guachimán "bichota" (coletas azules) canta "BbY WOW" en un estadio y los fuegos artificiales escriben las frases; a los ≈1:09 vuelve solo a la calle |
| `CALLE` | — | Volver a la calle desde cualquier modo (El Mordisco + péndulo de la araña, `SpiderBite`) |
| `SALIR` | — | Apagar el programa (El Cafecito, `CafecitoExit`: cafetería + "Bonito, Bonito" con El Guachimán y su frappé, y al final `Platform.exit()`) |
| `PICAKU` | (núcleo original) | Mutación "modo amor" y notas secretas del núcleo |

---

## 1. Mapa de paquetes (qué hay en cada carpeta)

| Paquete | Rol | Analogía |
|---|---|---|
| `app.main` | Punto de entrada (`Main`, `Launcher`) | La puerta del edificio |
| `app.ui` | Interfaz base: ventana, menú (solo CURAR + CONFIGURACIÓN), consola con línea de comandos | El primer piso, donde atiende todo el mundo |
| `app.animations` | Animaciones del núcleo (intro `UndergroundIntro`, cascada de errores, `KeyTrigger`, `PicakuPayload`) | Los efectos especiales del primer piso |
| `app.crypto` | `CryptoService` (AES-256-GCM + PBKDF2) y `Payload` | La caja fuerte |
| `app.stega` | `StegoVault`, `LsbStego`, `CoverFactory` | El que esconde la caja fuerte dentro de un cuadro |
| `app.narrative` | Textos y guiones | El libreto |
| **`app.secret`** | **Todo lo nuevo**: orquestación de los modos secretos | Un sótano secreto con su propia entrada |
| `app.secret.audio` | `AudioManager`, efectos y música | La consola de sonido |
| `app.secret.modes` | `BaseMode` + los 4 modos (Daft Punk, Bad Bunny, Feid, Sofía) | Los "guiones" de cada fiesta |
| `app.secret.dave` | Cinemáticas estilo *Dave the Diver* y el arte (`MechaSpiderArt`, `TropicalHeartArt`, `ArtSprite`) | El cine |
| `app.secret.view` | Interfaces exclusivas, mini-guías HUD, mascotas | Las salas temáticas |
| `app.secret.ui` | Kit pixel art y componentes pequeños (pads, log, fondos) | La utilería |
| `app.secret.service` | Lógica pura sin UI: `MetadataScrubber`, `SecureWiper` | Los técnicos |

**Regla de oro de la arquitectura:** `app.secret` conoce al núcleo, pero el núcleo **no** conoce a `app.secret`.
La única línea que se agregó al núcleo es `EasterEggManager.install(view);` en `Main.java`. Si se borra el paquete
`app.secret` y esa línea, la app original sigue funcionando igual.

---

## 2. Cómo arranca la app

```
Main.start(stage)
 ├─ new MainView(stage)               ← arma toda la interfaz base (1280×720 lógico, escalado a la ventana)
 ├─ EasterEggManager.install(view)    ← ÚNICO gancho hacia el código nuevo
 ├─ stage.show()
 └─ Platform.runLater(view::playIntro) ← la intro arranca cuando la ventana ya existe
```

`EasterEggManager.install` crea:
- el `AudioManager` (y precarga los efectos en un hilo aparte),
- el `SecretContext` (la "fachada" que esconde los detalles de `MainView`),
- el `TriggerHook` (el oído que escucha las palabras clave).

---

## 3. La inyección de los modos secretos (el "Hook")

### 3.1 La idea
Imagina un edificio con **portero en la puerta** (la ventana) y **recepcionista en el lobby** (la escena). El núcleo ya
tenía un recepcionista escuchando "SOFIA" y "PICAKU" (`KeyTrigger`, instalado como filtro de la **escena**). Si
ponemos otro recepcionista al lado, **los dos** oyen "SOFIA" y los dos reaccionan: ese era el **bug de las dos
interfaces superpuestas**.

La solución fue poner a nuestro oyente **en la puerta**: `TriggerHook` se instala como filtro de la **ventana**
(`Window`). En JavaFX una tecla viaja `Window → Scene → nodos`, y los filtros se ejecutan en ese orden. Así que el
nuestro la oye **primero**, y si reconoce la palabra, **consume** la última letra: el núcleo nunca la recibe.

### 3.2 El detalle fino (el "espacio sintético")
El `KeyTrigger` del núcleo ya había guardado "SOFI" en su memoria. Si solo nos comiéramos la "A", la próxima "A" que
alguien teclee completaría "SOFIA" en el núcleo. Por eso, después de reclamar la palabra, `TriggerHook` **inyecta un
espacio falso** en la escena: el búfer del núcleo queda en "SOFI " y ya nunca puede terminar en "SOFIA".

Archivo: `app/secret/TriggerHook.java` (métodos `handle` y `breakCoreSuffix`).

### 3.3 Otros ganchos
- **Sonido de tecleo:** el mismo `TriggerHook` llama a `AudioManager.keyClick()` en cada tecla mientras estás en la interfaz base.
- **Tecleo de la consola del núcleo:** `SecretContext.hookConsoleTyping()` escucha el texto de la consola y suena un clic cuando crece **exactamente 1 carácter** (efecto máquina de escribir), no cuando se imprime una línea completa.
- **Por qué es seguro:** ninguno de estos ganchos modifica clases del núcleo; solo **escuchan** propiedades o eventos públicos.

---

## 4. La máquina de estados (lo que corrige el solapamiento)

### 4.1 La idea
Piensa en un **ascensor**: no puede ir al piso 3 si todavía está abriendo la puerta en el piso 1. Cada fase tiene
"siguientes fases permitidas", y cualquier otro botón se **ignora**.

```
IDLE ──► TAKEOVER ──► CINEMATIC ──► INTERFACE ──► RESTORING ──► IDLE
             │             │                          ▲
             └─────────────┴──────── (CALLE / error) ─┘
```

| Estado | Qué está pasando |
|---|---|
| `IDLE` | Interfaz base normal. Solo aquí se aceptan disparadores. |
| `TAKEOVER` | La UI base se **deshabilita y oculta en el mismo instante** (sin fundido). |
| `CINEMATIC` | Corren las cinemáticas. Un segundo disparador se ignora. |
| `INTERFACE` | La interfaz temática está montada. |
| `RESTORING` | Se cortan animaciones, se liberan recursos y vuelve la UI base. |

Archivo: `app/secret/TakeoverStateMachine.java`. La tabla `LEGAL` define las transiciones permitidas y
`transition(to)` devuelve `false` si no es legal (y no cambia nada).

### 4.2 Por qué "deshabilitar" y no solo "ocultar"
Un nodo invisible **pero con foco** (por ejemplo, un campo de contraseña) seguiría recibiendo teclas. Un nodo
**deshabilitado** pierde el foco y no puede recuperarlo. Por eso `SecretContext.takeover()` hace `setDisable(true)` +
`setVisible(false)`.

### 4.3 El orden exacto que pidió el requisito
1. Se detecta la palabra → `TAKEOVER`: la vista principal desaparece **inmediatamente**.
2. `CINEMATIC`: se reproduce la cinemática.
3. **Solo** cuando el `setOnFinished` de la última transición dispara, se crea la nueva interfaz (`createInterface`). Antes de ese momento **no existe** ningún objeto de la interfaz temática.

---

## 5. Template Method: cómo están hechos los modos

### 5.1 La idea
Una **receta de cocina fija** ("precalentar, mezclar, hornear, servir") donde cada plato solo cambia los
**ingredientes**. La receta vive en `BaseMode` y es `final` (nadie la puede alterar). Cada modo solo declara:

```java
protected List<Supplier<Cinematic>> cinematics();     // QUÉ cinemáticas, en orden
protected TakeoverView createInterface(Runnable exit); // QUÉ interfaz
protected SoundEffect entrySound();                   // QUÉ sonidos (opcional)
protected MusicTrack cinematicMusic(), interfaceMusic();
```

Ejemplo real, `DaftPunkMode`:
```java
cinematics()      → [preludio de la araña-mecha, escenario piramidal]
createInterface() → new DaftPunkConsoleView(ctx, exit)
interfaceMusic() → DAFT_PUNK_THEME   (el preludio arranca la canción anclada a su drop)
```

### 5.2 Cómo se encadenan las cinemáticas (`BaseMode.playNext`)
Como en el cine: **la escena siguiente se monta detrás de la actual** y la actual se desvanece encima.
1. La cinemática N termina (su `setOnFinished`) con su último cuadro **congelado** cubriendo la pantalla.
2. Se crea la N+1 y se coloca **debajo** (`ctx.mountBelow`).
3. La N+1 arranca y la N hace `dissolve()` (fundido de salida que la revela).
4. Cuando ya no quedan cinemáticas, en vez de "la N+1" se monta **la interfaz**.

El espectador nunca ve un hueco negro ni dos escenas peleando. El callback va envuelto en `once(...)`, así que
aunque el usuario salte la cinemática con un clic **y** además termine sola, la interfaz se crea **una sola vez**.

### 5.3 La interfaz común `Cinematic`
Cualquier cosa que sea cinemática cumple 4 métodos: `node()`, `play(onFinished)`, `dissolve()`, `stop()`.
Así `BaseMode` puede encadenar un `Playback` (preludio Dave the Diver) o una `DaftPunkCinematicView` sin saber cuál es
cuál (**polimorfismo**).

---

## 6. Las animaciones (Transitions) explicadas

### 6.1 Las piezas de JavaFX
| Clase | Qué hace | Ejemplo en el proyecto |
|---|---|---|
| `FadeTransition` | Cambia la opacidad de A a B | El flash blanco de la cámara: 1.0 → 0.0 en 200 ms |
| `ScaleTransition` | Cambia el tamaño | La araña entra de 0.2 a 1.0 con rebote |
| `TranslateTransition` | Mueve | Concho camina 500 px |
| `RotateTransition` | Gira | La aureola de Sofía |
| `Timeline` + `KeyFrame` | Anima **cualquier** propiedad, o ejecuta código en un instante | Máquina de escribir: un KeyFrame por letra |
| `SequentialTransition` | Pone animaciones **una detrás de otra** | El guion completo de cada cinemática |
| `ParallelTransition` | Las ejecuta **a la vez** | Zoom de cámara + temblor + destello |
| `PauseTransition` | Espera | Pausas dramáticas |

### 6.2 Interpoladores: el "carácter" del movimiento (`Cine.java`)
Un interpolador decide **cómo** se recorre el camino de 0 a 1 en el tiempo:
- `SNAP` (Bézier 0.12, 0.92, 0.18, 1.0): sale disparado y se asienta → entradas de paneles.
- `SLAM` (0.85, 0, 1, 0.55): arranca lento y **revienta** al final → golpes, pisadas.
- `SOFT`: suave → cámara lenta.
- `OVERSHOOT`: se pasa un poco y vuelve → rebotes. Fórmula `u²·((c+1)·u + c) + 1` con `u = t − 1`, `c = 1.9`.

### 6.3 El truco del `cue` de 1 ms
Para ejecutar código **en un punto exacto** de una `SequentialTransition` (cambiar el fondo, lanzar partículas, sonar
un efecto) se usa `Cine.cue(runnable)`: un `Timeline` de **1 ms**. ¿Por qué 1 ms y no 0? Porque JavaFX **salta sin
ejecutar** los hijos de duración cero dentro de una secuencia.

### 6.4 La "cámara"
`CineStage` tiene tres capas: `bg` (fondo), `world` (lo que la cámara mira) y `hud` (lo que va pegado a la pantalla).
Hacer zoom = escalar `world`. Para acercarse al punto P con factor Z se escala `world` (pivote en su centro C) y se
traslada `−(P − C)·Z`, así P termina en el centro de la pantalla (`DaftPunkCinematicView`).

### 6.5 Ejemplo: cómo leer la cinemática de Sofía (`SofiaCinematic.build`)
```
hold 450 ms
→ (paralelo) se abre el haz de luz + resplandor
→ (paralelo) aparece el "dibujo de luz" de su retrato, baja 40 px y crece 0.9→1.0 con SOFT
→ cue: sonido mágico + estallido de estrellas y corazones
→ (paralelo) el color aparece en 900 ms mientras la luz se apaga en 1500 ms
→ aureola, nombre, dedicatoria → hold → setOnFinished
```
El "dibujo de luz" es **la misma imagen** con `ColorAdjust(saturación −1, brillo +0.55)` + `Glow`. Como la capa de luz
se apaga **más lento** de lo que el color aparece, nunca hay un cuadro en que se vea el fondo a través de ella.

---

## 7. Hilos: por qué la app no se congela

### 7.1 La regla
JavaFX tiene **un solo hilo** que dibuja y atiende clics: el **FX Application Thread**. Si haces algo pesado ahí
(cifrar 16 MB, 210.000 iteraciones de PBKDF2) la ventana se congela. Y **solo** ese hilo puede tocar nodos.

### 7.2 Las herramientas
| Herramienta | Para qué | Dónde |
|---|---|---|
| `Task<T>` | Trabajo pesado en un hilo aparte | `SofiaView.launch`, `VeranoTropicalView.onSafaera`, `DaftPunkConsoleView.run` |
| `setOnSucceeded` / `setOnFailed` | JavaFX los ejecuta **ya en el hilo de la UI**: pueden tocar nodos | Mostrar "Carta sellada", errores |
| `updateMessage` / `updateProgress` | Publicar avance **desde** el hilo de fondo de forma segura | Barra de progreso del Módulo Cartas (enlazada con `bind`) |
| `Platform.runLater(r)` | "Ejecuta esto en el hilo de la UI cuando puedas" | `LogBox.println`, `ConsolePanel.log`, `AudioManager.fx` |
| Hilo `daemon` | No impide cerrar la app si sigue trabajando | Todos los `new Thread(task)` |

Patrón típico (`LogBox`): `if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);` → el mismo
método sirve llamado desde la UI o desde un `Task`.

### 7.3 Detalles de seguridad en hilos
- La contraseña viaja como `char[]` y se **borra** (`Arrays.fill(pw, '\0')`) al terminar, salga bien o mal.
- Si la vista se cerró mientras el `Task` trabajaba, `isDisposed()` evita mostrar el resultado en una vista muerta.
- En la consola Daft Punk, el PNG temporal limpio se asigna en `setOnSucceeded` (hilo UI), no desde el hilo de fondo, para no cruzarse con "Sellar disco".

---

## 8. Las tres funciones reales y cómo se conectan al núcleo

Todas usan **el mismo motor del núcleo**, `StegoVault` (que a su vez usa `CryptoService` y `LsbStego`). Los modos
secretos **no reimplementan** criptografía: solo le ponen otra interfaz.

### 8.1 Daft Punk — limpieza EXIF + sellado (`DaftPunkConsoleView`)
1. **Cargar track:** la imagen portada.
2. **Borrar huella EXIF:** `MetadataScrubber.inspect` lista los metadatos (GPS, cámara, fecha) y `scrubToTempPng` **reconstruye los píxeles** en un PNG nuevo sin metadatos.
3. **Sellar disco:** `StegoVault.sealFile(archivo, clave, portadaLimpia, ...)` cifra y esconde. Después `stripAncillaryInPlace` audita el PNG final: solo deben quedar los chunks `IHDR/IDAT/IEND`.

### 8.2 Bad Bunny — Safaera, el borrado kamikaze (`VeranoTropicalView`)
1. Primer clic: el botón se **arma** (pide confirmación durante 4 s).
2. Segundo clic: `sealFile` cifra y **verifica** releyendo el PNG.
3. **Solo si eso salió bien**, `SecureWiper.wipe` sobrescribe el original con ceros, lo verifica, lo renombra y lo elimina. Si algo falla antes, la excepción salta el borrado y el original queda intacto.

### 8.3 Sofía — Módulo Cartas (`SofiaView`)
- **Escribir:** `StegoVault.sealNote(texto, clave, png, ...)` → guarda la carta cifrada en una foto de neón dentro de `Settings.notesDir()`.
- **Abrir:** `StegoVault.open(png, clave, ...)` → devuelve un `Payload`; si es de tipo `NOTE` se muestra el texto.
- Es **la misma función** que las "notas secretas" del modo amor del núcleo (`SecretNotesView`), con otra interfaz.
- Con clave incorrecta, GCM detecta que la etiqueta de autenticación no coincide y lanza error: **no** se muestra basura.

*(El antiguo "Visor RAM" se eliminó del modo Sofía por requisito; su vista y su servicio se retiraron del proyecto.)*

---

## 9. Arte, mascotas y mini-guías

### 9.1 Las mascotas de Daft Punk y Bad Bunny: arte vectorial dibujado por código
No son imágenes: cada mascota es un `Group` de JavaFX armado con figuras (`Path`, `QuadCurve`, `SVGPath`, `Polygon`...)
y **todo el cuerpo** está rediseñado. No es la araña base con un accesorio pegado encima. El mismo arte se usa en la
cinemática y en la consola, así que el personaje es coherente de principio a fin.

**`MechaSpiderArt` (Daft Punk): una araña hecha de equipo de concierto**
- **Cabeza = casco:** mitad plata y mitad oro. La costura entre ambas es un **circuito impreso a la vista**, con pistas en ángulo recto, vías y un chip, y una **tira de LED** que se enciende en ola.
- **Muslos = andamios de escenario (truss):** dos barras paralelas con refuerzo en zigzag. Las barras se obtienen desplazando la línea de la pata a lo largo de su **vector normal** `n = (−dy, dx)/|d|`, a ±ancho/2.
- **Piernas = cables de audio gruesos** que cuelgan por gravedad: una curva cuadrática con el punto de control bajo el centro (una comba del 18 % del largo). Terminan en un **plug de 6.35 mm**.
- **Rodillas** = mini altavoces. **Abdomen** = caja de subwoofer con el cono latiendo.
- **Animación (`idle()`):** rebote del cuerpo, LEDs en persecución (el LED `i` se enciende cuando `(i + k) mod 3 == 0`, con `k` subiendo cada 90 ms) y el cono del subwoofer bombeando.

**`TropicalHeartArt` (Bad Bunny): un Corazón Triste tropical-ciberpunk**
- **Torso = el corazón triste** de un solo ojo, cosido con **grapas cibernéticas** luminosas y una placa remachada con LED. Cada grapa se coloca perpendicular a la cicatriz: se calcula el punto y la tangente de la curva Bézier y se usa la normal.
- **Chasis de araña = duna de arena** con granos y palmeras de neón grabadas. Los granos se reparten de forma uniforme dentro de la elipse con radio `√u`, para que no se amontonen en el centro.
- **Patas = troncos de palma** (un trazo marrón más un trazo discontinuo claro que dibuja los anillos) con **tenis** blancos.
- **Streetwear:** camisa floral abierta recortada al contorno del corazón, bucket hat estampado y cadena con dije.
- **Fondo:** sol synthwave con franjas, hecho restando rectángulos a un círculo con `Shape.subtract`, más dos palmeras de neón.
- **Efectos en la cinemática:** `silhouette(true)` aplica `ColorAdjust(brillo −1)` más un `DropShadow` naranja (contraluz); `splitCopy(color)` son copias del contorno con `BlendMode.ADD` desplazadas (glitch rojo y cian); `grin(true)` cambia la boca triste por la sonrisa con dientes.

**`MascotView` (las consolas)** coloca ese arte en una caja con encaje *contain*: escala `s = min(ancho/anchoArte,
alto/altoArte)` y luego lo centra. Al entrar, `mutate()` hace 6 cuadros de glitch de color y un rebote elástico
(0.1 → 1.15 → 1.0).

### 9.2 El retrato de Sofía (`ArtSprite`)
El modo Sofía sí usa la imagen de referencia: `src/main/resources/art/sofia_portrait.png`, cargada una sola vez por
`ArtSprite` (caché `EnumMap`). Se recorta con un rectángulo redondeado, porque el PNG original trae esquinas negras.
En la cinemática, la misma imagen con `ColorAdjust` + `Glow` hace de "dibujo de luz" que se materializa en color.

> Nota: `daft_mecha_spider.png` y `cyborg_heart.png` (de una versión anterior) se eliminaron en la limpieza: Daft Punk
> y Bad Bunny usan arte vectorial dibujado por código. En `art/` solo queda `sofia_portrait.png`.

### 9.3 `GuideHud`: las mini-guías
Panel semitransparente estilo HUD de videojuego, una por interfaz:
- **Módulo Human After All** (Daft Punk), **Módulo Safaera** (Bad Bunny), **Módulo Cartas** (Sofía).
- Dice **qué hace la pantalla** en una frase, los **pasos numerados** y una **advertencia** (⚠).
- Se abre al entrar, se pliega solo a los 12 s (salvo que el ratón esté encima) y se abre/cierra con clic o **F1**.
- Es una capa superpuesta (`StackPane`): no mueve los controles de la vista.

---

## 10. Audio (`AudioManager`)
- **Efectos** con `AudioClip`: se cargan enteros en memoria, suenan al instante y se pueden solapar.
- **Música** con `MediaPlayer`: se reproduce en streaming y en bucle. Al cambiar de pista o con CALLE la anterior se detiene en seco (`stop()` + `dispose()`): nunca suenan dos a la vez. Las pistas sin tempo entran con fundido de potencia constante (`sen(t·π/2)`).
- **Rhythm sync**: `MusicTrack` guarda BPM, primer beat y drop medidos sobre cada canción; `AudioManager.beats()` = `(currentTime − primerBeat) / beat`. Los preludios arrancan la canción en `drop − impacto` y un seguidor ajusta la velocidad del guion cada frame; `BeatSync` recoloca (`jumpTo`) las animaciones cíclicas si se desvían más de 18 ms.
- Si falta un archivo o el sistema no tiene soporte de medios, el sonido **se omite**: el audio nunca rompe una cinemática.
- Los `.wav` incluidos son provisionales, sintetizados con `herramientas/SoundGen.java`.

---

## 11. Cómo leer el diagrama UML (`diagrama-clases-ministerio-del-rebusque.puml`)

El archivo genera **tres** imágenes. Léelas en este orden:

### Paso 1 — Diagrama de clases: mira los COLORES (las cajas grandes son paquetes)
| Color | Significa |
|---|---|
| Gris | Núcleo original. **No se tocó.** |
| Azul claro | Orquestación, modos y vistas (`app.secret`) |
| Amarillo | Cinemáticas y arte |
| Verde | Servicios y audio, sin interfaz gráfica |

Lo primero que hay que notar: **todas las flechas van del color al gris, nunca al revés.** Eso demuestra que el
código nuevo depende del núcleo y no al contrario.

### Paso 2 — Aprende a leer las flechas
| Flecha | Se lee | Ejemplo |
|---|---|---|
| Línea sólida con triángulo hueco `—▷` | "es un / hereda de" | `DaftPunkMode —▷ BaseMode` |
| Línea punteada con triángulo hueco `‥▷` | "implementa la interfaz" | `BaseMode ‥▷ SecretMode` |
| Rombo relleno `◆—` | "contiene y es dueño" (composición: si muere el dueño, muere la parte) | `SofiaView ◆— GuideHud` |
| Rombo vacío `◇—` | "usa uno compartido" (agregación) | `SecretContext ◇— AudioManager` |
| Flecha punteada `‥>` | "depende de / crea / usa" | `BaseMode ‥> Cinematic` |
| Flecha sólida `—>` | "tiene una referencia y lo llama" | `SofiaView —> StegoVault` |
| `+—` con círculo | "clase interna" | `ArtSprite +— Art` |

Los textos entre `<< >>` son **estereotipos**: una pista del rol (`<<Facade>>`, `<<Factory>>`, `<<singleton>>`).
`{final}` en un método significa que las subclases no lo pueden cambiar. `#` = protegido, `+` = público, `-` = privado.

### Paso 3 — Sigue el camino de una activación (de arriba abajo)
1. `Main ‥> EasterEggManager : install(view, audio)`: la única conexión con el núcleo (comparte el `AudioManager` de la intro).
2. `EasterEggManager ◆— TriggerHook` (oye las palabras) y `◆— SecretContext` (fachada).
3. `SecretContext ◆— TakeoverStateMachine`: el "ascensor" de estados.
4. `EasterEggManager ◇— "4" SecretMode`: los cuatro modos, que heredan de `BaseMode`.
5. `BaseMode ‥> Cinematic` y `BaseMode ‥> TakeoverView`: la receta encadena cinemáticas y luego crea la vista.
6. Cada vista **compone** su `GuideHud` y, si aplica, su `MascotView`.
7. Las vistas bajan hasta `StegoVault —> CryptoService`: ahí está la seguridad real.

### Paso 4 — Diagrama de estados (`Ministerio_FSM`)
Cada caja es un estado y cada flecha una transición con su motivo. Fíjate en que de `INTERFACE` solo se puede ir a
`RESTORING`: no hay flecha de `INTERFACE` a `TAKEOVER`. Por eso es **imposible** que se monten dos interfaces.

### Paso 5 — Diagrama de secuencia (`Ministerio_Flujo_SOFIA`)
Se lee **de arriba hacia abajo** (el tiempo corre hacia abajo). Cada columna es un objeto y cada flecha un mensaje
numerado. Muestra el caso completo: teclear SOFIA → el hook se adelanta al núcleo → estados → cinemática →
`setOnFinished` → se crea `SofiaView` → sellar una carta en un `Task` → teclear CALLE (El Mordisco) → volver a la calle.

---

## 12. Preguntas que podría hacer el profesor (y cómo responder)

**1. ¿Por qué no modificaron directamente `MainView` para quitar el disparador "SOFIA" del núcleo?**
> Por el **principio abierto/cerrado**: el núcleo está cerrado a modificaciones y abierto a extensión. Lo extendimos con
> un filtro de ventana que se ejecuta antes. Así el núcleo sigue siendo exactamente el original y se puede probar por separado.

**2. ¿Qué patrones de diseño usaron?**
> - **Template Method**: `BaseMode` fija el flujo; cada modo rellena los pasos.
> - **Facade**: `SecretContext` expone solo lo que los modos necesitan de `MainView`.
> - **State**: la FSM del takeover.
> - **Factory**: `DaveTheDiverAnimations` crea las cinemáticas.
> - **Strategy/Polimorfismo**: `Cinematic` y `SecretMode` permiten tratar distinto objeto con la misma interfaz.
> - **Singleton**: `Theme` y `Settings` (del núcleo).
> - **Observer**: listeners de propiedades JavaFX (por ejemplo, el gancho de tecleo de la consola).

**3. ¿Cómo garantizan que no se abran dos interfaces?**
> Tres capas: (a) `TriggerHook` evita que el núcleo y el modo reaccionen a la misma palabra; (b) la FSM solo acepta
> disparadores en `IDLE`; (c) `once(...)` impide que el `setOnFinished` y un "saltar" creen la interfaz dos veces.
> Además hay una prueba automática que lo verifica sobre la app real.

**4. ¿Qué pasa si el usuario escribe CALLE en mitad de la cinemática?**
> La FSM permite `CINEMATIC → RESTORING`. `deactivate()` corta todas las cinemáticas con `stop()`, libera animaciones y
> partículas, apaga la música con fundido y restaura la UI base. Si luego llega un `setOnFinished` tardío, `playNext`
> ve que el estado ya no es `CINEMATIC` y lo ignora.

**5. ¿Por qué AES-256 en modo GCM y no CBC?**
> GCM es **cifrado autenticado**: además de ocultar el contenido, detecta cualquier modificación o una clave
> incorrecta (la etiqueta no coincide). CBC necesitaría un MAC aparte. Por eso, con clave errónea, el Módulo Cartas da
> error en vez de mostrar texto basura.

**6. ¿Para qué sirve PBKDF2 con 210.000 iteraciones?**
> Convierte una contraseña humana en una llave de 256 bits y, a propósito, lo hace **lento**: cada intento de fuerza
> bruta cuesta 210.000 operaciones HMAC-SHA256. Lleva sal aleatoria para que dos contraseñas iguales no den la misma llave.

**7. ¿La esteganografía LSB es segura?**
> Oculta que hay un mensaje, pero **la seguridad la da el cifrado**, no el escondite. Un análisis estadístico podría
> sospechar que la imagen tiene datos, pero sin la clave no se puede leer. Limitación: solo sobrevive en formatos sin
> pérdida. Si alguien convierte el PNG a JPG se destruye; por eso la guía avisa "envíala como archivo".

**8. ¿El borrado "Safaera" es realmente irrecuperable?**
> En discos mecánicos, sobrescribir con ceros es efectivo. En **SSD y memorias flash** no se puede garantizar al 100 %
> por el *wear leveling* (el controlador puede escribir en otra celda). Lo decimos honestamente en la mini-guía. Además,
> el orden es seguro: primero se sella y verifica, y **solo después** se borra.

**9. ¿Por qué usan `Task` y no un `Thread` normal que actualice la UI?**
> Porque solo el hilo de JavaFX puede tocar nodos. `Task` da `updateProgress`/`updateMessage` (seguros desde el fondo)
> y `setOnSucceeded`/`setOnFailed` (que se ejecutan en el hilo de la UI). Con un `Thread` a secas habría que envolver
> cada actualización en `Platform.runLater`.

**10. ¿Qué es `Platform.runLater` y cuándo lo usan?**
> Encola código para que el hilo de JavaFX lo ejecute en su próximo ciclo. Lo usamos cuando algo que **no** está en ese
> hilo necesita tocar la UI: los logs que escribe el motor de cifrado durante un `Task`, o el `AudioManager` si lo
> llaman desde otro hilo.

**11. ¿Por qué la contraseña es `char[]` y no `String`?**
> Un `String` es inmutable y queda en memoria hasta que el recolector de basura quiera. Un `char[]` se puede **borrar**
> (`Arrays.fill`) apenas se usa, y así reducimos el tiempo en que la clave está en RAM.

**12. ¿Por qué las mascotas son vectoriales y no imágenes?**
> Porque así **fusionamos** los conceptos en un solo cuerpo en vez de pegar un accesorio sobre la araña base: las patas
> de la araña-mecha *son* andamios y cables, y el torso del corazón *es* la prenda y la cicatriz. Además, el vector
> escala sin perder nitidez (la misma figura sirve en la consola a 0.7× y en la cinemática a 2.8×) y cada pieza se
> puede animar por separado: LEDs, cono del subwoofer, sonrisa o silueta a contraluz.

**13. ¿Por qué el `cue` dura 1 ms?**
> `SequentialTransition` salta los hijos de duración cero sin ejecutar sus `KeyFrame`; con 1 ms la acción sí corre y
> el retraso es imperceptible.

**14. ¿Cómo probaron que funciona?**
> Compilación con `-Xlint:all` sin advertencias y una prueba de punta a punta sobre la app real que teclea las palabras
> con eventos sintéticos. Verifica, entre otras cosas: UI base oculta al instante, una sola vista tras la cinemática,
> mini-guía presente, ida y vuelta real de una carta (sellar y luego abrir con la misma clave devuelve el texto exacto),
> que una clave incorrecta da error, y que `CALLE` restaura todo en los 4 modos.

**15. Si mañana quieren un cuarto modo, ¿qué tienen que tocar?**
> Crear una clase que extienda `BaseMode` (declara palabra, cinemáticas, interfaz y sonidos) y agregarla a la lista de
> `EasterEggManager`. No se toca la FSM, ni el hook, ni el núcleo. Eso es bajo acoplamiento y alta cohesión.

**16. ¿Qué limitaciones reconocen?**
> - Máximo 16 MB por archivo escondido.
> - LSB no sobrevive a la recompresión.
> - El borrado seguro en SSD no es garantizable.
> - El cifrado protege el contenido, no oculta que la imagen existe.
> - Los audios incluidos son provisionales.

---

## 13. Guion corto para la demo
1. Mostrar la app base cifrando y descifrando un archivo (núcleo).
2. Teclear `SOFIA`: la UI desaparece al instante → cinemática → Módulo Cartas. Escribir una carta, sellarla y abrirla con la misma clave. Mostrar la guía con F1.
3. `CALLE`, luego `DAFTPUNK`: cinemática → consola → cargar una foto con GPS → "Borrar huella EXIF" y leer en el log qué metadatos se eliminaron.
4. `CALLE`, luego `CONCHO`: cinemática → explicar el doble clic de Safaera (sin ejecutarlo sobre un archivo importante).
5. Abrir el diagrama UML y recorrer los Pasos 1–5 de la sección 11.

## 14. Glosario rápido
- **FSM**: máquina de estados finitos.
- **Takeover**: la interfaz secreta "toma" toda la pantalla.
- **Hook (gancho)**: punto donde nuestro código se engancha a eventos de otro sin modificarlo.
- **Filtro de eventos**: oyente que recibe el evento en la fase de **captura**, antes que los nodos hijos.
- **LSB**: *Least Significant Bit*, el bit que menos cambia el color.
- **GCM**: *Galois/Counter Mode*, modo de AES con autenticación.
- **EXIF**: metadatos de una foto (GPS, cámara, fecha).
- **Vector normal**: dirección perpendicular a una línea; sirve para desplazar las barras del andamio o colocar las grapas.
- **Smoothstep**: curva `t²(3−2t)` que suaviza transiciones sin cortes bruscos.
