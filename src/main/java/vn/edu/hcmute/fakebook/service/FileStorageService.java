package vn.edu.hcmute.fakebook.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final long MAX_IMAGE_SIZE =
        5L * 1024L * 1024L;

    private static final long MAX_VIDEO_SIZE =
        100L * 1024L * 1024L;

    private static final Map<String, String> IMAGE_TYPES =
        Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif"
        );

    private static final Map<String, String> VIDEO_TYPES = Map.of(
        "video/mp4", ".mp4",
        "video/webm", ".webm",
        "video/ogg", ".ogv",
        "video/quicktime", ".mov"
    );

    private final Path uploadRoot;
    private final Path postUploadDirectory;
    private final Path videoUploadDirectory;

    public FileStorageService(
            @Value("${app.upload-dir:uploads}")
            String uploadDirectory) {

        try {
            this.uploadRoot = Paths
                .get(uploadDirectory)
                .toAbsolutePath()
                .normalize();

            this.postUploadDirectory =
                uploadRoot.resolve("posts");

            this.videoUploadDirectory = uploadRoot.resolve("videos");

            Files.createDirectories(
                postUploadDirectory
            );
            Files.createDirectories(videoUploadDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Không thể tạo thư mục upload",
                exception
            );
        }
    }

    public String storePostImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                "Ảnh không được vượt quá 5 MB"
            );
        }

        String contentType = file.getContentType();
        String extension = IMAGE_TYPES.get(contentType);

        if (extension == null) {
            throw new IllegalArgumentException(
                "Chỉ chấp nhận ảnh JPG, PNG hoặc GIF"
            );
        }

        validateRealImage(file);

        String fileName =
            UUID.randomUUID() + extension;

        Path destination = postUploadDirectory
            .resolve(fileName)
            .normalize();

        if (!destination.startsWith(postUploadDirectory)) {
            throw new IllegalArgumentException(
                "Tên file không hợp lệ"
            );
        }

        try (InputStream inputStream =
                file.getInputStream()) {

            Files.copy(
                inputStream,
                destination,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Không thể lưu ảnh bài viết",
                exception
            );
        }

        return "/uploads/posts/" + fileName;
    }

    public String storeVideoPoster(MultipartFile file) {
        return storeImage(file, videoUploadDirectory, "/uploads/videos/");
    }

    public String storePostVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("Video không được vượt quá 100 MB");
        }

        String extension = VIDEO_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Chỉ chấp nhận video MP4, WebM, OGV hoặc MOV");
        }

        String fileName = UUID.randomUUID() + extension;
        Path destination = safeDestination(videoUploadDirectory, fileName);
        copy(file, destination, "Không thể lưu video bài viết");
        return "/uploads/videos/" + fileName;
    }

    private String storeImage(
            MultipartFile file,
            Path directory,
            String publicPrefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 5 MB");
        }
        String extension = IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG hoặc GIF");
        }
        validateRealImage(file);
        String fileName = UUID.randomUUID() + extension;
        Path destination = safeDestination(directory, fileName);
        copy(file, destination, "Không thể lưu ảnh");
        return publicPrefix + fileName;
    }

    private Path safeDestination(Path directory, String fileName) {
        Path destination = directory.resolve(fileName).normalize();
        if (!destination.startsWith(directory)) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        return destination;
    }

    private void copy(MultipartFile file, Path destination, String errorMessage) {
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private void validateRealImage(MultipartFile file) {
        try (InputStream inputStream =
                file.getInputStream()) {

            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new IllegalArgumentException(
                    "File đã chọn không phải ảnh hợp lệ"
                );
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Không thể đọc file ảnh",
                exception
            );
        }
    }
}
