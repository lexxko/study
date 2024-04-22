package exception;

public class NonCriticalException extends ShapeApplicationException {

    public NonCriticalException() {
    }

    public NonCriticalException(String message) {
        super(message);
    }

    public NonCriticalException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonCriticalException(Throwable cause) {
        super(cause);
    }

    public NonCriticalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
