package exception;

public class CriticalException extends ShapeApplicationException {

    public CriticalException() {
    }

    public CriticalException(String message) {
        super(message);
    }

    public CriticalException(String message, Throwable cause) {
        super(message, cause);
    }

    public CriticalException(Throwable cause) {
        super(cause);
    }

    public CriticalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
