package app.secret;

/** Contrato de un modo secreto: se activa por palabras clave y deja todo limpio al desactivarse. */
public interface SecretMode {
    String id();
    String[] triggers();
    /** false = el manager ignora el disparador (p. ej. otro efecto en curso). */
    boolean canActivate(SecretContext ctx);
    void activate(SecretContext ctx);
    void deactivate();
    boolean isActive();
}
