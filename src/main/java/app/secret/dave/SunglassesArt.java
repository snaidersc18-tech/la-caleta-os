package app.secret.dave;

import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Gafas de sol que se rompen en esquirlas. */
public final class SunglassesArt extends Group {
    private final List<Polygon> shards = new ArrayList<>();
    private final Group frame = new Group();
    private final Random r = new Random();

    public SunglassesArt() {
        for (int lens = 0; lens < 2; lens++) {
            double ox = lens * 190;
            double[][] pts = {{ox, 0}, {ox + 80, -6}, {ox + 160, 0}, {ox + 150, 70}, {ox + 80, 92}, {ox + 12, 66}};
            double cx = ox + 80, cy = 40;
            for (int i = 0; i < pts.length; i++) {
                double[] a = pts[i], b = pts[(i + 1) % pts.length];
                Polygon p = new Polygon(cx, cy, a[0], a[1], b[0], b[1]);
                p.setFill(i % 2 == 0 ? Color.web("#ff9f1c") : Color.web("#d9480f"));
                p.setStroke(Color.BLACK); p.setStrokeWidth(3);
                shards.add(p); getChildren().add(p);
            }
        }
        Rectangle bridge = new Rectangle(158, 6, 36, 10); bridge.setFill(Color.BLACK);
        Rectangle armL = new Rectangle(-70, 4, 72, 9), armR = new Rectangle(348, 4, 72, 9);
        armL.setFill(Color.BLACK); armR.setFill(Color.BLACK);
        frame.getChildren().addAll(bridge, armL, armR);
        frame.setEffect(new Glow(0.2));
        getChildren().add(frame);
    }

    public Animation shatter() {
        ParallelTransition all = new ParallelTransition();
        for (Polygon p : shards) {
            double dx = (r.nextDouble() - 0.5) * 420, dy = 80 + r.nextDouble() * 320, rot = (r.nextDouble() - 0.5) * 540;
            TranslateTransition t = Cine.move(p, 700, 0, 0, dx, dy, Cine.SLAM);
            RotateTransition ro = new RotateTransition(Duration.millis(700), p); ro.setByAngle(rot);
            all.getChildren().addAll(t, ro, Cine.fade(p, 700, 1, 0));
        }
        all.getChildren().addAll(Cine.move(frame, 600, 0, 0, 0, 260, Cine.SLAM), Cine.fade(frame, 600, 1, 0));
        return all;
    }
}
