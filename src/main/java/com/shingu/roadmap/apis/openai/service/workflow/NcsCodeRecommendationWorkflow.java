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
                희망 직무: [%s] 와 가장 관련성이 높은 NCS 직무 코드를 3~5개 추천해줘.

                [출력 형식]
                - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
                - 예시: ["20010202", "20010206"]
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
            당신은 제공된 이력서와 사용자 정보를 **오직 그 근거로만** 분석하는 고도로 정확한 커리어 분석 AI입니다.

            [규칙]
            1.  **가장 중요한 규칙:** 당신은 **반드시 [사용자 정보]에 명시된 내용만을 근거로 NCS 코드를 추천**해야 합니다.
            2.  제공된 정보에 명시되지 않은 기술, 경험, 역량을 **절대로 추론하거나 상상해서는 안 됩니다.**
            3.  사용자의 핵심 경력(예: '쇼핑몰 프로젝트 백엔드 개발')을 절대 무시해서는 안 됩니다.
            4.  '정보처리기사', '컴퓨터공학' 등 일반적인 키워드에서 '인공지능', '빅데이터' 등 언급되지 않은 최신 기술 트렌드를 임의로 연결하지 마세요.

            [분석 가이드라인]
            1. 이력서의 프로젝트 경험과 역할을 **가장 중요한 증거**로 삼으세요.
            2. 기술 스택의 숙련도(Proficiency)를 참고하여 직무의 깊이를 판단하세요. (예: ADVANCED 등급 기술은 핵심 역량)
            3. 자격증은 보조적인 지표로만 활용하세요.
            4. 위 [규칙]과 [분석 가이드라인]을 종합하여, 사용자의 경험과 가장 직접적으로 관련된 NCS 코드 3~5개를 신중하게 선택하세요.

            [사용자 정보]
            - 기술스택: %s
            - 자격증: %s
            - 이력서: %s

            [출력 형식]
            - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
            - 예시: ["20010202", "20010206"]
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

                                    // 최소 2개 이상 성공하면 검증 생략하고 사용
                                    if (validCount >= 2) {
                                        log.info("Skipping AI validation due to partial results. Using {} validated codes directly.", validCount);
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
                당신은 NCS 코드 추천의 정확성을 검증하는 선임 커리어 컨설턴트입니다.
                후배가 추천한 아래 NCS 직무 목록을 사용자 정보와 **매우 엄격하게** 비교하여 검증하는 것이 당신의 임무입니다.

                [검증 기준]
                1.  **핵심 증거:** 이력서의 프로젝트 경험(예: '쇼핑몰 개발')과 역할(예: '백엔드 개발')이 검증 대상 NCS 직무와 직접적으로 일치하는가?
                2.  **기술 증거:** 사용자의 핵심 기술 스택(예: 'Java (ADVANCED)')이 검증 대상 NCS 직무의 요구 역량과 부합하는가?
                3.  **능력단위 적합성:** NCS 직무의 능력단위 목록이 사용자의 실제 프로젝트 수행 역량과 일치하는가?
                4.  **직무 정의 연관성:** NCS 직무 설명이 사용자의 실제 경험 및 역량과 명확하게 연관되는가?
                5.  **부합하지 않는 경우:** 위 기준에 하나라도 명확하게 부합하지 않으면, 그 코드는 부적합한 것으로 간주해야 합니다.

                [사용자 정보]
                - 기술스택: %s
                - 자격증: %s
                - 이력서: %s

                [검증 대상 NCS 직무 상세 정보]
                다음은 각 NCS 직무의 코드, 이름, 설명, 그리고 요구되는 능력단위 목록입니다:
                %s

                [과업]
                - 위 [검증 대상 NCS 직무 상세 정보] 중에서 [검증 기준]에 **완벽하게 부합하는 코드만** 골라주세요.
                - 각 직무의 이름, 설명, 그리고 **능력단위 목록**을 모두 참고하여 사용자 프로필과의 적합성을 정확히 판단하세요.
                - 능력단위가 사용자의 실제 프로젝트 경험과 얼마나 일치하는지 중요하게 고려하세요.
                - 만약 목록에 있는 직무가 모두 부적합하다면, 결과에 "REGENERATE"만 포함시켜 주세요.

                [출력 형식]
                - 반드시 아래 JSON 배열 형식 중 하나로만 응답해주세요.
                - 적합한 코드가 있을 경우: ["20010202", "20010206"]
                - 적합한 코드가 없을 경우: ["REGENERATE"]
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
                선임 컨설턴트의 검토 결과, 이전 추천이 모두 부적절하다고 판단되었습니다.
                이는 사용자의 핵심 경험을 무시하고 잘못된 추론을 했기 때문일 가능성이 높습니다.
                사용자 정보를 더 깊이, 그리고 더 정확하게 분석하여 새로운 NCS 코드를 추천해야 합니다.

                [사용자 정보]
                - 기술스택: %s
                - 자격증: %s
                - 이력서: %s

                [실패한 이전 추천 (상세 정보 포함, 반드시 제외)]
                다음 NCS 직무들은 사용자 프로필과 적합하지 않다고 판단되었습니다:
                %s

                [새로운 추천 가이드라인]
                1. **오직 이력서에 명시된 프로젝트의 핵심 역할(예: '백엔드 개발')과 성과(예: 'API 성능 개선')에만 집중하세요.**
                2. 위에 제시된 실패한 추천의 직무명과 능력단위를 참고하여, 왜 부적합한지 이해하세요.
                3. 이 핵심 증거와 가장 직접적으로 관련된 **새로운** NCS 코드 3~5개를 추천해주세요.
                4. 이전과 같은 실수를 반복하지 않도록, 절대 이력서에 없는 내용을 상상하지 마세요.
                5. 실패한 추천 목록에 있는 코드는 절대 다시 추천하지 마세요.

                [출력 형식]
                - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
                - 예시: ["20010204", "20010602"]
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
