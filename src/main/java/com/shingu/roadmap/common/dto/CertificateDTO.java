package com.shingu.roadmap.common.dto;

import com.shingu.roadmap.resume.domain.ResumeCertificate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자격증 (응답, 요청) DTO")
public record CertificateDTO(
        @Schema(description = "자격증 이름", example = "정보처리기사")
        String name,

        @Schema(description = "발급 기관", example = "한국산업인력공단")
        String agency,

        @Schema(description = "취득 연도", example = "2022")
        String year
) {
        public static CertificateDTO from(ResumeCertificate rc) {
                return new CertificateDTO(
                        rc.getCertificate().getJmfldnm(),
                        rc.getCertificate().getQualgbnm(),
                        rc.getAcquiredYear()
                );
        }
}
