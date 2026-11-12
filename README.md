# Ministerio Del Rebusque

App de escritorio JavaFX: cifra con **AES-256-GCM** (llave PBKDF2-HMAC-SHA256 ×210.000) y esconde
el resultado por **esteganografía LSB** dentro de una imagen PNG. Cualquier archivo, uso real. Alrededor del núcleo
hay una capa de easter eggs (cinemáticas pixel art sincronizadas con la música) que se abren tecleando comandos.

## Ejecutar
    mvn clean javafx:run                                    # desarrollo (JDK 21+)
    mvn clean package && java -jar target/ministerio-del-rebusque-1.0.0-fat.jar
    compilar-ejecutable.bat                                 # Windows -> dist\MinisterioDelRebusque\MinisterioDelRebusque.exe
    java -cp target/classes app.stega.SelfCheck             # prueba de humo del motor AES + LSB (sin UI)
    java -cp target/classes app.secret.service.SecretServicesCheck   # prueba de humo de borrado seguro / EXIF

Diagnóstico: `-Drebusque.fps=true` imprime cada segundo los FPS reales y el peor frame de la escena en curso;
`-Drebusque.debug=true` imprime cada cue de música (`[cue]`), los cambios de pista (`[musica]`) y la despedida.
Detalles técnicos: `ARQUITECTURA.md`. Para estudiar el código: `EXPLICACION_CODIGO.md`.

## Uso (Zero-UI: el menú son comandos)
- **Arranque:** intro hacker + graffiti "MINISTERIO DEL REBUSQUE"; al terminar el último trazo todo se congela y suena
  Alcolirykoz con la voz de Jaime Garzón; en el beat (14.06 s) un flash revela la interfaz.
- **Menú principal (antivirus de barrio):** `[1] CURAR ARCHIVO` (imagen camuflada + contraseña → restaura el original)
  y `[2] CONFIGURACIÓN`. Cifrar solo existe dentro de los modos secretos.
- **Comandos** (teclear con ningún campo de texto enfocado; `ESC` suelta un campo):

| Comando | Qué pasa |
|---|---|
| `DAFTPUNK` | Araña-mecha en concierto → Consola Ciber-Musical: borrar huella EXIF y sellar archivos (AES + LSB) |
| `BADBUNNY` / `CONCHO` | Corazón Triste tropical → playa de Concho → "Safaera": sellar, verificar y destruir el original |
| `FEID` / `FERXXO` | Feid aplasta a El Guachimán con su moto → **FERXXO RIDER**, mini-juego endless runner (ESPACIO/↑ pique, El Cafexito da invencibilidad; `CALLE` + Enter para salir) |
| `SOFIA` | Retrato de Sofía coreografiado sobre "Algo Mágico" (la interfaz late con el espectro) → Módulo Cartas cifradas |
| `BBYWOW` | **Concierto**: El Guachimán "bichota" (coletas azules) baja por su telaraña, flashazo en el drop, estadio con láseres, público, mini arañas y **fuegos artificiales que escriben las frases** en el cielo; a ≈1:09 apagón y vuelta a la calle |
| `PICAKU` | El núcleo muta a modo amor (suena "Algo Mágico"): notas secretas cifradas |
| `CALLE` | El Mordisco: la araña muerde, péndulo y vuelta al menú; vuelve Alcolirykoz directo en el beat |
| `SALIR` | El Cafecito: El Guachimán con un frappé al ritmo de "Bonito, Bonito"; a los 33 s se apaga la app |

## Audio
Las canciones van en `src/main/resources/audio/` (detalles, mapas de ritmo medidos y momentos de cada cinemática en
`audio/LEEME-AUDIO.md`): `alcolirykoz_injusticias.mp3` (CALLE), `daft_punk_theme.mp3`, `bad_bunny_theme.mp3`,
`feid_theme.mp3`, `sofia_theme.mp3`, `cafecito_theme.mp3` (SALIR) y `bbywow_theme.mp3`. Si falta una, esa música se
omite en silencio y las cinemáticas corren con el reloj de pared.

**Letra de BBYWOW (opcional):** los fuegos artificiales del concierto escriben las frases que haya en
`src/main/resources/lyrics/bbywow.lrc`, una por línea con su tiempo: `[00:29.40] texto`. Cada cohete sale un segundo
antes y explota justo en ese milisegundo. Sin el archivo se usan dos marcas de la escena y frases del show.

## Opcional
Pon una fuente pixel (OFL, p. ej. VT323) en `src/main/resources/fonts/pixel.ttf`; se carga sola.

## Límites honestos
LSB sobrevive solo a formatos sin pérdida (la salida es PNG; no la recomprimas ni la subas a redes que la conviertan a JPG). Máx. 16 MB de carga. El cifrado protege el contenido, no oculta que la imagen existe.
