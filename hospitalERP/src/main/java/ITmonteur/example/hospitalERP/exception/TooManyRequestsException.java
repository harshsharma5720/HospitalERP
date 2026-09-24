package ITmonteur.example.hospitalERP.exception;

// Thrown when a client exceeds a rate limit, e.g. repeated failed logins (HTTP 429).
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
