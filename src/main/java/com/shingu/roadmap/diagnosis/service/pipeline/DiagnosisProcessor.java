package com.shingu.roadmap.diagnosis.service.pipeline;

/**
 * 진단 파이프라인 프로세서 인터페이스
 * 각 진단 단계를 처리하는 프로세서는 이 인터페이스를 구현합니다.
 */
public interface DiagnosisProcessor {

    /**
     * 진단 컨텍스트를 처리합니다.
     *
     * @param context 진단 컨텍스트
     * @return 처리된 진단 컨텍스트
     */
    DiagnosisContext process(DiagnosisContext context);

    /**
     * 프로세서의 이름을 반환합니다.
     *
     * @return 프로세서 이름
     */
    String getName();
}
