package com.shingu.roadmap.apis.openai.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.apis.openai.util.JsonResponseParser;
import com.shingu.roadmap.apis.openai.util.ResumeTextFormatter;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NCS 코드 추천 워크플로우
 * 사용자 프로필 기반 NCS 직무 코드 추천 및 검증을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NcsCodeRecommendationWorkflow {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final NcsApiService ncsApiService;
    private final JsonResponseParser jsonResponseParser;
    private final ResumeTextFormatter resumeTextFormatter;

    /**
     * 희망 직무 기반 NCS 코드 추천
     */
    @Cacheable(value = OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<Set<String>> recommendDesiredJobCodeUsingAssistant(String desiredJob) {
        String userPrompt = String.format(
                """
                희망 직무: [%s] 와 직접적으로 관련된 핵심 NCS 직무 코드만 추천해줘.

                [추천 가이드라인]
                - 희망 직무와 **직접 일치**하는 핵심 직무만 선정 (3~4개)
                - 연관 직무나 확장 가능한 직무는 제외
                - 예시: "백엔드 개발자" → 응용SW 엔지니어링, DB 엔지니어링, 서버 프로그램 개발

                [출력 형식]
                - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
                - 예시: ["20010202", "20010204", "20010206"]
                - 3~4개의 핵심 직무 코드만 포함
                - 설명이나 다른 텍스트는 절대 포함하지 마세요.
                """,
                desiredJob
        );
        return getNcsCodesFromAssistant(userPrompt);
    }

    /**
     * Profile 객체를 받아 사용자의 실제 역량에 기반한 NCS 코드를 추천합니다.
     * AI가 잘못된 추론을 하지 못하도록 프롬프트를 대폭 강화했습니다.
     *
     * 3단계 프로세스:
     * 1. AI가 사용자 프로필 기반으로 NCS 코드 추천
     * 2. NCS API를 통해 추천된 코드 검증 및 상세 정보 조회
     * 3. AI가 상세 정보(직무명, 설명)를 참고하여 최종 검증
     */
    @Cacheable(value = OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<Set<String>> recommendNcsCodeUsingAssistant(Profile profile) {
        String skillsWithProficiency = profile.getProfileSkills().stream()
                .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
                .collect(Collectors.joining(", "));
        String certificates = profile.getResume() != null ? profile.getResume().getCertificates().stream()
                .map(rc -> rc.getCertificate().getJmfldnm())
                .collect(Collectors.joining(", ")) : "";

        String initialPrompt = String.format("""
            당신은 제공된 이력서와 사용자 정보를 분석하여 **다양한 커리어 가능성**을 제시하는 커리어 컨설턴트 AI입니다.

            [핵심 원칙 - 매우 중요]
            1.  **프로젝트에서 수행한 역할**이 가장 중요한 증거입니다 (가중치 60%%)
                - 예: "백엔드 개발" 역할 → 응용SW 엔지니어링, DB 엔지니어링 등
                - 예: "프론트엔드 개발" 역할 → UI/UX 개발, 웹 퍼블리싱 등
                - 예: "인프라 구축" 역할 → 시스템 운영, 클라우드 엔지니어링 등

            2.  **단순 기술 스택 언급**만으로 직무를 추론하지 마세요 (가중치 20%%)
                - 예: AWS를 사용했다고 해서 자동으로 "클라우드 아키텍트"는 아님
                - 예: Docker를 사용했다고 해서 자동으로 "데브옵스 엔지니어"는 아님
                - **실제로 그 기술을 주도적으로 설계/운영했는지**가 핵심

            3.  **실제 업무 내용**과 NCS 직무의 능력단위를 비교하세요 (가중치 20%%)
                - "API 개발, DB 설계" → 응용SW 엔지니어링 ✅
                - "단순 AWS 배포" → 클라우드 아키텍트 ❌

            [추천 전략 - 핵심 직무만 선정]
            **핵심 직무 (3~4개)**
            - 프로젝트에서 **직접 수행한 역할**과 정확히 일치하는 직무만 추천
            - "백엔드 개발" → 응용SW 엔지니어링(20010202), DB 엔지니어링(20010204)
            - "프론트엔드 개발" → UI/UX 개발(20010203)
            - "시스템 운영" → 시스템 운영관리(20010302)
            - **연관 직무나 발전 가능 직무는 제외하고, 현재 수행 중인 핵심 역할만 포함**

            [중요한 제약사항]
            ❌ "AWS 사용" ≠ "클라우드 아키텍트" (설계/운영 주도 경험 필요)
            ❌ "Docker 사용" ≠ "데브옵스" (CI/CD 전체 구축 경험 필요)
            ❌ "보안 고려" ≠ "보안 전문가" (보안 감사/설계 경험 필요)
            ✅ "백엔드 API 개발" = "응용SW 엔지니어링" (정확한 매칭)

            [사용자 정보]
            - 기술스택: %s
            - 자격증: %s
            - 이력서 (프로젝트 역할 필수 확인): %s

            [과업]
            - **가장 먼저 이력서의 프로젝트 역할**을 확인하세요
            - 역할과 **직접 일치**하는 핵심 직무만 선정 (3~4개)
            - **연관 직무나 확장 가능 직무는 추천하지 마세요**

            [출력 형식]
            - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
            - 예시 (백엔드 개발자): ["20010202", "20010204", "20010206"]
            - 3~4개의 핵심 직무 코드만 포함
            - 설명이나 다른 텍스트는 절대 포함하지 마세요.
            """,
                skillsWithProficiency,
                certificates,
                resumeTextFormatter.resumeToText(profile.getResume())
        );

        // 1단계: AI가 NCS 코드 추천
        return getNcsCodesFromAssistant(initialPrompt)
                .flatMap(recommendedCodes -> {
                    if (recommendedCodes.isEmpty()) {
                        log.info("No NCS codes recommended by AI");
                        return Mono.just(Collections.emptySet());
                    }

                    log.info("AI recommended {} NCS codes: {}", recommendedCodes.size(), recommendedCodes);

                    // 2단계: NCS API를 통한 코드 검증 및 상세 정보 조회
                    return Mono.fromCallable(() -> ncsApiService.filterValidNcsCodes(recommendedCodes))
                            .flatMap(validOccupations -> {
                                if (validOccupations.isEmpty()) {
                                    log.warn("No valid NCS occupations found for recommended codes: {}", recommendedCodes);
                                    return Mono.just(Collections.emptySet());
                                }

                                int validCount = validOccupations.size();
                                int totalCount = recommendedCodes.size();
                                log.info("Found {} valid NCS occupations out of {} ({}%)",
                                        validCount, totalCount, (validCount * 100 / totalCount));

                                // 부분 성공 처리: 일부만 검증된 경우
                                if (validCount < totalCount) {
                                    log.warn("Partial validation detected: {}/{} codes successfully validated", validCount, totalCount);
                                    log.warn("Missing codes may cause false negatives in AI validation");

                                    // 최소 2개 이상 성공하면 검증 생략하고 사용 (핵심 직무 보존)
                                    if (validCount >= 2) {
                                        log.info("Skipping AI validation due to partial results. Using {} validated codes directly to preserve core jobs.", validCount);
                                        Set<String> validCodes = validOccupations.stream()
                                                .map(NcsOccupation::getDutyCd)
                                                .collect(Collectors.toSet());
                                        return Mono.just(validCodes);
                                    }
                                }

                                // 전부 성공한 경우에만 AI 검증 진행
                                log.info("All codes validated successfully, proceeding with AI validation");

                                // 3단계: AI가 상세 정보를 참고하여 최종 검증
                                return validateNcsCodesWithAssistant(profile, validOccupations)
                                        .map(validatedCodes -> {
                                            log.info("Original recommendations: {}, Validated recommendations: {}",
                                                    recommendedCodes, validatedCodes);
                                            return validatedCodes;
                                        });
                            });
                });
    }

    /**
     * AI가 추천한 NCS 코드들이 사용자의 프로필에 적합한지 검증합니다.
     * NCS 상세 정보(직무명, 설명, 능력단위)를 포함하여 더 정확한 검증을 수행합니다.
     *
     * @param profile 사용자 프로필
     * @param validOccupations filterValidNcsCodes를 통해 검증된 NCS 직무 정보
     * @return 검증된 NCS 코드 Set
     */
    private Mono<Set<String>> validateNcsCodesWithAssistant(Profile profile, Set<NcsOccupation> validOccupations) {
        String skillsWithProficiency = profile.getProfileSkills().stream()
                .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
                .collect(Collectors.joining(", "));
        String certificates = profile.getResume() != null ? profile.getResume().getCertificates().stream()
                .map(rc -> rc.getCertificate().getJmfldnm())
                .collect(Collectors.joining(", ")) : "";

        // NCS 상세 정보 조회 (능력단위 포함)
        return Mono.fromCallable(() -> {
            Map<String, com.shingu.roadmap.apis.openai.dto.request.NcsDetailedInfoDto> ncsDetailsMap = new HashMap<>();

            for (NcsOccupation occupation : validOccupations) {
                try {
                    var compUnitResponse = ncsApiService.getNcsCompUnit(occupation.getDutyCd());
                    var detailedInfo = com.shingu.roadmap.apis.openai.dto.request.NcsDetailedInfoDto.from(
                            occupation,
                            compUnitResponse.data()
                    );
                    ncsDetailsMap.put(occupation.getDutyCd(), detailedInfo);
                    log.debug("Fetched {} competency units for NCS code {}",
                            compUnitResponse.data() != null ? compUnitResponse.data().size() : 0,
                            occupation.getDutyCd());
                } catch (Exception e) {
                    log.warn("Failed to fetch competency units for NCS code {}: {}", occupation.getDutyCd(), e.getMessage());
                    // 능력단위 조회 실패시에도 기본 정보는 포함
                    ncsDetailsMap.put(occupation.getDutyCd(),
                            com.shingu.roadmap.apis.openai.dto.request.NcsDetailedInfoDto.from(occupation, List.of()));
                }
            }

            return ncsDetailsMap;
        })
        .flatMap(ncsDetailsMap -> {
            // NCS 상세 정보를 JSON 형식으로 변환
            String ncsDetailsJson;
            try {
                ncsDetailsJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(ncsDetailsMap.values());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize NCS details for validation", e);
                // Fallback: 간단한 텍스트 포맷
                ncsDetailsJson = validOccupations.stream()
                        .map(occ -> String.format("- %s: %s\n  설명: %s",
                                occ.getDutyCd(),
                                occ.getDutyNm(),
                                occ.getDutyDef() != null ? occ.getDutyDef() : "설명 없음"))
                        .collect(Collectors.joining("\n\n"));
            }

            String validationPrompt = String.format("""
                당신은 NCS 코드 추천의 정확성을 검증하는 커리어 컨설턴트입니다.
                후배가 추천한 아래 NCS 직무 목록을 사용자 정보와 비교하여 **핵심 직무만** 선별하는 것이 당신의 임무입니다.

                [중요 원칙]
                - **프로젝트에서 수행한 역할**과 직접 일치하는 직무만 선택합니다.
                - 연관 직무나 발전 가능 직무는 제외하고, **현재 수행 중인 핵심 역할만** 포함합니다.
                - 단순 기술 스택 언급만으로 추론된 직무는 제외합니다.

                [검증 기준 - 엄격한 평가]
                1.  **프로젝트 역할 일치 (필수)**: 이력서의 프로젝트에서 명시한 역할과 NCS 직무가 정확히 일치하는가?
                    - 예: "백엔드 개발" 역할 → 응용SW 엔지니어링 ✅
                    - 예: "백엔드 개발" 역할 → 클라우드 아키텍트 ❌ (AWS 사용만으로는 부족)

                2.  **실제 업무 내용 일치 (필수)**: 실제로 수행한 업무가 NCS 능력단위와 직접 연관되는가?
                    - 예: "API 개발, DB 설계" → 응용SW 엔지니어링 ✅
                    - 예: "단순 AWS 배포" → 클라우드 아키텍트 ❌

                3.  **기술 주도성 (필수)**: 해당 기술을 단순 사용한 것인가, 주도적으로 설계/운영한 것인가?
                    - 예: "CI/CD 파이프라인 전체 구축" → 데브옵스 ✅
                    - 예: "Docker로 배포만 함" → 데브옵스 ❌

                [과업]
                - 위 [검증 대상 NCS 직무 상세 정보]를 사용자의 **프로젝트 역할**과 정확히 비교하세요.
                - **프로젝트에서 직접 수행한 역할**과 일치하는 핵심 직무만 선택하세요 (2~3개).
                - 연관 직무, 확장 가능 직무, 발전 가능 직무는 모두 제외하세요.
                - 완전히 동떨어진 직무는 당연히 제외하세요.
                - 만약 정말 모든 직무가 부적합하다면(매우 드문 경우), 결과에 "REGENERATE"만 포함시켜 주세요.

                [사용자 정보]
                - 기술스택: %s
                - 자격증: %s
                - 이력서: %s

                [검증 대상 NCS 직무 상세 정보]
                다음은 각 NCS 직무의 코드, 이름, 설명, 그리고 요구되는 능력단위 목록입니다:
                %s

                [출력 형식]
                - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
                - 최소 3개 이상 포함 권장: ["20010202", "20010206", "20010301", "20020105"]
                - 모두 부적합한 경우만: ["REGENERATE"]
                - 다른 설명은 절대 추가하지 마세요.
                """,
                    skillsWithProficiency,
                    certificates,
                    resumeTextFormatter.resumeToText(profile.getResume()),
                    ncsDetailsJson
            );

            // validOccupations에서 코드 Set 추출
            Set<String> recommendedCodes = validOccupations.stream()
                    .map(NcsOccupation::getDutyCd)
                    .collect(Collectors.toSet());

            return openAiClient.generateAssistantResponse(validationPrompt)
                    .flatMap(validationResponse -> {
                        try {
                            List<String> codes = objectMapper.readValue(validationResponse.trim(), new TypeReference<>() {});
                            if (codes.contains("REGENERATE")) {
                                log.info("All recommended NCS codes were inappropriate, generating new recommendations.");
                                return generateNewNcsCodesWithAssistant(profile, recommendedCodes);
                            }

                            Set<String> validatedCodes = new HashSet<>(codes);
                            validatedCodes.retainAll(recommendedCodes);

                            if (validatedCodes.isEmpty() && !recommendedCodes.isEmpty()) {
                                log.warn("AI validation returned empty result for non-empty input, generating new recommendations.");
                                return generateNewNcsCodesWithAssistant(profile, recommendedCodes);
                            }

                            log.info("Validation completed: {} out of {} codes validated successfully",
                                    validatedCodes.size(), recommendedCodes.size());
                            return Mono.just(validatedCodes);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to parse validation response: {}. Generating new recommendations.", validationResponse, e);
                            return generateNewNcsCodesWithAssistant(profile, recommendedCodes);
                        }
                    });
        });
    }

    /**
     * 기존 추천이 부적절할 때 새로운 NCS 코드를 생성합니다.
     * NCS 코드와 함께 상세 정보(직무명, 설명, 능력단위)를 제공하여 더 정확한 매칭을 수행합니다.
     *
     * @param profile 사용자 프로필
     * @param rejectedCodes 거부된 NCS 코드 목록
     * @return 새로운 NCS 코드 Set
     */
    private Mono<Set<String>> generateNewNcsCodesWithAssistant(Profile profile, Set<String> rejectedCodes) {
        String skillsWithProficiency = profile.getProfileSkills().stream()
                .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
                .collect(Collectors.joining(", "));
        String certificates = profile.getResume() != null ? profile.getResume().getCertificates().stream()
                .map(rc -> rc.getCertificate().getJmfldnm())
                .collect(Collectors.joining(", ")) : "";

        // 거부된 코드들의 상세 정보 조회
        return Mono.fromCallable(() -> {
            Set<NcsOccupation> rejectedOccupations = ncsApiService.filterValidNcsCodes(rejectedCodes);

            // 각 NCS 코드에 대해 능력단위 정보 조회
            Map<String, com.shingu.roadmap.apis.openai.dto.request.NcsDetailedInfoDto> rejectedNcsDetails = new HashMap<>();

            for (NcsOccupation occupation : rejectedOccupations) {
                try {
                    var compUnitResponse = ncsApiService.getNcsCompUnit(occupation.getDutyCd());
                    var detailedInfo = com.shingu.roadmap.apis.openai.dto.request.NcsDetailedInfoDto.from(
                            occupation,
                            compUnitResponse.data()
                    );
                    rejectedNcsDetails.put(occupation.getDutyCd(), detailedInfo);
                } catch (Exception e) {
                    log.warn("Failed to fetch competency units for NCS code {}: {}", occupation.getDutyCd(), e.getMessage());
                    // 능력단위 조회 실패시에도 기본 정보는 포함
                    rejectedNcsDetails.put(occupation.getDutyCd(),
                            com.shingu.roadmap.apis.openai.dto.request.NcsDetailedInfoDto.from(occupation, List.of()));
                }
            }

            return rejectedNcsDetails;
        })
        .flatMap(rejectedNcsDetails -> {
            // NCS 상세 정보를 JSON 형식으로 변환
            String rejectedNcsDetailsJson;
            try {
                rejectedNcsDetailsJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(rejectedNcsDetails.values());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize rejected NCS details", e);
                rejectedNcsDetailsJson = rejectedCodes.toString();
            }

            String regenerationPrompt = String.format("""
                이전 추천이 사용자와 맞지 않다고 판단되었습니다.
                **프로젝트에서 수행한 역할**을 중심으로 새로운 핵심 직무 NCS 코드를 추천해야 합니다.

                [사용자 정보]
                - 기술스택: %s
                - 자격증: %s
                - 이력서: %s

                [제외해야 할 코드 (반드시 제외)]
                다음 NCS 직무들은 이미 검토되었으므로 다시 추천하지 마세요:
                %s

                [새로운 추천 전략]
                1. **이력서의 프로젝트 역할**을 정확히 확인하세요
                   - "백엔드 개발" 역할 → 응용SW 엔지니어링, DB 엔지니어링
                   - "프론트엔드 개발" 역할 → UI/UX 개발
                   - "시스템 운영" 역할 → 시스템 운영관리
                2. **역할과 직접 일치하는 핵심 직무만** 선정하세요
                3. 단순 기술 스택 언급으로 확장된 직무는 제외하세요
                4. **총 3~4개**의 핵심 직무 코드만 제시하세요
                5. 제외 목록의 코드는 **절대 포함하지 마세요**

                [출력 형식]
                - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
                - 3~4개 예시: ["20010202", "20010204", "20010206"]
                - 설명은 절대 추가하지 마세요.
                """,
                    skillsWithProficiency,
                    certificates,
                    resumeTextFormatter.resumeToText(profile.getResume()),
                    rejectedNcsDetailsJson
            );

            return getNcsCodesFromAssistant(regenerationPrompt)
                    .map(newCodes -> {
                        newCodes.removeAll(rejectedCodes);
                        log.info("Generated new NCS codes: {} (excluding rejected codes: {})", newCodes, rejectedCodes);
                        return newCodes;
                    });
        });
    }

    /**
     * Assistant API를 호출하고 응답(JSON 배열 문자열)을 Set<String>으로 파싱합니다.
     * (파싱 실패 시 안전하게 빈 Set을 반환하도록 로직 유지)
     */
    private Mono<Set<String>> getNcsCodesFromAssistant(String userPrompt) {
        return openAiClient.generateAssistantResponse(userPrompt)
                .map(response -> {
                    if (log.isDebugEnabled()) {
                        jsonResponseParser.logJsonExtractionProcess(response);
                    }
                    return jsonResponseParser.parseStringSet(response);
                });
    }
}
