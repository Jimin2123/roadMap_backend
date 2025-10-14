package com.shingu.roadmap.apis.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.careernet.service.CareerNetCodeProvider;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.cache.OpenAiCacheKeyGenerator;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.apis.openai.dto.request.GptUserPromptRequest;
import com.shingu.roadmap.apis.openai.dto.request.GptUserProfileDto;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import com.shingu.roadmap.apis.openai.util.JsonResponseParser;
import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.resume.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

  private final OpenAiClient openAiClient;
  private final ObjectMapper objectMapper;
  private final CareerNetCodeProvider careerNetCodeProvider;
  private final SecureLogger secureLogger;
  private final JsonResponseParser jsonResponseParser;

  /**
   * 사용자 정보를 바탕으로 부족한 역량을 보완할 훈련과정을 추천합니다.
   * (AI가 주어진 목록 내에서만 응답하도록 프롬프트를 강화했습니다.)
   */
  @Cacheable(value = OpenAiCacheConfig.TRAINING_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
  public Mono<Set<String>> recommendTrainingCourse(TrainingRecommendationRequest request) {
    if (request == null || request.userProfile() == null || request.trainingCourses() == null) {
      return Mono.error(new IllegalArgumentException("요청 정보가 올바르지 않습니다."));
    }

    String systemPrompt = """
        당신은 사용자의 프로필과 희망 직무를 분석하여, 역량 강화를 위한 최적의 훈련과정을 추천하는 AI입니다.
        
        [규칙]
        1. 당신은 **반드시** 입력으로 주어진 [훈련과정 리스트] 내에 존재하는 과정 중에서만 추천해야 합니다.
        2. 사용자의 현재 보유 역량(기술, 자격증)과 희망 직무(desiredJob, NCS 코드)를 비교하여, 부족한 부분을 채워줄 수 있는 과정을 선택합니다.
        3. 사용자의 주소(address)를 참고하여 지역적으로 수강 가능한 훈련과정을 우선적으로 고려해야 합니다.
        4. 이미 보유한 역량과 중복되는 과정은 추천하지 않습니다.
        
        [출력 형식]
        - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
        - 예시: ["T123456", "T654321"]
        - 설명이나 다른 텍스트는 절대 포함하지 마세요.
        """;

    String userPrompt;
    try {
      GptUserPromptRequest promptRequest = new GptUserPromptRequest(
              GptUserProfileDto.from(request.userProfile()),
              request.address(),
              request.trainingCourses()
      );
      userPrompt = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(promptRequest);

    } catch (JsonProcessingException e) {
      log.error("User Prompt JSON 직렬화 실패", e);
      return Mono.error(new RuntimeException("요청 직렬화 실패", e));
    }

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
    );

    return openAiClient.generateChatCompletion(messages)
            .flatMap(response -> {
              try {
                Set<String> ids = objectMapper.readValue(response, new TypeReference<Set<String>>() {});
                return Mono.just(ids);
              } catch (JsonProcessingException e) {
                log.error("GPT 응답 파싱 실패: {}", response, e);
                return Mono.error(new RuntimeException("GPT 응답 파싱 오류", e));
              }
            });
  }

  /**
   * 사용자의 상세 정보를 바탕으로 커리어넷 API 검색에 적합한 분류 코드를 추천합니다.
   * (AI가 주어진 목록 내에서만 응답하도록 프롬프트를 강화했습니다.)
   */
  @Cacheable(value = OpenAiCacheConfig.SEARCH_CODES_CACHE, keyGenerator = "openAiCacheKeyGenerator")
  public Mono<Map<String, String>> recommendSearchCodes(Profile profile) {
    String jobInfoCodesJson = careerNetCodeProvider.getJobInfoCodesJson();
    String encyclopediaCodesJson = careerNetCodeProvider.getEncyclopediaCodesJson();

    String userContext = """
        - 보유 기술: %s
        - 보유 자격증: %s
        - 보유 NCS 역량 : %s
        - 희망 직무 NCS 역량 : %s
        - 이력서 내용:
        %s
    """.formatted(
            profile.getProfileSkills().stream().map(ps -> ps.getSkill().getName()).collect(Collectors.joining(", ")),
            profile.getResume() != null ? profile.getResume().getCertificates().stream().map(rc -> rc.getCertificate().getJmfldnm()).collect(Collectors.joining(", ")) : "",
            profile.getUserCapabilities().stream().map(NcsOccupation::getDutyNm).collect(Collectors.joining(", ")),
            profile.getDesiredCapabilities().stream().map(NcsOccupation::getDutyNm).collect(Collectors.joining(", ")),
            resumeToText(profile.getResume())
    );

    String systemPrompt = """
        당신은 사용자의 프로필을 분석하여 커리어넷 API 검색에 가장 적합한 분류 코드를 추천하는 고도로 정확한 AI입니다.
        
        [규칙]
        1. 당신은 **반드시** [사용자 정보]만을 근거로 판단해야 합니다.
        2. 당신은 **반드시** [선택 가능한 코드 목록]에 명시된 코드 중에서만 각 분류별로 가장 적합한 코드 **하나만**을 선택해야 합니다.
        
        [출력 형식]
        - 반드시 아래 JSON 형식으로만 응답해야 합니다.
        - 설명, ملاحظات, 마크다운 기호("```json") 등 다른 어떤 텍스트도 포함해서는 안 됩니다.
        
        {
          "jobInfoCategoryCode": "...",
          "jobInfoAbilityCode": "...",
          "encyclopediaThemeCode": "..."
        }
        """;

    String userPrompt = """
          [사용자 정보]
          %s
          
          [선택 가능한 '직업 정보' 및 '직업 능력' 코드 목록]
          %s
          
          [선택 가능한 '직업 백과' 테마 목록]
          %s
          """.formatted(userContext, jobInfoCodesJson, encyclopediaCodesJson);

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
    );

    return openAiClient.generateChatCompletion(messages)
            .flatMap(resp -> {
              try {
                String raw = resp.trim();
                if (raw.startsWith("```")) {
                  raw = raw.replaceAll("```json", "").replaceAll("```", "").trim();
                }
                Map<String, String> codes = objectMapper.readValue(raw, new TypeReference<>() {});
                return Mono.just(Objects.requireNonNullElse(codes, Collections.emptyMap()));
              } catch (JsonProcessingException e) {
                log.error("GPT 응답 JSON 파싱 실패: {}", resp, e);
                return Mono.error(new IllegalStateException("GPT 응답 파싱 실패", e));
              }
            });
  }

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
            resumeToText(profile.getResume())
    );

    // 1단계: NCS 코드 추천
    return getNcsCodesFromAssistant(initialPrompt)
            .flatMap(recommendedCodes -> {
              if (recommendedCodes.isEmpty()) {
                return Mono.just(Collections.emptySet());
              }
              // 2단계: 추천된 NCS 코드 검증
              return validateNcsCodesWithAssistant(profile, recommendedCodes)
                      .map(validatedCodes -> {
                        log.info("Original recommendations: {}, Validated recommendations: {}",
                                recommendedCodes, validatedCodes);
                        return validatedCodes;
                      });
            });
  }

  @Cacheable(value = OpenAiCacheConfig.KEYWORD_GENERATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
  public Mono<Set<String>> generateKeyword(Profile profile) {
    ProfileResponse dto = ProfileResponse.from(profile);
    String userJson;
    try {
      userJson = objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      return Mono.error(new RuntimeException("사용자 JSON 직렬화 실패", e));
    }

    String systemPrompt = """
        당신은 커리어 분석 전문가입니다.
        아래 사용자 정보를 보고, 이 사람의 직무/역량에 적합한 핵심 키워드 목록을 한국어로 생성해주세요.
        
        [규칙]
        - **반드시** 주어진 사용자 정보에 명시된 내용(기술 스택, 주요 역할, 성과 등)만을 기반으로 키워드를 생성해야 합니다.
        - 언급되지 않은 내용은 절대 추론하지 마세요.
        - 최대 10개 이내로, 중복 없이 가장 핵심적인 키워드만 선택하세요.
        
        [출력 형식]
        - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
        - 예시: ["Java", "Spring Boot", "백엔드 개발", "REST API", "성능 최적화", "Docker"]
        """;

    String userPrompt = String.format("{\"user\": %s}", userJson);

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
    );

    return openAiClient.generateChatCompletion(messages)
            .flatMap(response -> {
              try {
                return Mono.just(objectMapper.readValue(response, new TypeReference<Set<String>>() {}));
              } catch (JsonProcessingException e) {
                log.error("GPT 응답 파싱 실패: {}", response, e);
                return Mono.error(new RuntimeException("GPT 응답 파싱 오류", e));
              }
            });
  }

  /**
   * AI가 추천한 NCS 코드들이 사용자의 프로필에 적합한지 검증합니다.
   * (검증 기준을 명확히 하여 AI의 오판을 줄이도록 프롬프트를 수정했습니다.)
   */
  private Mono<Set<String>> validateNcsCodesWithAssistant(Profile profile, Set<String> recommendedCodes) {
    String skillsWithProficiency = profile.getProfileSkills().stream()
            .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
            .collect(Collectors.joining(", "));
    String certificates = profile.getResume() != null ? profile.getResume().getCertificates().stream()
            .map(rc -> rc.getCertificate().getJmfldnm())
            .collect(Collectors.joining(", ")) : "";

    String validationPrompt = String.format("""
        당신은 NCS 코드 추천의 정확성을 검증하는 선임 커리어 컨설턴트입니다.
        후배가 추천한 아래 코드 목록을 사용자 정보와 **매우 엄격하게** 비교하여 검증하는 것이 당신의 임무입니다.

        [검증 기준]
        1.  **핵심 증거:** 이력서의 프로젝트 경험(예: '쇼핑몰 개발')과 역할(예: '백엔드 개발')이 검증 대상 코드와 직접적으로 일치하는가?
        2.  **기술 증거:** 사용자의 핵심 기술 스택(예: 'Java (ADVANCED)')이 검증 대상 코드의 요구 역량과 부합하는가?
        3.  **부합하지 않는 경우:** 위 기준에 하나라도 명확하게 부합하지 않으면, 그 코드는 부적합한 것으로 간주해야 합니다.

        [사용자 정보]
        - 기술스택: %s
        - 자격증: %s
        - 이력서: %s

        [검증 대상 NCS 코드 목록]
        %s

        [과업]
        - 위 [검증 대상 NCS 코드 목록] 중에서 [검증 기준]에 **완벽하게 부합하는 코드만** 골라주세요.
        - 만약 목록에 있는 코드가 모두 부적합하다면, 결과에 "REGENERATE"만 포함시켜 주세요.

        [출력 형식]
        - 반드시 아래 JSON 배열 형식 중 하나로만 응답해주세요.
        - 적합한 코드가 있을 경우: ["20010202", "20010206"]
        - 적합한 코드가 없을 경우: ["REGENERATE"]
        - 다른 설명은 절대 추가하지 마세요.
        """,
            skillsWithProficiency,
            certificates,
            resumeToText(profile.getResume()),
            recommendedCodes
    );

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
                return Mono.just(validatedCodes);
              } catch (JsonProcessingException e) {
                log.error("Failed to parse validation response: {}. Generating new recommendations.", validationResponse, e);
                return generateNewNcsCodesWithAssistant(profile, recommendedCodes);
              }
            });
  }

  /**
   * 기존 추천이 부적절할 때 새로운 NCS 코드를 생성합니다.
   * (실패 원인을 명시하고, 핵심 증거에 집중하도록 프롬프트를 수정했습니다.)
   */
  private Mono<Set<String>> generateNewNcsCodesWithAssistant(Profile profile, Set<String> rejectedCodes) {
    String skillsWithProficiency = profile.getProfileSkills().stream()
            .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
            .collect(Collectors.joining(", "));
    String certificates = profile.getResume() != null ? profile.getResume().getCertificates().stream()
            .map(rc -> rc.getCertificate().getJmfldnm())
            .collect(Collectors.joining(", ")) : "";

    String regenerationPrompt = String.format("""
        선임 컨설턴트의 검토 결과, 이전 추천이 모두 부적절하다고 판단되었습니다.
        이는 사용자의 핵심 경험을 무시하고 잘못된 추론을 했기 때문일 가능성이 높습니다.
        사용자 정보를 더 깊이, 그리고 더 정확하게 분석하여 새로운 NCS 코드를 추천해야 합니다.

        [사용자 정보]
        - 기술스택: %s
        - 자격증: %s
        - 이력서: %s

        [실패한 이전 추천 코드 (반드시 제외)]
        %s

        [새로운 추천 가이드라인]
        1. **오직 이력서에 명시된 프로젝트의 핵심 역할(예: '백엔드 개발')과 성과(예: 'API 성능 개선')에만 집중하세요.**
        2. 이 핵심 증거와 가장 직접적으로 관련된 새로운 코드 3개를 추천해주세요.
        3. 이전과 같은 실수를 반복하지 않도록, 절대 이력서에 없는 내용을 상상하지 마세요.

        [출력 형식]
        - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
        - 예시: ["20010204", "20010602"]
        - 설명은 절대 추가하지 마세요.
        """,
            skillsWithProficiency,
            certificates,
            resumeToText(profile.getResume()),
            rejectedCodes
    );

    return getNcsCodesFromAssistant(regenerationPrompt)
            .map(newCodes -> {
              newCodes.removeAll(rejectedCodes);
              log.info("Generated new NCS codes: {} (excluding rejected codes: {})", newCodes, rejectedCodes);
              return newCodes;
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

  /**
   * AI 기반 KSA 역량 분석
   * NCS KSA 항목들과 사용자 프로필을 비교하여 각 항목별 보유 수준을 평가합니다.
   *
   * @param ncsCode NCS 코드
   * @param ksaItems NCS KSA 항목 목록
   * @param profile 사용자 프로필
   * @return KSA 항목명과 평가 결과 맵
   */
  @Cacheable(value = OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
  public Mono<Map<String, KsaEvaluationResult>> analyzeKsaCompetency(
          String ncsCode,
          List<String> ksaItems,
          Profile profile
  ) {
    if (ksaItems == null || ksaItems.isEmpty()) {
      return Mono.just(Collections.emptyMap());
    }

    String skillsWithProficiency = profile.getProfileSkills().stream()
            .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
            .collect(Collectors.joining(", "));

    String certificates = profile.getResume() != null
            ? profile.getResume().getCertificates().stream()
            .map(rc -> rc.getCertificate().getJmfldnm())
            .collect(Collectors.joining(", "))
            : "";

    String resumeText = resumeToText(profile.getResume());

    String systemPrompt = """
            당신은 NCS(국가직무능력표준) 기반 역량 평가 전문가입니다.
            사용자의 이력서와 프로필을 분석하여, NCS KSA 항목별로 보유 수준을 정확하게 평가해야 합니다.

            [평가 기준]
            1. **이력서의 프로젝트 경험**이 가장 중요한 평가 근거입니다
            2. **기술 스택의 숙련도**(BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)를 반영하세요
            3. **자격증**은 보조 지표로 활용하세요
            4. 명시되지 않은 내용은 추론하지 말고, 있는 그대로 평가하세요

            [점수 산정 가이드]
            - 0.9~1.0: 해당 역량에 대한 실무 경험이 풍부하고 전문가 수준
            - 0.7~0.9: 실무 경험이 있으며 숙련된 수준
            - 0.5~0.7: 기본적인 이해와 경험이 있는 수준
            - 0.3~0.5: 이론적 지식은 있으나 실무 경험 부족
            - 0.0~0.3: 거의 경험이 없거나 관련성이 낮음

            [평가 등급]
            - EXCELLENT: 목표 수준을 초과 달성 (gap <= 0)
            - ADEQUATE: 적정 수준 (gap <= 0.1)
            - NEED_IMPROVEMENT: 개선 필요 (gap <= 0.3)
            - INSUFFICIENT: 상당한 역량 강화 필요 (gap > 0.3)
            """;

    String userPrompt = String.format("""
            [사용자 정보]
            - NCS 코드: %s
            - 보유 기술: %s
            - 보유 자격증: %s
            - 이력서:
            %s

            [평가 대상 KSA 항목 목록]
            %s

            [과업]
            위 KSA 항목 각각에 대해 사용자의 보유 수준(userScore, 0.0~1.0)을 평가하고,
            목표 수준(targetScore)은 0.8로 설정한 후,
            각 항목별로 다음 정보를 제공하세요:
            1. userScore: 사용자 보유 수준 (0.0~1.0)
            2. levelAssessment: 평가 등급 (EXCELLENT, ADEQUATE, NEED_IMPROVEMENT, INSUFFICIENT)
            3. gapDescription: 갭에 대한 간단한 설명 (1문장)
            4. recommendation: 역량 강화 방안 (1문장)

            [출력 형식]
            반드시 아래 JSON 형식으로만 응답해주세요. 마크다운 코드 블록이나 설명 없이 순수 JSON만 출력하세요.

            {
              "ksaScores": {
                "KSA항목명1": {
                  "userScore": 0.75,
                  "levelAssessment": "ADEQUATE",
                  "gapDescription": "기본적인 역량은 보유하고 있으나 심화 학습이 필요합니다",
                  "recommendation": "관련 프로젝트 경험을 더 쌓으세요"
                },
                "KSA항목명2": { ... }
              }
            }
            """,
            ncsCode,
            skillsWithProficiency,
            certificates,
            resumeText,
            String.join("\n", ksaItems)
    );

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
    );

    return openAiClient.generateChatCompletion(messages)
            .flatMap(response -> {
              try {
                // JSON 파싱
                String cleanedResponse = response.trim();
                if (cleanedResponse.startsWith("```")) {
                  cleanedResponse = cleanedResponse
                          .replaceAll("```json", "")
                          .replaceAll("```", "")
                          .trim();
                }

                Map<String, Object> parsed = objectMapper.readValue(
                        cleanedResponse,
                        new TypeReference<>() {}
                );

                Map<String, Map<String, Object>> ksaScores =
                        (Map<String, Map<String, Object>>) parsed.get("ksaScores");

                if (ksaScores == null) {
                  log.warn("AI response does not contain ksaScores");
                  return Mono.just(Collections.emptyMap());
                }

                // Map<String, KsaEvaluationResult>로 변환
                Map<String, KsaEvaluationResult> results = new HashMap<>();
                for (Map.Entry<String, Map<String, Object>> entry : ksaScores.entrySet()) {
                  Map<String, Object> scoreData = entry.getValue();
                  KsaEvaluationResult result = new KsaEvaluationResult(
                          ((Number) scoreData.get("userScore")).doubleValue(),
                          (String) scoreData.get("levelAssessment"),
                          (String) scoreData.get("gapDescription"),
                          (String) scoreData.get("recommendation")
                  );
                  results.put(entry.getKey(), result);
                }

                return Mono.just(results);

              } catch (Exception e) {
                log.error("Failed to parse KSA analysis response: {}", response, e);
                return Mono.just(Collections.emptyMap());
              }
            });
  }

  /**
   * AI 기반 NCS 적합도 신뢰도 평가
   * 사용자 프로필과 NCS 직무/능력단위 간의 매칭 적합도를 AI로 평가합니다.
   *
   * @param ncsCode NCS 코드
   * @param ncsName NCS 직무명
   * @param compUnitNames 능력단위 목록
   * @param profile 사용자 프로필
   * @return 신뢰도 평가 결과 (0.0~1.0)
   */
  @Cacheable(value = OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
  public Mono<NcsConfidenceEvaluation> evaluateNcsMatchConfidence(
          String ncsCode,
          String ncsName,
          List<String> compUnitNames,
          Profile profile
  ) {
    String skillsWithProficiency = profile.getProfileSkills().stream()
            .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
            .collect(Collectors.joining(", "));

    String certificates = profile.getResume() != null
            ? profile.getResume().getCertificates().stream()
            .map(rc -> rc.getCertificate().getJmfldnm())
            .collect(Collectors.joining(", "))
            : "";

    String resumeText = resumeToText(profile.getResume());

    String systemPrompt = """
            당신은 NCS(국가직무능력표준) 기반 직무 적합도를 평가하는 전문 커리어 컨설턴트입니다.
            사용자의 이력서와 프로필을 분석하여, 특정 NCS 직무에 대한 적합도 신뢰도를 정확하게 평가해야 합니다.

            [평가 기준]
            1. **프로젝트 경험의 직접적 관련성** (가중치 40%)
               - 이력서의 프로젝트가 NCS 직무와 얼마나 직접적으로 연관되는가?
               - 프로젝트 역할이 NCS 직무 수행과 유사한가?

            2. **기술 스택의 적합성** (가중치 30%)
               - 보유 기술이 NCS 능력단위 요구사항과 일치하는가?
               - 기술 숙련도가 직무 수행에 충분한가?

            3. **경력 수준의 적절성** (가중치 20%)
               - 프로젝트 경험 기간과 역할이 해당 직무 수준과 맞는가?

            4. **자격증 보유** (가중치 10%)
               - NCS 직무와 관련된 자격증을 보유하고 있는가?

            [신뢰도 점수 가이드]
            - 0.9~1.0: 매우 높은 적합도 - 프로젝트 경험과 기술이 직무 요구사항과 거의 완벽하게 일치
            - 0.8~0.9: 높은 적합도 - 핵심 경험과 기술이 충분하며 직무 수행 가능
            - 0.7~0.8: 적정 적합도 - 기본 경험과 기술은 있으나 일부 보완 필요
            - 0.6~0.7: 낮은 적합도 - 관련 경험은 있으나 직무 수행에 추가 학습 필요
            - 0.0~0.6: 부적합 - 직무와의 연관성이 낮거나 경험 부족
            """;

    String userPrompt = String.format("""
            [평가 대상 NCS 직무]
            - NCS 코드: %s
            - 직무명: %s
            - 능력단위 목록:
            %s

            [사용자 정보]
            - 보유 기술: %s
            - 보유 자격증: %s
            - 이력서:
            %s

            [과업]
            위 [평가 기준]을 바탕으로 사용자가 해당 NCS 직무에 얼마나 적합한지 평가하고,
            다음 정보를 제공하세요:

            1. confidenceScore: 적합도 신뢰도 점수 (0.0~1.0)
            2. matchLevel: 적합 수준 (EXCELLENT, HIGH, ADEQUATE, LOW, POOR)
            3. keyStrengths: 주요 강점 (최대 3개, 문장 배열)
            4. keyWeaknesses: 주요 약점 (최대 3개, 문장 배열)
            5. reasoning: 평가 근거 (2-3문장)

            [출력 형식]
            반드시 아래 JSON 형식으로만 응답해주세요. 마크다운 코드 블록이나 설명 없이 순수 JSON만 출력하세요.

            {
              "confidenceScore": 0.85,
              "matchLevel": "HIGH",
              "keyStrengths": [
                "백엔드 개발 프로젝트 경험이 풍부함",
                "Spring Boot 기술 스택이 직무 요구사항과 일치",
                "팀 리더 역할 수행 경험 보유"
              ],
              "keyWeaknesses": [
                "클라우드 인프라 경험 부족",
                "대규모 시스템 설계 경험 필요"
              ],
              "reasoning": "사용자는 해당 NCS 직무에 필요한 핵심 백엔드 개발 경험과 기술 스택을 충분히 보유하고 있습니다. 다만 일부 능력단위에서 요구하는 클라우드 및 대규모 시스템 경험이 다소 부족하여 추가 학습이 권장됩니다."
            }
            """,
            ncsCode,
            ncsName,
            String.join("\n", compUnitNames.stream().map(name -> "  - " + name).toList()),
            skillsWithProficiency,
            certificates,
            resumeText
    );

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
    );

    return openAiClient.generateChatCompletion(messages)
            .flatMap(response -> {
              try {
                // JSON 파싱
                String cleanedResponse = response.trim();
                if (cleanedResponse.startsWith("```")) {
                  cleanedResponse = cleanedResponse
                          .replaceAll("```json", "")
                          .replaceAll("```", "")
                          .trim();
                }

                Map<String, Object> parsed = objectMapper.readValue(
                        cleanedResponse,
                        new TypeReference<>() {}
                );

                NcsConfidenceEvaluation evaluation = new NcsConfidenceEvaluation(
                        ((Number) parsed.get("confidenceScore")).doubleValue(),
                        (String) parsed.get("matchLevel"),
                        (List<String>) parsed.get("keyStrengths"),
                        (List<String>) parsed.get("keyWeaknesses"),
                        (String) parsed.get("reasoning")
                );

                return Mono.just(evaluation);

              } catch (Exception e) {
                log.error("Failed to parse NCS confidence evaluation response: {}", response, e);
                // Fallback: 기본 신뢰도 반환
                return Mono.just(new NcsConfidenceEvaluation(
                        0.7,
                        "ADEQUATE",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        "AI 평가 실패로 기본 신뢰도를 반환합니다."
                ));
              }
            });
  }

  /**
   * NCS 적합도 신뢰도 평가 결과
   */
  public record NcsConfidenceEvaluation(
          double confidenceScore,
          String matchLevel,
          List<String> keyStrengths,
          List<String> keyWeaknesses,
          String reasoning
  ) {}

  /**
   * KSA 평가 결과
   */
  public record KsaEvaluationResult(
          double userScore,
          String levelAssessment,
          String gapDescription,
          String recommendation
  ) {}

  // --- Helper Methods ---

  public String resumeToText(Resume resume) {
    if (resume == null) return "";
    StringBuilder sb = new StringBuilder();

    if (resume.getIntroduction() != null) {
      Introduction intro = resume.getIntroduction();

      // 각 항목(성장과정, 강점, 학교생활, 지원동기)의 내용이 비어있지 않은 경우에만 추가합니다.
      if (intro.getGrowthProcess() != null && !intro.getGrowthProcess().isBlank()) {
        sb.append("성장과정: ").append(intro.getGrowthProcess()).append("\n\n");
      }

      if (intro.getStrengths() != null && !intro.getStrengths().isBlank()) {
        sb.append("장점 및 강점: ").append(intro.getStrengths()).append("\n\n");
      }

      if (intro.getSchoolLife() != null && !intro.getSchoolLife().isBlank()) {
        sb.append("학교생활: ").append(intro.getSchoolLife()).append("\n\n");
      }

      if (intro.getMotivation() != null && !intro.getMotivation().isBlank()) {
        sb.append("지원동기: ").append(intro.getMotivation()).append("\n\n");
      }
    }
    if (resume.getEducation() != null) {
      Education edu = resume.getEducation();
      sb.append("학력: ").append(edu.getSchool()).append(" ").append(edu.getMajor()).append(" ").append(edu.getGpa()).append(" (").append(edu.getStatus()).append(")\n")
              .append("  기간: ").append(formatPeriod(edu.getPeriod())).append("\n\n");
    }
    if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
      sb.append("프로젝트:\n");
      for (Project p : resume.getProjects()) {
        String techStackString = p.getTechStack().stream().map(Skill::getName).collect(Collectors.joining(", "));
        sb.append("- ").append(p.getName()).append(" (").append(formatPeriod(p.getPeriod())).append(")\n")
                .append("  역할: ").append(p.getRole()).append("\n")
                .append("  설명: ").append(p.getDescription()).append("\n")
                .append("  기술스택: ").append(techStackString).append("\n");
        if (p.getAchievements() != null && !p.getAchievements().isEmpty()) {
          sb.append("  주요 성과:\n");
          p.getAchievements().forEach(ach -> sb.append("    - ").append(ach).append("\n"));
        }
        if (p.getUrl() != null && !p.getUrl().isBlank()) {
          sb.append("  URL: ").append(p.getUrl()).append("\n");
        }
      }
      sb.append("\n");
    }
    if (resume.getActivities() != null && !resume.getActivities().isEmpty()) {
      sb.append("대외활동:\n");
      for (Activity a : resume.getActivities()) {
        sb.append("- ").append(a.getTitle()).append(" (").append(a.getOrganization()).append(")\n")
                .append("  기간: ").append(formatPeriod(a.getPeriod())).append("\n")
                .append("  내용: ").append(a.getDescription()).append("\n");
      }
    }
    return sb.toString().trim();
  }

  private String formatPeriod(Period period) {
    if (period == null || period.getStartDate() == null) return "";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM");
    String startDate = period.getStartDate().format(formatter);
    if (period.getEndDate() == null) return startDate + " - 진행 중";
    String endDate = period.getEndDate().format(formatter);
    return startDate + " - " + endDate;
  }
}