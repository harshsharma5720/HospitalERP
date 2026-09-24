package ITmonteur.example.hospitalERP.exception;

// Thrown when the caller is authenticated but not allowed to touch the resource (HTTP 403).
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
