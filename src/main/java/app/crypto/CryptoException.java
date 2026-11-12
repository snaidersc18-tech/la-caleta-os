package app.crypto;

/** Error de cifrado/descifrado con mensaje apto para mostrar en la consola de la app. */
public class CryptoException extends Exception {
    public CryptoException(String message) { super(message); }
    public CryptoException(String message, Throwable cause) { super(message, cause); }
}
