package ITmonteur.example.hospitalERP.exception;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class ResourceNotFoundException extends RuntimeException{
    long userId;
    String resourceName;
    String fieldName;
    String userName;

    public ResourceNotFoundException(String resourceName, String fieldName, long userId) {
        super(String.format("%s not found with %s : %s", resourceName, fieldName, userId));
        this.resourceName=resourceName;
        this.fieldName=fieldName;
        this.userId=userId;

    }
    public ResourceNotFoundException(String resourceName, String fieldName, String userName) {
        super(String.format("%s not found with %s : %s", resourceName, fieldName, userName));
        this.resourceName=resourceName;
        this.fieldName=fieldName;
        this.userName=userName;

    }
}
