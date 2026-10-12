package hr.tvz.brewlog.brewlogapp.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    public static final String PUBLIC_BASE_PATH = "/uploads";

    // Validated against the actual decoded bytes (ImageIO format name), NOT the
    // client-declared Content-Type — cloud-file pickers (Google Drive, etc.) often
    // upload a perfectly valid image as "application/octet-stream", which a MIME-type
    // allowlist would wrongly reject.
    private static final Set<String> ALLOWED_FORMATS =
            Set.of("jpeg", "jpg", "png", "webp");

    /** Longest-edge cap; larger images are downscaled to fit within this box. */
    private static final int MAX_DIMENSION = 1600;
    private static final double JPEG_QUALITY = 0.85;
    /** Everything is re-encoded to JPEG, so stored files always carry this extension. */
    private static final String OUTPUT_EXTENSION = ".jpg";

    private final Path rootDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }
        byte[] original = readBytes(file);
        String format = detectFormat(original);
        if (format == null) {
            throw new IllegalArgumentException("Unsupported or unreadable image. Please use JPG, PNG, or WebP.");
        }

        byte[] processed = process(original);
        String filename = UUID.randomUUID() + OUTPUT_EXTENSION;
        try {
            Path targetDir = rootDir.resolve(subDir).normalize();
            Files.createDirectories(targetDir);
            Files.copy(new ByteArrayInputStream(processed), targetDir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file.", e);
        }
        return PUBLIC_BASE_PATH + "/" + subDir + "/" + filename;
    }

    /**
     * Decodes the upload, applies EXIF orientation (handled by Thumbnailator for JPEG sources),
     * downscales so the longest edge is at most {@link #MAX_DIMENSION}px, and re-encodes to JPEG.
     * Re-encoding also strips EXIF/location metadata from the stored file.
     */
    private byte[] process(byte[] original) {
        int[] dimensions = readDimensions(original);
        boolean needsResize = Math.max(dimensions[0], dimensions[1]) > MAX_DIMENSION;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Thumbnails.Builder<? extends InputStream> builder =
                    Thumbnails.of(new ByteArrayInputStream(original));
            if (needsResize) {
                builder.size(MAX_DIMENSION, MAX_DIMENSION); // keeps aspect ratio, fits within the box
            } else {
                builder.scale(1.0); // keep original size, still re-encode + orient
            }
            builder.outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toOutputStream(out);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to process image.", e);
        }
        return out.toByteArray();
    }

    /**
     * Returns the lowercased ImageIO format name (e.g. "jpeg", "png", "webp") if the bytes are a
     * supported, decodable image, or {@code null} otherwise. Trusting the decoded bytes — rather
     * than the client's declared Content-Type — is what lets a mislabeled cloud-picker upload
     * (often "application/octet-stream") through while still rejecting genuinely unsupported formats.
     */
    private String detectFormat(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            String name = readers.next().getFormatName().toLowerCase();
            return ALLOWED_FORMATS.contains(name) ? name : null;
        } catch (IOException e) {
            return null;
        }
    }

    /** Reads image dimensions without fully decoding the pixels; rejects anything ImageIO can't read. */
    private int[] readDimensions(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                throw new IllegalArgumentException("Invalid image file.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Invalid image file.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read image.", e);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file.", e);
        }
    }

    public void delete(String publicUrl) {
        String prefix = PUBLIC_BASE_PATH + "/";
        if (publicUrl == null || !publicUrl.startsWith(prefix)) {
            return;
        }
        Path target = rootDir.resolve(publicUrl.substring(prefix.length())).normalize();
        if (!target.startsWith(rootDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete file.", e);
        }
    }
}
