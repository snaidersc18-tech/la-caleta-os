package app.animations;

import javafx.collections.ListChangeListener;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Afinado de render para GPUs integradas.
 * <p>
 * Medido con {@code -Drebusque.fps=true}: el preludio de DAFTPUNK caía a 14-32 FPS. Cada {@code Glow}/{@code DropShadow}
 * aplicado a un nodo pequeño (LEDs, barras del ecualizador, contornos de la araña-mecha, paneles de cómic) es una pasada
 * de render fuera de pantalla EN CADA FRAME en que esa zona se repinta, y una escena tenía decenas.
 * <p>
 * {@link #cacheEffects(Node)} recorre el subárbol y pone en caché de GPU ({@code setCache(true)} +
 * {@link CacheHint#SPEED}) a todo nodo con efecto: el efecto se rasteriza UNA vez y después la tarjeta solo mueve,
 * escala o gira ese bitmap. Si el nodo cambia de verdad (otro color, otro texto) la caché se rehace sola. Además vigila
 * los nodos que la cinemática añade más tarde y los efectos que se asignan después.
 */
public final class RenderTuning {

    private RenderTuning() {}

    public static void cacheEffects(Node root) {
        if (root == null) return;
        tune(root);
        root.effectProperty().addListener((o, a, b) -> tune(root));
        if (root instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) cacheEffects(child);
            p.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
                while (c.next()) for (Node added : c.getAddedSubList()) cacheEffects(added);
            });
        }
    }

    private static void tune(Node n) {
        if (n.getEffect() != null && !n.isCache()) {
            n.setCache(true);
            n.setCacheHint(CacheHint.SPEED);
        }
    }
}
