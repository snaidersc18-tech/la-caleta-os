# Ministerio Del Rebusque — Easter Eggs · v7 "Zero-UI"

> Para estudiar el código antes de exponer, lee **`EXPLICACION_CODIGO.md`**. Diagramas: `diagrama-clases-ministerio-del-rebusque.puml`.

## Qué cambia en la v7
- **Nombre:** "La Caleta OS" pasa a ser **Ministerio Del Rebusque**.
- **Zero-UI:** ya no hay botones de salir ni de "volver a la calle". Se navega con comandos: `CALLE` (Mordisco de la Araña con péndulo de vuelta a su marco, `SpiderBite`) y `SALIR` (el Cafecito, `CafecitoExit`: cafetería + "Bonito, Bonito" sincronizado con la letra, y solo al final `Platform.exit()`).
- **Arranque en frame 0:** telón negro en un `StackPane` de `Main`; intro hacker + graffiti + homenaje a Jaime Garzón sincronizado con Alcolirykoz (`UndergroundIntro`, un solo `AnimationTimer`, ~60 FPS) y el "sello" de la araña en su marco al revelar la interfaz.
- **DAFTPUNK:** concierto en vivo (cabeceo al pulso y estroboscopio cian/amarillo/magenta).
- **Limpieza:** fuera la vista de cifrado del menú (`InjectView`), la intro anterior (`IntroSequence`), atajos de la v4 y los PNG `daft_mecha_spider.png` / `cyborg_heart.png` que ya no usaba nada.

## Qué cambia en la v6
- **Modo Sofía:** se eliminó el Visor RAM. La interfaz ahora es el **Módulo Cartas**: escribir una carta y sellarla cifrada (AES-256-GCM + LSB) dentro de una foto de neón, o abrir una carta sellada con su clave. Usa el mismo motor que las "notas secretas" del núcleo (`StegoVault.sealNote` / `StegoVault.open`).
- **Mascotas** (arte vectorial dibujado por código, el mismo en cinemática y consola):
  - DAFTPUNK → `MechaSpiderArt`: casco plata/oro con circuito impreso y tira de LED en la costura, muslos de andamio de escenario, piernas de cable de audio con plug, rodillas-altavoz y subwoofer como abdomen.
  - BADBUNNY/CONCHO → `TropicalHeartArt`: Corazón Triste con grapas cibernéticas, chasis de duna de arena con palmeras de neón, patas de tronco de palma con tenis, camisa floral, bucket hat y cadena; sol synthwave detrás.
  - SOFIA → retrato de referencia `src/main/resources/art/sofia_portrait.png`, cargado por `ArtSprite`.
- **Mini-guías HUD** (`GuideHud`) en las 3 interfaces: panel semitransparente que explica qué hace la pantalla y cómo usarla. Se abre al entrar, se pliega solo a los 12 s y se abre/cierra con clic o **F1**.
- **Limpieza:** se retiraron las clases sin uso (visor RAM, servicio RAM, órbita de corazones, retrato pixel generado por código, cargador antiguo, casco pixel sin uso) y artefactos viejos (`reemplazos/`, capturas y PNG del diagrama desactualizados).

## Ejecutar
- Desarrollo: `mvn clean javafx:run`
- Ejecutable (Windows): `compilar-ejecutable.bat` → `dist\MinisterioDelRebusque\MinisterioDelRebusque.exe`

## Disparadores (teclea con ningún campo de texto enfocado)
| Teclea | Cinemáticas | Interfaz | Función real |
|---|---|---|---|
| DAFTPUNK | Araña-mecha: pisada, visor con ecualizador, DROP → escenario piramidal + diálogo RPG | Consola Ciber-Musical + guía | Borrar huella EXIF + sellar (AES-256-GCM + LSB) |
| BADBUNNY / CONCHO | Corazón Triste tropical a contraluz, franjas, revelación → playa, Concho, ¡CLICK! | UI tropical + guía | "Safaera": sellar, verificar y destruir el original |
| FEID / FERXXO | Callejón de Medellín en neón verde: El Guachimán en la calle, suena una moto, Feid entra derrapando en su moto de motocross y lo aplasta (humo verde, temblor), se baja, sonríe y el flash de sus grills de oro ciega la pantalla | FERXXO RIDER: mini-juego endless runner (Medellín de noche en parallax, KM recorridos, mini-consola abajo) | Ninguna: es pura sorpresa. ESPACIO/↑ = pique, R = volver a rodar, `CALLE` + Enter = volver |
| SOFIA | Retrato de Sofía: dibujo de luz que se materializa, aureola y corazones | Módulo Cartas + guía | Escribir / abrir cartas cifradas en una foto PNG |
| BBYWOW | Concierto sobre "BbY WOW" (`bbywow_theme.mp3`): apagón, El Guachimán "bichota" (coletas azules con ganchos de estrella) baja por su telaraña, se detiene en el quiebre (28.9 s), flashazo en el drop (31.86 s) y estadio con láseres, pantallas con el espectro, público saltando, mini arañas bailarinas y fuegos artificiales que explotan revelando cada frase en el cielo. Frases: `src/main/resources/lyrics/bbywow.lrc` (`[mm:ss.cc] texto`) | — (sin interfaz) | a los ≈1:09 (69.6 s) apagón y vuelta a la calle con el péndulo de CALLE |
| PICAKU | (núcleo original) cascada + mutación | Notas secretas del núcleo | — |
| CALLE | El Mordisco: la araña baja, muerde, infección glitch, péndulo y aterrizaje en su marco | vuelve al menú principal | — |
| SALIR | El Cafecito: la UI se funde en una cafetería verde y cálida; con "Bonito, Bonito" El Guachimán baja con un frappé, sonríe a cámara y lo abraza (≈33 s, sincronizado con la letra) | `cafecito_theme.mp3` | apaga el programa (fundido a negro y `Platform.exit()`) |

Durante las cinemáticas: clic / ESC = saltar (ESPACIO avanza el diálogo de DAFTPUNK). En las interfaces: F1 = guía.

## Integración con el núcleo
- `Main.java`: `EasterEggManager.install(view, audio);` (el mismo `AudioManager` de la intro)
- El núcleo criptográfico (`app.crypto`, `app.stega`) no se tocó.
