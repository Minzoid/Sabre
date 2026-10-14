package moon.studio.sabre.core;

public class SabreException extends RuntimeException {

    public SabreException(String message) {
        super(message);
    }

    public SabreException(String message, Throwable cause) {
        super(message, cause);
    }

    public SabreException(Throwable cause) {
        super(cause);
    }
}
