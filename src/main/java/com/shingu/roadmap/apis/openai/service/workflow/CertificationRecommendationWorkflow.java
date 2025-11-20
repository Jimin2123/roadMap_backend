package com.shingu.roadmap.apis.openai.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.dto.response.NcsCompUnitResponse;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.apis.openai.util.ResumeTextFormatter;
import com.shingu.roadmap.diagnosis.dto.response.CertificationRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 자격증 추천 워크플로우 (AI 기반)
 *
 * AI 기반 자격증 추천 전략:
 * 1. OpenAI를 사용하여 사용자 프로필, NCS 직무, KSA gap 분석 결과를 종합적으로 평가
 * 2. 실제 한국 자격증 데이터베이스와 AI 추천을 결합하여 정확도 향상
 * 3. NCS 능력단위와 자격증 요구사항을 매칭하여 gap 해소 기여도 산출
 * 4. 사용자 보유 자격증 필터링 및 우선순위 기반 정렬
 *
 * Fallback 전략:
 * - AI 호출 실패 시 정적 데이터베이스 기반 추천으로 자동 전환
 * - 항상 최소 1개 이상의 추천 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificationRecommendationWorkflow {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final NcsApiService ncsApiService;
    private final ResumeTextFormatter resumeTextFormatter;

    // IT 직무별 주요 자격증 매핑 (실제 데이터 기반)
    private static final Map<String, List<CertificationTemplate>> CERTIFICATION_DATABASE = new HashMap<>();

    static {
        // 소프트웨어 개발 관련 자격증
        CERTIFICATION_DATABASE.put("소프트웨어", List.of(
                new CertificationTemplate("정보처리기사", "한국산업인력공단", "정보기술", 3, 1, 40, 4),
                new CertificationTemplate("정보처리산업기사", "한국산업인력공단", "정보기술", 2, 2, 30, 3),
                new CertificationTemplate("SQL 개발자(SQLD)", "한국데이터산업진흥원", "데이터베이스", 2, 3, 25, 2),
                new CertificationTemplate("AWS Certified Developer", "Amazon", "클라우드", 3, 2, 30, 3)
        ));

        // 응용SW 엔지니어링
        CERTIFICATION_DATABASE.put("응용", List.of(
                new CertificationTemplate("정보처리기사", "한국산업인력공단", "정보기술", 3, 1, 45, 4),
                new CertificationTemplate("정보보안기사", "한국산업인력공단", "보안", 4, 2, 35, 5),
                new CertificationTemplate("CKAD (Kubernetes)", "CNCF", "클라우드", 4, 3, 30, 4)
        ));

        // 데이터베이스 관련
        CERTIFICATION_DATABASE.put("데이터", List.of(
                new CertificationTemplate("SQL 전문가(SQLP)", "한국데이터산업진흥원", "데이터베이스", 4, 1, 40, 5),
                new CertificationTemplate("SQL 개발자(SQLD)", "한국데이터산업진흥원", "데이터베이스", 2, 2, 35, 2),
                new CertificationTemplate("빅데이터분석기사", "한국산업인력공단", "빅데이터", 3, 3, 30, 4)
        ));

        // 네트워크/시스템
        CERTIFICATION_DATABASE.put("네트워크", List.of(
                new CertificationTemplate("정보통신기사", "한국산업인력공단", "네트워크", 3, 1, 40, 4),
                new CertificationTemplate("네트워크관리사 2급", "한국정보통신자격협회", "네트워크", 2, 2, 30, 2),
                new CertificationTemplate("리눅스마스터 1급", "한국정보통신진흥협회", "시스템", 3, 3, 30, 3)
        ));

        // 보안
        CERTIFICATION_DATABASE.put("보안", List.of(
                new CertificationTemplate("정보보안기사", "한국산업인력공단", "보안", 4, 1, 45, 5),
                new CertificationTemplate("정보보안산업기사", "한국산업인력공단", "보안", 3, 2, 35, 4),
                new CertificationTemplate("CISSP", "ISC2", "보안", 5, 3, 25, 12)
        ));

        // 기본/공통 자격증 (폴백용)
        CERTIFICATION_DATABASE.put("기본", List.of(
                new CertificationTemplate("정보처리기사", "한국산업인력공단", "정보기술", 3, 1, 40, 4),
                new CertificationTemplate("컴퓨터활용능력 1급", "대한상공회의소", "OA", 2, 3, 20, 2),
                new CertificationTemplate("정보기술자격(ITQ)", "한국생산성본부", "OA", 1, 4, 15, 1)
        ));
    }

    private record CertificationTemplate(
            String name,
            String organization,
            String category,
            int difficulty,
            int basePriority,
            int baseGapContribution,
            int estimatedMonths
    ) {}

    /**
     * 사용자 프로필, NCS 코드, KSA 역량 분석 결과를 기반으로 자격증을 추천합니다.
     *
     * 전략:
     * 1. NCS 직무명에서 키워드 추출 (소프트웨어, 데이터, 네트워크 등)
     * 2. 해당 키워드에 매핑된 자격증 템플릿 조회
     * 3. 사용자가 이미 보유한 자격증 필터링
     * 4. KSA gap 분석 결과 기반 우선순위 조정
     * 5. 최대 5개 추천
     *
     * @param profile 사용자 프로필
     * @param ncsCode 목표 NCS 코드
     * @param ksaAnalysis KSA 역량 분석 결과
     * @return 추천 자격증 리스트
     */
    @Cacheable(value = OpenAiCacheConfig.CERTIFICATION_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<List<CertificationRecommendationResponse>> recommendCertifications(
            Profile profile,
            String ncsCode,
            KsaAnalysisResponse ksaAnalysis) {

        log.info("[CertificationRecommendationWorkflow] Starting certification recommendation - memberId: {}, ncsCode: {}",
                profile.getMember().getId(), ncsCode);

        // 1. NCS 코드 검증 및 직무 정보 조회
        return Mono.fromCallable(() -> {
            Set<NcsOccupation> validOccupations = ncsApiService.filterValidNcsCodes(Set.of(ncsCode));
            if (validOccupations.isEmpty()) {
                log.warn("[CertificationRecommendationWorkflow] Invalid NCS code: {}", ncsCode);
                return null;
            }
            return validOccupations.iterator().next();
        }).flatMap(ncsOccupation -> {
            if (ncsOccupation == null) {
                log.warn("[CertificationRecommendationWorkflow] NCS occupation is null, returning empty list");
                return Mono.just(Collections.<CertificationRecommendationResponse>emptyList());
            }

            log.info("[CertificationRecommendationWorkflow] NCS occupation found - code: {}, name: {}",
                    ncsOccupation.getDutyCd(), ncsOccupation.getDutyNm());

            // 2. 사용자 보유 자격증 추출
            Set<String> ownedCertificates = extractOwnedCertifications(profile);
            log.info("[CertificationRecommendationWorkflow] User owns {} certifications: {}",
                    ownedCertificates.size(), ownedCertificates);

            // 3. NCS 직무명 기반 자격증 추천
            return generateRecommendations(ncsOccupation, ksaAnalysis, ownedCertificates);
        })
        .doOnSuccess(recommendations ->
                log.info("[CertificationRecommendationWorkflow] Certification recommendation completed - count: {}",
                        recommendations != null ? recommendations.size() : 0)
        )
        .doOnError(e ->
                log.error("[CertificationRecommendationWorkflow] Error during certification recommendation: {}",
                        e.getMessage(), e)
        )
        .onErrorResume(e -> {
            log.error("[CertificationRecommendationWorkflow] Returning empty list due to error", e);
            return Mono.just(Collections.emptyList());
        });
    }

    /**
     * 사용자 보유 자격증 추출
     */
    private Set<String> extractOwnedCertifications(Profile profile) {
        if (profile.getResume() == null || profile.getResume().getCertificates() == null) {
            return Collections.emptySet();
        }

        return profile.getResume().getCertificates().stream()
                .map(rc -> rc.getCertificate().getJmfldnm())
                .collect(Collectors.toSet());
    }

    /**
     * AI 기반 자격증 추천 생성
     *
     * 전략:
     * 1. OpenAI를 사용하여 사용자 프로필, NCS 직무, KSA gap 분석
     * 2. AI 실패 시 정적 데이터베이스 기반 폴백
     */
    private Mono<List<CertificationRecommendationResponse>> generateRecommendations(
            NcsOccupation ncsOccupation,
            KsaAnalysisResponse ksaAnalysis,
            Set<String> ownedCertificates) {

        log.info("[CertificationRecommendationWorkflow] Starting AI-based certification recommendation");

        // AI 기반 추천 시도
        return getCertificationsFromAI(ncsOccupation, ksaAnalysis, ownedCertificates)
                .onErrorResume(e -> {
                    log.warn("[CertificationRecommendationWorkflow] AI recommendation failed, using fallback: {}",
                            e.getMessage());
                    // Fallback: 정적 데이터베이스 기반 추천
                    return generateFallbackRecommendations(ncsOccupation, ksaAnalysis, ownedCertificates);
                })
                .flatMap(aiRecommendations -> {
                    if (aiRecommendations.isEmpty()) {
                        log.warn("[CertificationRecommendationWorkflow] AI returned empty, using fallback");
                        return generateFallbackRecommendations(ncsOccupation, ksaAnalysis, ownedCertificates);
                    }
                    log.info("[CertificationRecommendationWorkflow] AI recommendation successful: {} certifications",
                            aiRecommendations.size());
                    return Mono.just(aiRecommendations);
                });
    }

    /**
     * OpenAI를 사용하여 자격증 추천
     */
    private Mono<List<CertificationRecommendationResponse>> getCertificationsFromAI(
            NcsOccupation ncsOccupation,
            KsaAnalysisResponse ksaAnalysis,
            Set<String> ownedCertificates) {

        log.info("[CertificationRecommendationWorkflow] Preparing AI prompt for certification recommendation");

        // KSA gap 정보 포맷팅
        String ksaGapSummary = formatKsaGapSummary(ksaAnalysis);
        double avgGap = calculateAverageGap(ksaAnalysis);

        // 사용자 보유 자격증 목록
        String ownedCertsText = ownedCertificates.isEmpty() ? "없음" : String.join(", ", ownedCertificates);

        // AI 프롬프트 생성
        String prompt = String.format("""
                당신은 한국 IT 업계의 자격증 추천 전문가입니다.
                사용자의 목표 직무, 현재 역량, 역량 부족 부분을 분석하여 적합한 자격증을 추천하세요.

                [목표 NCS 직무]
                - 코드: %s
                - 직무명: %s
                - 설명: %s

                [사용자 현재 역량 분석 (KSA Gap Analysis)]
                평균 Gap: %.2f (0.0=완벽, 1.0=부족)
                %s

                [사용자 보유 자격증]
                %s

                [추천 기준]
                1. NCS 직무와 직접 관련된 자격증만 추천
                2. KSA gap이 큰 영역을 보완할 수 있는 자격증 우선
                3. 사용자가 이미 보유한 자격증은 제외
                4. 실제 존재하는 한국 공인 자격증만 추천 (정보처리기사, SQLD, AWS Certified 등)
                5. 난이도와 준비 기간을 현실적으로 평가

                [한국 IT 자격증 예시]
                - 국가기술자격: 정보처리기사, 정보처리산업기사, 정보보안기사, 정보통신기사, 빅데이터분석기사
                - 민간자격: SQLD, SQLP, 리눅스마스터, 네트워크관리사, 정보기술자격(ITQ)
                - 국제자격: AWS Certified Developer, CKAD, CISSP, CompTIA

                [출력 형식]
                반드시 아래 JSON 배열 형식으로만 응답해주세요. 설명이나 다른 텍스트는 포함하지 마세요.
                최대 5개까지 추천하며, 우선순위가 높은 순서대로 배열하세요.

                [
                  {
                    "certificationName": "자격증명",
                    "issuingOrganization": "발급기관",
                    "category": "카테고리 (정보기술/데이터베이스/클라우드/보안/네트워크/빅데이터)",
                    "difficultyLevel": 난이도(1~5),
                    "priority": 우선순위(1~5, 1이 가장 높음),
                    "reason": "추천 이유 (1~2문장, KSA gap과 연결하여 설명)",
                    "gapResolutionContribution": gap해소기여도(0~100),
                    "estimatedPreparationMonths": 준비기간(개월)
                  }
                ]
                """,
                ncsOccupation.getDutyCd(),
                ncsOccupation.getDutyNm(),
                ncsOccupation.getDutyDef() != null ? ncsOccupation.getDutyDef() : "설명 없음",
                avgGap,
                ksaGapSummary,
                ownedCertsText
        );

        log.debug("[CertificationRecommendationWorkflow] AI prompt prepared, calling OpenAI");

        // OpenAI API 호출 (Assistant 방식)
        return openAiClient.generateAssistantResponse(prompt)
                .flatMap(aiResponse -> {
                    log.info("[CertificationRecommendationWorkflow] Received AI response, parsing JSON");
                    return parseAiCertificationResponse(aiResponse, ncsOccupation.getDutyCd());
                })
                .doOnError(e -> log.error("[CertificationRecommendationWorkflow] AI call failed: {}",
                        e.getMessage(), e));
    }

    /**
     * KSA gap 정보를 요약 텍스트로 포맷팅
     */
    private String formatKsaGapSummary(KsaAnalysisResponse ksaAnalysis) {
        if (ksaAnalysis == null) {
            return "KSA 분석 정보 없음";
        }

        StringBuilder summary = new StringBuilder();

        // Knowledge gaps
        if (ksaAnalysis.knowledgeItems() != null && !ksaAnalysis.knowledgeItems().isEmpty()) {
            summary.append("\n[지식(Knowledge) 부족 항목]\n");
            ksaAnalysis.knowledgeItems().stream()
                    .filter(item -> item.scoreGap() > 0.2)
                    .limit(3)
                    .forEach(item -> summary.append(String.format("- %s (gap: %.2f)\n",
                            item.itemName(), item.scoreGap())));
        }

        // Skill gaps
        if (ksaAnalysis.skillItems() != null && !ksaAnalysis.skillItems().isEmpty()) {
            summary.append("\n[기술(Skill) 부족 항목]\n");
            ksaAnalysis.skillItems().stream()
                    .filter(item -> item.scoreGap() > 0.2)
                    .limit(3)
                    .forEach(item -> summary.append(String.format("- %s (gap: %.2f)\n",
                            item.itemName(), item.scoreGap())));
        }

        // Attitude gaps
        if (ksaAnalysis.attitudeItems() != null && !ksaAnalysis.attitudeItems().isEmpty()) {
            summary.append("\n[태도(Attitude) 부족 항목]\n");
            ksaAnalysis.attitudeItems().stream()
                    .filter(item -> item.scoreGap() > 0.2)
                    .limit(3)
                    .forEach(item -> summary.append(String.format("- %s (gap: %.2f)\n",
                            item.itemName(), item.scoreGap())));
        }

        return summary.length() > 0 ? summary.toString() : "역량 부족 항목 없음";
    }

    /**
     * AI 응답을 파싱하여 자격증 추천 리스트로 변환
     */
    private Mono<List<CertificationRecommendationResponse>> parseAiCertificationResponse(
            String aiResponse,
            String ncsCode) {

        return Mono.fromCallable(() -> {
            log.debug("[CertificationRecommendationWorkflow] Parsing AI response: {}", aiResponse);

            // JSON 추출 (코드 블록 제거)
            String jsonContent = aiResponse.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
            }
            if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring(3);
            }
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            jsonContent = jsonContent.trim();

            log.debug("[CertificationRecommendationWorkflow] Cleaned JSON: {}", jsonContent);

            // JSON 파싱
            List<CertificationRecommendationDto> dtos = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<CertificationRecommendationDto>>() {}
            );

            log.info("[CertificationRecommendationWorkflow] Parsed {} certification recommendations from AI",
                    dtos.size());

            // DTO를 Response로 변환
            return dtos.stream()
                    .map(dto -> CertificationRecommendationResponse.builder()
                            .certificationName(dto.certificationName())
                            .issuingOrganization(dto.issuingOrganization())
                            .category(dto.category())
                            .difficultyLevel(dto.difficultyLevel())
                            .priority(dto.priority())
                            .reason(dto.reason())
                            .isOwned(false)
                            .gapResolutionContribution(dto.gapResolutionContribution())
                            .relatedNcsCode(ncsCode)
                            .estimatedPreparationMonths(dto.estimatedPreparationMonths())
                            .build())
                    .collect(Collectors.toList());
        });
    }

    /**
     * Fallback: 정적 데이터베이스 기반 자격증 추천
     */
    private Mono<List<CertificationRecommendationResponse>> generateFallbackRecommendations(
            NcsOccupation ncsOccupation,
            KsaAnalysisResponse ksaAnalysis,
            Set<String> ownedCertificates) {

        String ncsName = ncsOccupation.getDutyNm();
        log.info("[CertificationRecommendationWorkflow] Generating fallback recommendations for NCS: {}", ncsName);

        // 1. NCS 직무명에서 키워드 추출
        List<String> keywords = extractKeywords(ncsName);
        log.info("[CertificationRecommendationWorkflow] Extracted keywords: {}", keywords);

        // 2. 키워드에 매핑된 자격증 템플릿 수집
        List<CertificationTemplate> templates = new ArrayList<>();
        for (String keyword : keywords) {
            List<CertificationTemplate> matched = CERTIFICATION_DATABASE.get(keyword);
            if (matched != null) {
                templates.addAll(matched);
                log.debug("[CertificationRecommendationWorkflow] Found {} templates for keyword: {}",
                        matched.size(), keyword);
            }
        }

        // 키워드 매칭 실패 시 기본 자격증 사용
        if (templates.isEmpty()) {
            log.warn("[CertificationRecommendationWorkflow] No templates found for keywords, using default");
            templates.addAll(CERTIFICATION_DATABASE.get("기본"));
        }

        // 3. 중복 제거 및 보유 자격증 필터링
        Map<String, CertificationTemplate> uniqueTemplates = templates.stream()
                .collect(Collectors.toMap(
                        CertificationTemplate::name,
                        t -> t,
                        (t1, t2) -> t1 // 중복 시 첫 번째 선택
                ));

        List<CertificationTemplate> filteredTemplates = uniqueTemplates.values().stream()
                .filter(template -> !ownedCertificates.contains(template.name()))
                .collect(Collectors.toList());

        log.info("[CertificationRecommendationWorkflow] Filtered to {} unique, unowned certifications",
                filteredTemplates.size());

        // 4. KSA gap 기반 우선순위 조정
        double avgGap = calculateAverageGap(ksaAnalysis);
        log.info("[CertificationRecommendationWorkflow] Average KSA gap: {}", avgGap);

        // 5. 응답 DTO 변환 및 정렬
        List<CertificationRecommendationResponse> recommendations = filteredTemplates.stream()
                .map(template -> convertToResponse(template, ncsOccupation.getDutyCd(), avgGap))
                .sorted(Comparator.comparingInt(CertificationRecommendationResponse::priority))
                .limit(5) // 최대 5개
                .collect(Collectors.toList());

        log.info("[CertificationRecommendationWorkflow] Generated {} fallback recommendations", recommendations.size());
        return Mono.just(recommendations);
    }

    /**
     * NCS 직무명에서 키워드 추출
     */
    private List<String> extractKeywords(String ncsName) {
        List<String> keywords = new ArrayList<>();
        String lowerName = ncsName.toLowerCase();

        // 정확한 매칭 우선
        for (String key : CERTIFICATION_DATABASE.keySet()) {
            if (lowerName.contains(key.toLowerCase())) {
                keywords.add(key);
            }
        }

        // 매칭 실패 시 기본 키워드 사용
        if (keywords.isEmpty()) {
            keywords.add("기본");
        }

        return keywords;
    }

    /**
     * KSA 평균 gap 계산
     */
    private double calculateAverageGap(KsaAnalysisResponse ksaAnalysis) {
        if (ksaAnalysis == null) {
            return 0.5; // 기본값
        }

        List<Double> gaps = new ArrayList<>();

        // Knowledge gap
        if (ksaAnalysis.knowledgeItems() != null) {
            gaps.addAll(ksaAnalysis.knowledgeItems().stream()
                    .map(KsaAnalysisResponse.KsaItem::scoreGap)
                    .collect(Collectors.toList()));
        }

        // Skill gap
        if (ksaAnalysis.skillItems() != null) {
            gaps.addAll(ksaAnalysis.skillItems().stream()
                    .map(KsaAnalysisResponse.KsaItem::scoreGap)
                    .collect(Collectors.toList()));
        }

        // Attitude gap
        if (ksaAnalysis.attitudeItems() != null) {
            gaps.addAll(ksaAnalysis.attitudeItems().stream()
                    .map(KsaAnalysisResponse.KsaItem::scoreGap)
                    .collect(Collectors.toList()));
        }

        return gaps.isEmpty() ? 0.5 : gaps.stream().mapToDouble(Double::doubleValue).average().orElse(0.5);
    }

    /**
     * 템플릿을 응답 DTO로 변환
     */
    private CertificationRecommendationResponse convertToResponse(
            CertificationTemplate template,
            String ncsCode,
            double avgGap) {

        // Gap에 따라 우선순위 및 기여도 조정
        int adjustedPriority = template.basePriority();
        int adjustedContribution = template.baseGapContribution();

        // Gap이 크면 우선순위 상승 (숫자 감소)
        if (avgGap > 0.3) {
            adjustedPriority = Math.max(1, adjustedPriority - 1);
            adjustedContribution += 10;
        }

        // 추천 이유 생성
        String reason = generateReason(template, avgGap);

        return CertificationRecommendationResponse.builder()
                .certificationName(template.name())
                .issuingOrganization(template.organization())
                .category(template.category())
                .difficultyLevel(template.difficulty())
                .priority(adjustedPriority)
                .reason(reason)
                .isOwned(false)
                .gapResolutionContribution(Math.min(adjustedContribution, 100))
                .relatedNcsCode(ncsCode)
                .estimatedPreparationMonths(template.estimatedMonths())
                .build();
    }

    /**
     * 자격증 추천 이유 생성
     */
    private String generateReason(CertificationTemplate template, double avgGap) {
        String baseReason = String.format("%s 분야의 전문성을 인증하는 %s 자격증입니다.",
                template.category(), template.name());

        if (avgGap > 0.3) {
            return baseReason + " 현재 역량 gap을 해소하는데 크게 도움이 될 것입니다.";
        } else if (avgGap > 0.15) {
            return baseReason + " 추가 역량 향상에 도움이 됩니다.";
        } else {
            return baseReason + " 보유 역량을 공식적으로 인증하는데 유용합니다.";
        }
    }

    /**
     * AI 자격증 추천 결과 DTO (향후 AI 기반 추천 활성화 시 사용)
     */
    @SuppressWarnings("unused")
    private record CertificationRecommendationDto(
            String certificationName,
            String issuingOrganization,
            String category,
            Integer difficultyLevel,
            Integer priority,
            String reason,
            Integer gapResolutionContribution,
            String relatedNcsCode,
            Integer estimatedPreparationMonths
    ) {}
}
