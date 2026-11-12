package app.secret.ui;

/**
 * Feid "Ferxxo" en pixel art, generado por código (caricatura original, sin imágenes externas), con su identidad
 * icónica: gorra hacia atrás, gafas de escudo blancas envolventes con lente verde, bigote fino y perilla, arete,
 * cadena, chaleco táctico negro con correas verde neón sobre camiseta verde, brazos tatuados y tenis blancos de suela
 * verde. Paleta Ferxxo: verde neón #00FF66.
 * <p>
 * <b>v2 (dirección de arte):</b> sombreado de cuatro tonos de piel (luz cálida de frente, sombra, sombra profunda y
 * luz de borde verde neón por la derecha, como si un letrero lo iluminara), mandíbula esculpida en V, barba de tres
 * días tramada, gafas con degradado y reflejo diagonal, gorra con costuras y brillo de borde, y ropa con volumen
 * (pliegues del pantalón, luz en el chaleco, sombra bajo la cadena).
 * <ul>
 *   <li>{@link #body(int)}: cuerpo entero 36x60, de perfil 3/4 mirando a la derecha. Poses: {@link #STAND},
 *       {@link #RUN1}..{@link #RUN4} (ciclo de carrera con el chaleco al viento), {@link #STOMP_UP}, {@link #STOMP_DOWN}.</li>
 *   <li>{@link #face(int)}: primer plano 48x48 de frente (busto). Expresiones: {@link #NEUTRAL}, {@link #SMIRK},
 *       {@link #GRILLS} (sonrisa con grills de oro).</li>
 *   <li>{@link #headProfile(PixelGrid, int, int)}: la cabeza de perfil, compartida por el cuerpo y el piloto de la moto.</li>
 * </ul>
 */
public final class FeidSprite {

    private FeidSprite() {}

    public static final int OUT = 1, SKIN = 2, SKIN_D = 3, CAP = 4, CAP_HI = 5, FRAME = 6, LENS = 7, LENS_HI = 8,
            BEARD = 9, VEST = 10, VEST_HI = 11, NEON = 12, TEE = 13, INK = 14, PANTS = 15, PANTS_HI = 16, SHOE = 17,
            SOLE = 18, GOLD = 19, GOLD_HI = 20, GOLD_D = 21, HAIR = 22, MOUTH = 23, CHAIN = 24, SKIN_L = 25,
            // v2: sombreado de 4 tonos, luz de borde neón y detalles finos
            SKIN_DD = 26, RIM = 27, LENS_MID = 28, FRAME_SH = 29, STUBBLE = 30, STUD = 31, TEE_D = 32, PANTS_D = 33,
            CAP_RIM = 34, HAIR_HI = 35, LIP = 36, TEETH = 37;
    public static final int[] PALETTE = {0x000000,
            0x0b0b0b, 0xc98b62, 0x9c6545, 0x151515, 0x2c2c2c, 0xf4f4f4, 0x0c1f16, 0x00ff66,
            0x2a1a12, 0x121212, 0x2e2e2e, 0x00ff66, 0x00c752, 0x2f4a3a, 0x1b1b1b, 0x343434, 0xf2f2f2,
            0x00ff66, 0xf5c542, 0xfff1b0, 0x9a7414, 0x1a120e, 0x3a1410, 0xd9d9d9, 0xe0a57c,
            0x6e412c, 0x9fe8b4, 0x0f6a3a, 0xb9c2bd, 0x7d5039, 0xd6f7ff, 0x00913c, 0x0c0c0c,
            0x1d7a46, 0x3b2a20, 0x8a4b3c, 0xf7f3e8};

    public static final int BW = 36, BH = 60, FW = 48, FH = 48;
    public static final int STAND = 0, RUN1 = 1, RUN2 = 2, RUN3 = 3, RUN4 = 4, STOMP_UP = 5, STOMP_DOWN = 6;
    public static final int NEUTRAL = 0, SMIRK = 1, GRILLS = 2;

    /** Punto del pie delantero (en celdas del grid) de la pose STOMP_DOWN: ahí cae el pisotón. */
    public static final int STOMP_FOOT_X = 29, STOMP_FOOT_Y = 57;
    /** Centro de la cara en el grid del cuerpo (para el paneo de cámara) y de los grills en el grid de la cara. */
    public static final int HEAD_X = 19, HEAD_Y = 12, GRILL_X = 24, GRILL_Y = 34;

    // ================================================================== cuerpo entero

    public static PixelGrid body(int pose) {
        PixelGrid p = new PixelGrid(BW, BH);
        int bob = (pose == RUN1 || pose == RUN3) ? -1 : (pose == STOMP_DOWN ? 1 : 0);
        int y0 = bob;

        // ---- piernas: {cadera, rodilla, pie} para la pierna lejana (f) y la cercana (n)
        int[][] far, near;
        switch (pose) {
            case RUN1 -> { far = new int[][]{{15, 39}, {11, 46}, {6, 50}};  near = new int[][]{{20, 39}, {25, 46}, {29, 54}}; }
            case RUN2 -> { far = new int[][]{{15, 39}, {16, 46}, {14, 51}}; near = new int[][]{{20, 39}, {20, 47}, {19, 55}}; }
            case RUN3 -> { far = new int[][]{{15, 39}, {20, 46}, {24, 54}}; near = new int[][]{{20, 39}, {15, 46}, {9, 50}}; }
            case RUN4 -> { far = new int[][]{{15, 39}, {14, 47}, {13, 55}}; near = new int[][]{{20, 39}, {21, 46}, {19, 51}}; }
            case STOMP_UP -> { far = new int[][]{{15, 39}, {14, 47}, {13, 55}}; near = new int[][]{{20, 39}, {27, 37}, {28, 45}}; }
            case STOMP_DOWN -> { far = new int[][]{{15, 39}, {12, 47}, {10, 55}}; near = new int[][]{{20, 39}, {25, 47}, {26, 55}}; }
            default -> { far = new int[][]{{15, 39}, {14, 47}, {13, 55}}; near = new int[][]{{20, 39}, {22, 47}, {22, 55}}; }
        }
        leg(p, far, y0, true);
        leg(p, near, y0, false);

        // ---- brazo lejano (detrás del torso)
        int[][] armFar, armNear;
        switch (pose) {
            case RUN1 -> { armFar = new int[][]{{15, 23}, {23, 27}, {27, 23}}; armNear = new int[][]{{22, 23}, {17, 30}, {12, 33}}; }
            case RUN3 -> { armFar = new int[][]{{15, 23}, {11, 30}, {7, 33}};  armNear = new int[][]{{22, 23}, {27, 28}, {31, 24}}; }
            case STOMP_UP -> { armFar = new int[][]{{15, 23}, {9, 21}, {4, 17}}; armNear = new int[][]{{22, 23}, {28, 21}, {33, 17}}; }
            case STOMP_DOWN -> { armFar = new int[][]{{15, 23}, {10, 27}, {6, 24}}; armNear = new int[][]{{22, 23}, {28, 26}, {32, 22}}; }
            default -> { armFar = new int[][]{{15, 23}, {14, 30}, {14, 36}}; armNear = new int[][]{{22, 23}, {23, 30}, {23, 36}}; }
        }
        arm(p, armFar, y0, true);

        // ---- torso: camiseta verde + chaleco táctico negro con correas neón (con volumen)
        p.poly(new double[]{12, 25, 26, 11}, new double[]{20 + y0, 20 + y0, 40 + y0, 40 + y0}, TEE);
        p.rect(18, 21 + y0, 2, 18, TEE);
        p.rect(18, 21 + y0, 1, 18, TEE_D);                           // sombra de la camiseta entre las solapas
        p.poly(new double[]{12, 18, 18, 11}, new double[]{20 + y0, 20 + y0, 40 + y0, 40 + y0}, VEST);   // solapa trasera
        p.poly(new double[]{20, 25, 26, 20}, new double[]{20 + y0, 20 + y0, 40 + y0, 40 + y0}, VEST);   // solapa delantera
        p.rect(24, 21 + y0, 2, 18, VEST_HI);                         // luz de borde en el pecho (frente a la cámara)
        p.rect(12, 21 + y0, 1, 18, VEST_HI);
        p.rect(14, 22 + y0, 1, 17, NEON);                            // correas verde neón
        p.rect(22, 22 + y0, 1, 17, NEON);
        p.rect(12, 30 + y0, 4, 3, VEST_HI);                          // bolsillos con solapa neón
        p.rect(20, 30 + y0, 4, 3, VEST_HI);
        p.rect(12, 30 + y0, 4, 1, NEON);
        p.rect(20, 30 + y0, 4, 1, NEON);
        p.set(21, 32 + y0, FRAME_SH);                                // hebilla del bolsillo
        p.rect(11, 38 + y0, 16, 2, VEST);                            // cinturón
        p.rect(18, 38 + y0, 3, 2, CHAIN);
        p.set(19, 38 + y0, STUD);
        if (pose >= RUN1 && pose <= RUN4) {                          // el chaleco al viento (flamea hacia atrás)
            int flap = (pose % 2 == 1) ? 2 : 1;
            p.poly(new double[]{12, 12 - flap - 1, 12}, new double[]{27 + y0, 33 + y0, 37 + y0}, VEST);
            p.set(12 - flap - 1, 33 + y0, VEST_HI);
        }

        // ---- cuello, cadena y cabeza
        p.rect(17, 17 + y0, 5, 4, SKIN_D);
        p.rect(17, 17 + y0, 2, 4, SKIN_DD);                          // sombra de la nuca
        p.line(15, 20 + y0, 23, 20 + y0, CHAIN, 0);
        p.set(17, 20 + y0, STUD);                                    // brillo de los eslabones
        p.set(21, 20 + y0, STUD);
        p.set(19, 21 + y0, CHAIN);
        p.set(19, 22 + y0, FRAME_SH);                                // dije
        headProfile(p, 0, y0);

        // ---- brazo cercano (delante del torso)
        arm(p, armNear, y0, false);
        p.outline(OUT);
        return p;
    }

    private static void leg(PixelGrid p, int[][] j, int y0, boolean far) {
        int pants = far ? PANTS : PANTS_HI;
        p.line(j[0][0], j[0][1] + y0, j[1][0], j[1][1] + y0, pants, 1);
        p.line(j[1][0], j[1][1] + y0, j[2][0], j[2][1] + y0 - 2, pants, 1);
        // pliegues: una línea de sombra por detrás del muslo y de la espinilla (la pierna lejana va entera en sombra)
        int shade = far ? PANTS_D : PANTS;
        p.line(j[0][0] - 1, j[0][1] + y0, j[1][0] - 1, j[1][1] + y0, shade, 0);
        p.line(j[1][0] - 1, j[1][1] + y0, j[2][0] - 1, j[2][1] + y0 - 2, shade, 0);
        if (!far) p.set(j[1][0] + 1, j[1][1] + y0, VEST_HI);       // brillo en la rodilla
        int fx = j[2][0], fy = j[2][1] + y0;                          // tenis blanco de suela verde
        p.rect(fx - 2, fy - 2, 6, 3, SHOE);
        p.rect(fx - 2, fy, 6, 1, far ? FRAME_SH : SHOE);
        p.rect(fx - 2, fy - 2, 2, 1, FRAME_SH);                       // talón en sombra
        p.rect(fx - 2, fy + 1, 7, 1, SOLE);
        p.set(fx + 3, fy - 1, SHOE);
        p.set(fx + 1, fy - 2, NEON);                                  // logo del tenis
    }

    private static void arm(PixelGrid p, int[][] j, int y0, boolean far) {
        int skin = far ? SKIN_D : SKIN;
        int ink = far ? SKIN_DD : INK;
        p.line(j[0][0], j[0][1] + y0, j[1][0], j[1][1] + y0, skin, 1);
        p.line(j[1][0], j[1][1] + y0, j[2][0], j[2][1] + y0, skin, 1);
        if (!far) {                                                   // luz de borde neón en el lado de la cámara
            p.line(j[0][0] + 1, j[0][1] + y0 + 1, j[1][0] + 1, j[1][1] + y0, SKIN_L, 0);
        }
        // manga corta verde en el hombro
        p.rect(j[0][0] - 1, j[0][1] + y0 - 1, 3, 3, TEE);
        p.set(j[0][0] - 1, j[0][1] + y0 + 1, TEE_D);
        // manga tatuada: bandas diagonales regulares (se leen como tatuaje y no como suciedad)
        for (int s = 0; s < 2; s++) {
            int ax = j[s][0], ay = j[s][1] + y0, bx = j[s + 1][0], by = j[s + 1][1] + y0;
            int n = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
            for (int k = (s == 0 ? 3 : 1); k < n; k++) {
                int x = ax + (bx - ax) * k / Math.max(1, n), y = ay + (by - ay) * k / Math.max(1, n);
                if ((k + s) % 3 == 0) {
                    if (p.get(x, y) == skin || p.get(x, y) == SKIN_L) p.set(x, y, ink);
                    if (p.get(x - 1, y + 1) == skin) p.set(x - 1, y + 1, ink);
                }
            }
        }
        p.rect(j[2][0] - 1, j[2][1] + y0 - 1, 3, 3, skin);            // mano
        p.set(j[2][0] + 1, j[2][1] + y0 - 1, far ? SKIN_DD : SKIN_D);  // nudillos
        if (!far) p.set(j[2][0], j[2][1] + y0 + 1, GOLD);             // anillo
    }

    /**
     * Cabeza de perfil 3/4 mirando a la derecha, desplazada (dx, dy) respecto al marco del cuerpo (centro en 19,12).
     * La comparten el cuerpo de pie y el piloto de la moto.
     */
    public static void headProfile(PixelGrid p, int dx, int dy) {
        // cráneo con 3 tonos: nuca en sombra, mejilla con luz, y la luz neón por el borde de la cara
        p.ellipse(19 + dx, 12 + dy, 6.5, 7, SKIN);
        p.ellipse(16 + dx, 12 + dy, 3.4, 6, SKIN_D);
        p.ellipse(22 + dx, 14 + dy, 3.6, 3.4, SKIN_L);
        p.rect(25 + dx, 11 + dy, 1, 5, RIM);                         // borde iluminado por el neón
        // patilla y oreja con arete
        p.rect(12 + dx, 8 + dy, 3, 5, HAIR);
        p.set(14 + dx, 8 + dy, HAIR_HI);
        p.rect(13 + dx, 11 + dy, 2, 3, SKIN_D);
        p.set(13 + dx, 12 + dy, SKIN_DD);
        p.set(14 + dx, 14 + dy, STUD);
        // gorra hacia atrás: copa con costura y brillo verde, visera hacia la nuca con forro neón
        p.ellipse(19 + dx, 6 + dy, 7.6, 4.4, CAP);
        p.line(14 + dx, 4 + dy, 22 + dx, 3 + dy, CAP_HI, 0);         // brillo superior
        p.set(24 + dx, 4 + dy, CAP_RIM);
        p.set(25 + dx, 5 + dy, CAP_RIM);
        p.set(26 + dx, 7 + dy, CAP_RIM);
        p.line(19 + dx, 2 + dy, 20 + dx, 8 + dy, CAP_HI, 0);         // costura del panel
        p.rect(7 + dx, 8 + dy, 7, 2, CAP);                            // visera
        p.rect(7 + dx, 10 + dy, 7, 1, NEON);
        p.rect(12 + dx, 9 + dy, 13, 1, CAP);                          // banda sobre la frente
        // gafas de escudo: montura blanca + lente verde con reflejo
        p.rect(15 + dx, 10 + dy, 11, 1, FRAME);
        p.rect(14 + dx, 11 + dy, 2, 1, FRAME_SH);                     // patilla hacia la oreja
        p.rect(18 + dx, 11 + dy, 8, 2, LENS);
        p.rect(18 + dx, 12 + dy, 8, 1, LENS_MID);
        p.set(21 + dx, 11 + dy, LENS_HI);
        p.set(22 + dx, 11 + dy, LENS_HI);
        p.set(25 + dx, 12 + dy, FRAME);
        // nariz con sombra
        p.rect(26 + dx, 13 + dy, 1, 2, SKIN);
        p.set(25 + dx, 15 + dy, SKIN_DD);
        // bigote fino, boca, perilla y barba de tres días en la mandíbula
        p.rect(21 + dx, 15 + dy, 5, 1, BEARD);
        p.rect(22 + dx, 16 + dy, 3, 1, LIP);
        p.rect(20 + dx, 17 + dy, 5, 2, BEARD);
        p.set(25 + dx, 17 + dy, STUBBLE);
        for (int y = 14; y <= 18; y++)
            for (int x = 15; x <= 19; x++)
                if (((x + y) & 1) == 0 && p.get(x + dx, y + dy) == SKIN_D) p.set(x + dx, y + dy, STUBBLE);
    }

    // ================================================================== primer plano (busto de frente)

    public static PixelGrid face(int expr) {
        PixelGrid p = new PixelGrid(FW, FH);

        // ---- hombros: camiseta verde con chaleco negro y correas neón
        p.poly(new double[]{1, 15, 33, 47, 47, 1}, new double[]{48, 41, 41, 48, 48, 48}, VEST);
        p.poly(new double[]{16, 32, 28, 20}, new double[]{41, 41, 48, 48}, TEE);
        p.poly(new double[]{18, 30, 24}, new double[]{41, 41, 46}, SKIN_D);           // escote en V
        p.line(11, 47, 16, 43, NEON, 0);
        p.line(37, 47, 32, 43, NEON, 0);
        p.line(33, 42, 44, 46, VEST_HI, 0);                                           // luz en el hombro derecho

        // ---- cuello con la sombra que proyecta la mandíbula
        p.rect(18, 36, 12, 8, SKIN_D);
        p.rect(18, 36, 12, 2, SKIN_DD);
        p.rect(28, 38, 2, 5, RIM);

        // ---- cabeza: cráneo + mandíbula en V
        p.ellipse(24, 23, 14.5, 14.5, SKIN);
        p.poly(new double[]{9.5, 38.5, 37, 31, 24, 17, 11}, new double[]{24, 24, 34, 41, 43.5, 41, 34}, SKIN);
        // orejas con sombra interior y arete de brillante
        for (int s = -1; s <= 1; s += 2) {
            double ex = 24 + s * 15.2;
            p.ellipse(ex, 25, 2.2, 3.6, s < 0 ? SKIN_D : SKIN_L);
            p.ellipse(ex + s * 0.3, 25, 0.9, 2.0, s < 0 ? SKIN_DD : SKIN_D);
            p.set((int) Math.round(ex), 29, STUD);
        }

        // ---- sombreado de la piel: luz cálida de frente-derecha, sombra a la izquierda, borde neón a la derecha
        for (int y = 8; y < 44; y++)
            for (int x = 0; x < FW; x++) {
                if (p.get(x, y) != SKIN) continue;
                double nx = (x - 24) / 14.5, ny = (y - 26) / 17.0;
                if (nx > 0.88) p.set(x, y, RIM);
                else if (nx < -0.78) p.set(x, y, SKIN_DD);
                else if (nx < -0.5) p.set(x, y, SKIN_D);
                else if (nx > 0.15 && nx < 0.62 && ny > -0.2 && ny < 0.3) p.set(x, y, SKIN_L);   // pómulo con luz
                else if (ny > 0.74) p.set(x, y, SKIN_D);                                       // bajo la quijada
            }

        // ---- patillas y nacimiento del pelo bajo la gorra
        p.rect(9, 13, 3, 10, HAIR);
        p.rect(36, 13, 3, 10, HAIR);
        p.set(37, 14, HAIR_HI);
        p.set(10, 14, HAIR_HI);

        // ---- gorra hacia atrás vista de frente: copa con paneles + arco del cierre mostrando el pelo
        p.ellipse(24, 9, 16.5, 8.5, CAP);
        p.rect(8, 11, 32, 3, CAP);
        p.ellipse(24, 13, 4.5, 2.6, HAIR);                            // hueco del snapback
        p.set(22, 13, HAIR_HI); p.set(26, 12, HAIR_HI);
        p.rect(19, 14, 11, 1, NEON);                                  // tira de ajuste verde
        p.set(24, 14, FRAME);                                         // broche
        p.line(24, 1, 24, 9, CAP_HI, 0);                              // costura central
        p.line(16, 3, 13, 11, CAP_HI, 0);                             // costuras laterales
        p.line(32, 3, 35, 11, CAP_HI, 0);
        p.line(14, 3, 21, 1, CAP_HI, 0);                              // brillo de la copa
        for (int y = 3; y <= 13; y++) {                               // luz de borde neón en el lado derecho
            int x = 40;
            while (x > 30 && p.get(x, y) != CAP) x--;
            if (p.get(x, y) == CAP) p.set(x, y, CAP_RIM);
        }
        p.set(24, 0, CAP_HI);                                         // botón superior

        // ---- gafas de escudo envolventes: montura blanca, lentes con degradado y reflejo diagonal neón
        p.rect(7, 16, 34, 2, FRAME);
        p.rect(7, 17, 34, 1, FRAME_SH);
        p.rect(6, 17, 2, 3, FRAME_SH);                                // varillas envolviendo la cabeza
        p.rect(40, 17, 2, 3, FRAME);
        p.poly(new double[]{9, 23, 22.5, 11}, new double[]{17.5, 17.5, 23, 22.5}, LENS);
        p.poly(new double[]{25, 39, 37, 25.5}, new double[]{17.5, 17.5, 22.5, 23}, LENS);
        p.poly(new double[]{9, 23, 22.5, 11}, new double[]{21.5, 21.5, 23, 22.5}, LENS_MID);
        p.poly(new double[]{25, 39, 37, 25.5}, new double[]{21.5, 21.5, 22.5, 23}, LENS_MID);
        p.line(13, 21, 16, 18, LENS_HI, 0);                           // reflejo diagonal del neón
        p.line(15, 21, 16, 20, LENS_MID, 0);
        p.line(29, 21, 32, 18, LENS_HI, 0);
        p.line(31, 21, 32, 20, LENS_MID, 0);
        p.rect(23, 18, 2, 2, FRAME);                                  // puente
        p.set(23, 20, FRAME_SH);

        // ---- nariz: tabique en sombra a la izquierda, punta con luz, aletas
        p.line(23, 23, 22, 28, SKIN_D, 0);
        p.set(25, 27, SKIN_L);
        p.set(24, 28, SKIN_L);
        p.rect(21, 29, 2, 1, SKIN_DD);
        p.rect(26, 29, 2, 1, SKIN_D);
        p.rect(23, 30, 3, 1, SKIN_D);

        // ---- bigote fino con puntas caídas
        p.rect(19, 31, 10, 1, BEARD);
        p.set(18, 32, BEARD); p.set(29, 32, BEARD);
        p.set(17, 33, STUBBLE); p.set(30, 33, STUBBLE);
        p.set(23, 31, HAIR_HI);                                       // brillo

        mouth(p, expr);

        // ---- perilla en el mentón + barba de tres días tramada en la mandíbula
        p.poly(new double[]{20, 28, 27, 24, 21}, new double[]{37, 37, 41.5, 42.5, 41.5}, BEARD);
        p.set(24, 38, STUBBLE);
        // barba de tres días: solo en el borde de la mandíbula (2-3 px hacia dentro del contorno), puntos sueltos
        for (int y = 31; y < 44; y++)
            for (int x = 10; x < 39; x++) {
                int v = p.get(x, y);
                if (v != SKIN && v != SKIN_D && v != SKIN_DD) continue;
                boolean nearEdge = p.get(x - 3, y) == 0 || p.get(x + 3, y) == 0 || p.get(x, y + 2) == 0
                        || p.get(x - 2, y + 2) == 0 || p.get(x + 2, y + 2) == 0;
                if (nearEdge && ((x + 2 * y) % 3 == 0)) p.set(x, y, STUBBLE);
            }

        // ---- cadena de eslabones sobre el pecho, con su sombra
        for (int x = 13; x <= 35; x++) {
            int y = 42 + (int) Math.round(3 * Math.sin(Math.PI * (x - 13) / 22.0));
            p.set(x, y + 1, VEST);
            p.set(x, y, (x % 3 == 0) ? STUD : CHAIN);
        }
        p.rect(23, 46, 3, 2, GOLD);                                   // dije
        p.set(24, 46, GOLD_HI);

        p.outline(OUT);
        return p;
    }

    private static void mouth(PixelGrid p, int expr) {
        switch (expr) {
            case SMIRK -> {                                           // media sonrisa ladeada, comisura derecha arriba
                p.rect(20, 34, 7, 1, MOUTH);
                p.rect(27, 33, 2, 1, MOUTH);
                p.rect(21, 35, 6, 1, LIP);
                p.set(29, 32, SKIN_D);                                // hoyuelo
            }
            case GRILLS -> {                                          // sonrisa abierta con grills de oro
                p.poly(new double[]{17.5, 30.5, 28.5, 19.5}, new double[]{33, 33, 37, 37}, MOUTH);
                p.rect(18, 33, 13, 2, GOLD);
                for (int x = 18; x < 31; x += 2) { p.set(x, 33, GOLD_HI); p.set(x + 1, 34, GOLD_D); }
                p.rect(20, 35, 9, 1, GOLD_D);                         // fila inferior
                p.set(24, 33, STUD);                                  // brillante incrustado
                p.rect(17, 32, 15, 1, LIP);                           // labio superior
                p.rect(19, 37, 10, 1, LIP);                           // labio inferior
            }
            default -> {                                              // serio
                p.rect(20, 34, 9, 1, MOUTH);
                p.rect(21, 35, 7, 1, LIP);
            }
        }
    }
}
