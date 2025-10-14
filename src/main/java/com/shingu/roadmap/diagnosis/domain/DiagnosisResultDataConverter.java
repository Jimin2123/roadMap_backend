package com.shingu.roadmap.diagnosis.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DiagnosisResultData를 JSON 문자열로 변환하는 JPA Converter
 *
 * 설계 특징:
 * - JPA 엔티티의 복잡한 객체를 JSON 컬럼으로 자동 변환
 * - ObjectMapper를 사용한 타입 안전 직렬화/역직렬화
 * - 변환 실패 시 런타임 예외 발생 (데이터 무결성 보장)
 *
 * 사용:
 * - DiagnosisResult 엔티티의 resultData 필드에 자동 적용
 * - @Convert(converter = DiagnosisResultDataConverter.class) 어노테이션 사용
 */
@Converter
@Component
@RequiredArgsConstructor
@Slf4j
public class DiagnosisResultDataConverter implements AttributeConverter<DiagnosisResultData, String> {

    private final ObjectMapper objectMapper;

    /**
     * DiagnosisResultData 객체를 JSON 문자열로 변환 (DB 저장 시)
     *
     * @param attribute DiagnosisResultData 객체
     * @return JSON 문자열
     */
    @Override
    public String convertToDatabaseColumn(DiagnosisResultData attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(attribute);
            log.debug("Converted DiagnosisResultData to JSON: {} characters", json.length());
            return json;
        } catch (JsonProcessingException e) {
            log.error("Failed to convert DiagnosisResultData to JSON", e);
            throw new IllegalStateException("진단 결과 데이터를 JSON으로 변환하는 데 실패했습니다.", e);
        }
    }

    /**
     * JSON 문자열을 DiagnosisResultData 객체로 변환 (DB 조회 시)
     *
     * @param dbData JSON 문자열
     * @return DiagnosisResultData 객체
     */
    @Override
    public DiagnosisResultData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            DiagnosisResultData data = objectMapper.readValue(dbData, DiagnosisResultData.class);
            log.debug("Converted JSON to DiagnosisResultData: {} NCS analyses",
                    data.getNcsAnalyses() != null ? data.getNcsAnalyses().size() : 0);
            return data;
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to DiagnosisResultData", e);
            throw new IllegalStateException("JSON 데이터를 진단 결과 객체로 변환하는 데 실패했습니다.", e);
        }
    }
}