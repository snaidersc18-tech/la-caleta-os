package app.secret.view;

import app.secret.dave.SofiaArt;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Botón temático del Módulo Cartas (modo Sofía): un sobre con sello de lacre en forma de corazón.
 * Es un {@link Button} real (foco, ENTER/ESPACIO, accesibilidad); solo cambia su gráfico. Al pasar el ratón la carta
 * se ilumina y el sello late; al pulsarla, el sello "salta" antes de ejecutar la acción.
 */
public final class LoveLetterButton extends Button {

    private final SVGPath seal = new SVGPath();
    private Runnable action = () -> { };

    /** @param scale tamaño del sobre (1 = 220×140 px); el texto va a la derecha del sobre. */
    public LoveLetterButton(String text, double scale) {
        super(text);
        Rectangle paper = new Rectangle(0, 0, 220, 140);
        paper.setArcWidth(10); paper.setArcHeight(10);
        paper.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#fff4f8")), new Stop(1, Color.web("#ffd9e8"))));
        paper.setStroke(Color.web("#c2185b")); paper.setStrokeWidth(3);
        Polygon flap = new Polygon(0, 0, 220, 0, 110, 82);
        flap.setFill(Color.web("#ffe3ee")); flap.setStroke(Color.web("#c2185b")); flap.setStrokeWidth(3);
        Polyline fold = new Polyline(0, 140, 88, 70, 132, 70, 220, 140);
        fold.setStroke(Color.web("#e6a1bd")); fold.setStrokeWidth(2);
        // corazón: dos lóbulos cúbicos que se unen en la punta inferior (centrado en (110, 82))
        seal.setContent("M 110 106 C 84 90 86 64 100 64 C 106 64 110 69 110 74 C 110 69 114 64 120 64 C 134 64 136 90 110 106 Z");
        seal.setFill(Color.web("#d81b60")); seal.setStroke(Color.web("#7a0f3a")); seal.setStrokeWidth(2);
        seal.setEffect(new Glow(0.4));
        Group envelope = new Group(paper, fold, flap, seal,
                SofiaArt.star4(20, 22, 9, Color.web("#ffd166")), SofiaArt.star4(200, 120, 7, Color.web("#ff9ac8")));

        // El sobre escalado va envuelto en otro Group: los bounds del Group exterior incluyen la escala, así el
        // Button reserva el tamaño REAL del gráfico y no el de 220×140.
        envelope.setScaleX(scale); envelope.setScaleY(scale);
        setGraphic(new Group(envelope));
        setContentDisplay(ContentDisplay.LEFT);
        setGraphicTextGap(12);
        setStyle("-fx-background-color: transparent; -fx-text-fill: #ffe1f0; -fx-font-family: Georgia; -fx-font-size: 18;"
                + " -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10;");
        setEffect(new DropShadow(24, Color.web("#ff5d8f", 0.55)));

        hoverProperty().addListener((o, a, on) -> {
            setEffect(new DropShadow(on ? 42 : 24, Color.web("#ff5d8f", on ? 0.95 : 0.55)));
            scale(seal, on ? 1.18 : 1.0, 160);
        });
        focusedProperty().addListener((o, a, on) -> setEffect(new DropShadow(on ? 36 : 24, Color.web(on ? "#ffd166" : "#ff5d8f", 0.8))));
        setOnAction(e -> pop());
    }

    /** La acción se ejecuta al terminar el salto del sello (180 ms), no antes: el clic se "siente". */
    public void setOnPress(Runnable r) { action = r; }

    private void pop() {
        ScaleTransition up = new ScaleTransition(Duration.millis(110), seal);
        up.setToX(1.5); up.setToY(1.5); up.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition down = new ScaleTransition(Duration.millis(70), seal);
        down.setToX(1.0); down.setToY(1.0);
        SequentialTransition s = new SequentialTransition(up, down);
        s.setOnFinished(e -> action.run());
        s.play();
    }

    private static void scale(javafx.scene.Node n, double to, double ms) {
        ScaleTransition t = new ScaleTransition(Duration.millis(ms), n);
        t.setToX(to); t.setToY(to);
        t.play();
    }
}
