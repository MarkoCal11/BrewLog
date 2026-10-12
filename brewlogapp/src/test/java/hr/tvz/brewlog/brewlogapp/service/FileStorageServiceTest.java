package hr.tvz.brewlog.brewlogapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path uploadDir;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(uploadDir.toString());
    }

    private static byte[] imageBytes(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private MultipartFile pngFile() throws Exception {
        return new MockMultipartFile("file", "photo.png", "image/png", imageBytes("png", 50, 50));
    }

    @Test
    void storeRejectsEmptyFile() {
        MultipartFile empty = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.store(empty, "coffee-beans"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void storeRejectsNonImageBytes() {
        // Rejection is driven by the decoded bytes, not the declared Content-Type:
        // PDF payload (no ImageIO reader) is refused even though a type header is present.
        MultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.store(pdf, "coffee-beans"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported or unreadable image");
    }

    @Test
    void storeRejectsCorruptImageBytes() {
        // valid content-type header, but the payload is not a decodable image
        MultipartFile bogus = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.store(bogus, "coffee-beans"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported or unreadable image");
    }

    @Test
    void storeWritesJpegAndReturnsPublicUrl() throws Exception {
        String url = service.store(pngFile(), "coffee-beans");

        assertThat(url).startsWith("/uploads/coffee-beans/").endsWith(".jpg");
        String relative = url.substring("/uploads/".length());
        assertThat(Files.exists(uploadDir.resolve(relative))).isTrue();
    }

    @Test
    void storeDownscalesImagesLargerThanMaxDimension() throws Exception {
        MultipartFile large = new MockMultipartFile("file", "big.jpg", "image/jpeg", imageBytes("jpg", 3000, 2000));

        String url = service.store(large, "coffee-beans");

        BufferedImage stored = ImageIO.read(uploadDir.resolve(url.substring("/uploads/".length())).toFile());
        assertThat(Math.max(stored.getWidth(), stored.getHeight())).isEqualTo(1600);
        assertThat(stored.getWidth()).isEqualTo(1600);
        assertThat(stored.getHeight()).isEqualTo(1067); // aspect ratio preserved (3:2)
    }

    @Test
    void storeKeepsDimensionsForSmallImages() throws Exception {
        MultipartFile small = new MockMultipartFile("file", "small.jpg", "image/jpeg", imageBytes("jpg", 800, 600));

        String url = service.store(small, "coffee-beans");

        BufferedImage stored = ImageIO.read(uploadDir.resolve(url.substring("/uploads/".length())).toFile());
        assertThat(stored.getWidth()).isEqualTo(800);
        assertThat(stored.getHeight()).isEqualTo(600);
    }

    @Test
    void deleteRemovesPreviouslyStoredFile() throws Exception {
        String url = service.store(pngFile(), "coffee-beans");
        String relative = url.substring("/uploads/".length());
        assertThat(Files.exists(uploadDir.resolve(relative))).isTrue();

        service.delete(url);

        assertThat(Files.exists(uploadDir.resolve(relative))).isFalse();
    }

    @Test
    void deleteIgnoresUrlsOutsidePublicBase() {
        // not under /uploads/ -> silently ignored, no exception
        service.delete("https://cdn.example.com/photo.png");
        service.delete(null);
    }

    @Test
    void deleteIgnoresPathTraversalAttempts() throws Exception {
        Path secret = uploadDir.getParent().resolve("secret.txt");
        Files.writeString(secret, "top secret");

        service.delete("/uploads/../secret.txt");

        assertThat(Files.exists(secret)).isTrue();   // traversal target left untouched
    }
}
