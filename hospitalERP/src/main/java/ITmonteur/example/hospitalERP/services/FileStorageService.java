package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded profile images under a server-generated name.
 * The client's filename is never used for the path, so uploads cannot escape the
 * upload directory or overwrite each other.
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_IMAGE_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Saves the image and returns its public URL path, e.g. /uploads/profileImages/uuid.jpg */
    public String storeProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is empty");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Image must be 2 MB or smaller");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = ALLOWED_IMAGE_TYPES.get(contentType);
        if (extension == null) {
            throw new BadRequestException("Only JPEG, PNG or WEBP images are allowed");
        }
        try (InputStream in = file.getInputStream()) {
            if (!hasImageSignature(in.readNBytes(12), extension)) {
                throw new BadRequestException("File content is not a valid " + extension.toUpperCase(Locale.ROOT) + " image");
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file");
        }

        String subDir = "profileImages";
        String fileName = UUID.randomUUID() + "." + extension;
        Path target = uploadRoot.resolve(subDir).resolve(fileName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BadRequestException("Invalid upload path");
        }
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to store uploaded image", e);
            throw new IllegalStateException("Image upload failed");
        }
        return "/uploads/" + subDir + "/" + fileName;
    }

    // Checks the file's magic bytes so a renamed HTML/SVG file is rejected
    private static boolean hasImageSignature(byte[] head, String extension) {
        return switch (extension) {
            case "jpg" -> head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png" -> head.length >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            case "webp" -> head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            default -> false;
        };
    }
}
