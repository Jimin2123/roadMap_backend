package com.shingu.roadmap.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 업로드 응답 DTO")
public record ImageUploadResponse(
        @Schema(description = "업로드된 이미지 URL", example = "http://localhost:8080/images/abc123.jpg")
        String imageUrl,

        @Schema(description = "원본 파일명", example = "profile.jpg")
        String originalFilename,

        @Schema(description = "저장된 파일명", example = "abc123.jpg")
        String savedFilename,

        @Schema(description = "파일 크기 (bytes)", example = "102400")
        Long fileSize
) {
    public static ImageUploadResponse of(String imageUrl, String originalFilename, String savedFilename, Long fileSize) {
        return new ImageUploadResponse(imageUrl, originalFilename, savedFilename, fileSize);
    }
}
