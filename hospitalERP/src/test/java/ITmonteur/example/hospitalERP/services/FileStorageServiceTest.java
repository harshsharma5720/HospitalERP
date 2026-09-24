package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @TempDir
    Path uploadDir;

    @Test
    void storesUnderGeneratedNameIgnoringClientFilename() {
        FileStorageService service = new FileStorageService(uploadDir.toString());
        MockMultipartFile file = new MockMultipartFile("profileImage",
                "../../evil.png", "image/png", PNG);

        String url = service.storeProfileImage(file);

        assertThat(url).startsWith("/uploads/profileImages/").endsWith(".png").doesNotContain("evil");
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(uploadDir.resolve("profileImages").resolve(fileName))).isTrue();
    }

    @Test
    void rejectsNonImageContentType() {
        FileStorageService service = new FileStorageService(uploadDir.toString());
        MockMultipartFile html = new MockMultipartFile("profileImage", "x.html", "text/html", "<script>".getBytes());

        assertThatThrownBy(() -> service.storeProfileImage(html)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsFileWhoseBytesAreNotAnImage() {
        FileStorageService service = new FileStorageService(uploadDir.toString());
        MockMultipartFile fake = new MockMultipartFile("profileImage", "x.png", "image/png", "<svg></svg>".getBytes());

        assertThatThrownBy(() -> service.storeProfileImage(fake)).isInstanceOf(BadRequestException.class);
    }
}
