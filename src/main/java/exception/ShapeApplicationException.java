package exception;

public class ShapeApplicationException extends RuntimeException {
    public ShapeApplicationException() {
        super();
    }

    public ShapeApplicationException(String message) {
        super(message);
    }

    public ShapeApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShapeApplicationException(Throwable cause) {
        super(cause);
    }

    protected ShapeApplicationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
