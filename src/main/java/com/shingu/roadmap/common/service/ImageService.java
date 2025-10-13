package com.shingu.roadmap.common.service;

import com.shingu.roadmap.common.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    @Value("${file.upload.path:uploads/images}")
    private String uploadPath;

    @Value("${file.upload.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * 이미지 파일을 업로드하고 URL을 반환합니다.
     *
     * @param file 업로드할 이미지 파일
     * @return 업로드된 이미지 정보
     * @throws IllegalArgumentException 파일이 유효하지 않은 경우
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    public ImageUploadResponse uploadImage(MultipartFile file) throws IOException {
        // 1. 파일 유효성 검증
        validateFile(file);

        // 2. 저장 디렉토리 생성
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 3. 유니크한 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String savedFilename = UUID.randomUUID().toString() + "." + extension;

        // 4. 파일 저장
        Path filePath = uploadDir.resolve(savedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 5. URL 생성 및 응답
        String imageUrl = baseUrl + "/images/" + savedFilename;
        log.info("Image uploaded successfully: {}", imageUrl);

        return ImageUploadResponse.of(
                imageUrl,
                originalFilename,
                savedFilename,
                file.getSize()
        );
    }

    /**
     * 이미지 파일을 삭제합니다.
     *
     * @param filename 삭제할 파일명
     * @throws IOException 파일 삭제 중 오류 발생 시
     */
    public void deleteImage(String filename) throws IOException {
        Path filePath = Paths.get(uploadPath).resolve(filename);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Image deleted successfully: {}", filename);
        } else {
            log.warn("Image file not found: {}", filename);
        }
    }

    /**
     * 파일 유효성을 검증합니다.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
        }

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "허용되지 않는 파일 형식입니다. 허용된 형식: " + String.join(", ", ALLOWED_EXTENSIONS)
            );
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
    }

    /**
     * 파일 확장자를 추출합니다.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
