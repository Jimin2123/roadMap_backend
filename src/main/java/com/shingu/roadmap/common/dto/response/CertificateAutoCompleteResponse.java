package com.shingu.roadmap.common.dto.response;

import com.shingu.roadmap.common.domain.Certificate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자격증 자동완성 응답 DTO")
public record CertificateAutoCompleteResponse(
        @Schema(description = "자격증 코드", example = "123456")
        String code,    // jmcd()

        @Schema(description = "자격증 이름", example = "정보처리기사")
        String name,    // jmfldnm

        @Schema(description = "자격증 종류", example = "국가자격증")
        String agency   // qualgbnm
) {
  public static CertificateAutoCompleteResponse from(Certificate c) {
    return new CertificateAutoCompleteResponse(
            c.getJmcd(),
            c.getJmfldnm(),
            c.getQualgbnm()
    );
  }
}