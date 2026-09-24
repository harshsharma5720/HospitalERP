package ITmonteur.example.hospitalERP.exception;

// Thrown when the request is invalid (HTTP 400).
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
