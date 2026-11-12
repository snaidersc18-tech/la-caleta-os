package app.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;

/** Preferencias en memoria de la sesión. */
public final class Settings {
    private static final Settings INSTANCE = new Settings();

    public final BooleanProperty crt = new SimpleBooleanProperty(true);
    public final BooleanProperty glitch = new SimpleBooleanProperty(true);
    /** null = misma carpeta que el archivo de origen. */
    public final ObjectProperty<Path> outputDir = new SimpleObjectProperty<>(null);

    private Settings() {}

    public static Settings get() { return INSTANCE; }

    public Path outDirFor(Path sibling) {
        Path d = outputDir.get();
        if (d != null) return d;
        Path parent = sibling == null ? null : sibling.toAbsolutePath().getParent();
        return parent != null ? parent : Path.of(System.getProperty("user.home"));
    }

    /** Carpeta por defecto para las notas secretas. */
    public Path notesDir() {
        Path d = outputDir.get();
        return d != null ? d : Path.of(System.getProperty("user.home"), "AranaHack");
    }
}
