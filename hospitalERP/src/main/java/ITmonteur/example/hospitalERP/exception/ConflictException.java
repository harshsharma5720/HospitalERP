package ITmonteur.example.hospitalERP.exception;

// Thrown when the request conflicts with the current state, e.g. a slot that is already booked (HTTP 409).
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
