package com.shingu.roadmap.apis.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.careernet.service.CareerNetCodeProvider;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.dto.request.GptUserProfileDto;
import com.shingu.roadmap.apis.openai.dto.request.GptUserPromptRequest;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.apis.openai.validation.OpenAiInputValidator;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import com.shingu.roadmap.apis.openai.error.OpenAiErrorHandler;
import com.shingu.roadmap.apis.openai.cache.OpenAiCacheService;
import com.shingu.roadmap.apis.openai.metrics.OpenAiMetricsCollector;
import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.resume.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

  private final OpenAiClient openAiClient;
  private final ObjectMapper objectMapper;
  private final CareerNetCodeProvider careerNetCodeProvider;
  private final OpenAiInputValidator inputValidator;
  private final SecureLogger secureLogger;
  private final OpenAiErrorHandler errorHandler;
  private final OpenAiCacheService cacheService;
  private final OpenAiMetricsCollector metricsCollector;

  // ─────────────────────────────────────────────────────────────────────────────
  // 훈련과정 추천 (Assistants v2 경로)
  // ─────────────────────────────────────────────────────────────────────────────
  public Mono<Set<String>> recommendTrainingCourse(TrainingRecommendationRequest request) {
    String sessionKey = "training-reco:" + System.currentTimeMillis();
    secureLogger.logApiCall(sessionKey, "recommendTrainingCourse", 0);

    // 입력 검증
    if (request == null || request.userProfile() == null || request.trainingCourses() == null) {
      secureLogger.logValidationFailure(sessionKey, "REQUEST_VALIDATION", "Missing required fields");
      return Mono.error(new IllegalArgumentException("요청 정보가 올바르지 않습니다."));
    }

    if (request.trainingCourses().isEmpty()) {
      secureLogger.logValidationFailure(sessionKey, "TRAINING_COURSES", "No training courses provided");
      return Mono.error(new IllegalArgumentException("훈련과정 목록이 비어있습니다."));
    }

    if (request.trainingCourses().size() > 1000) {
      secureLogger.logValidationFailure(sessionKey, "TRAINING_COURSES", "Too many training courses");
      return Mono.error(new IllegalArgumentException("훈련과정이 너무 많습니다. (최대 1000개)"));
    }

    final String systemPrompt = """
        당신은 사용자의 희망 직무에 따라 부족한 역량을 보완할 수 있는 훈련과정을 추천하는 AI입니다.
        입력받은 사용자 정보(skills, certificates, desiredJob, ncsCodes)와 훈련과정 리스트를 기반으로, 
        사용자가 아직 보유하지 않은 기술이나 자격증을 학습할 수 있는 훈련과정 중 가장 적합한 5개를 골라주세요.

        선정 기준:
        - 희망 직무(desiredJob) 또는 NCS 코드에서 요구되는 역량 중, 사용자가 아직 갖추지 못한 기술/자격증과 관련된 과정
        - address를 참조해서 지역적으로 가능한 훈련과정을 추천
        - 훈련 제목 또는 내용에 부족한 역량이 언급되어야 함
        - 이미 보유한 역량과 중복되지 않는 과정을 우선 고려

        ⚠️ 반환 형식: ["trprId"]  (입력 trainings 배열 안에 실제 존재하는 trprId만)
        설명은 포함하지 말고, 결과만 반환하세요.
        """;

    final String userPrompt;
    try {
      // 주소 검증
      if (request.address() != null) {
        inputValidator.validateUserInput(request.address());
      }

      GptUserPromptRequest promptRequest = new GptUserPromptRequest(
              GptUserProfileDto.from(request.userProfile()),
              request.address(),
              request.trainingCourses()
      );
      userPrompt = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(promptRequest);

      // 프롬프트 길이 검증
      inputValidator.validatePromptLength(systemPrompt, userPrompt);

    } catch (JsonProcessingException e) {
      secureLogger.logApiError(sessionKey, "recommendTrainingCourse", "JSON_SERIALIZATION", e.getMessage());
      return Mono.error(new RuntimeException("요청 직렬화 실패", e));
    } catch (Exception e) {
      secureLogger.logValidationFailure(sessionKey, "PROMPT_VALIDATION", e.getMessage());
      return Mono.error(e);
    }

    final String finalSessionKey = sessionKey;
    long startTime = System.currentTimeMillis();
    String cacheKey = cacheService.generateCacheKey("training_course", request);

    return cacheService.getCachedResponse("training_course", cacheKey,
            () -> callAssistant(finalSessionKey, systemPrompt, userPrompt),
            cacheService.isCacheable("training_course", cacheKey))
            .map(this::stripCodeFence)
            .map(resp -> {
              try {
                secureLogger.logResponseSummary(finalSessionKey, "recommendTrainingCourse", resp);
                Set<String> result = objectMapper.readValue(resp, new TypeReference<Set<String>>() {});
                metricsCollector.recordSuccess("recommendTrainingCourse", null, 0, System.currentTimeMillis() - startTime);
                return result;
              } catch (JsonProcessingException e) {
                secureLogger.logApiError(finalSessionKey, "recommendTrainingCourse",
                                       "JSON_PARSING", "Failed to parse response: " + resp);
                metricsCollector.recordError("recommendTrainingCourse", null, "JSON_PARSING", e.getMessage());
                throw new RuntimeException("GPT 응답 파싱 오류", e);
              }
            })
            .doOnSuccess(result -> {
              long duration = System.currentTimeMillis() - startTime;
              secureLogger.logApiResponse(finalSessionKey, result != null ? result.size() : 0, duration);
              secureLogger.logPerformanceMetric("recommendTrainingCourse", duration, 0);
            })
            .doOnError(error -> {
              long duration = System.currentTimeMillis() - startTime;
              secureLogger.logApiError(finalSessionKey, "recommendTrainingCourse",
                                     errorHandler.classifyError(error).name(), error.getMessage());
              metricsCollector.recordError("recommendTrainingCourse", null,
                                         errorHandler.classifyError(error).name(), error.getMessage());
            });
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // 커리어넷 검색 코드 추천 (Assistants v2 경로)
  // ─────────────────────────────────────────────────────────────────────────────
  public Mono<Map<String, String>> recommendSearchCodes(Profile profile) {
    final String jobInfoCodesJson = careerNetCodeProvider.getJobInfoCodesJson();
    final String encyclopediaCodesJson = careerNetCodeProvider.getEncyclopediaCodesJson();

    final String userContext = """
        - 보유 기술: %s
        - 보유 자격증: %s
        - 보유 NCS 역량 : %s
        - 희망 직무 NCS 역량 : %s
        - 이력서 내용:
        %s
        """.formatted(
            profile.getProfileSkills().stream().map(ps -> ps.getSkill().getName()).collect(Collectors.joining(", ")),
            profile.getProfileCertificates().stream().map(pc -> pc.getCertificate().getJmfldnm()).collect(Collectors.joining(", ")),
            profile.getUserCapabilities().stream().map(NcsOccupation::getDutyNm).collect(Collectors.joining(", ")),
            profile.getDesiredCapabilities().stream().map(NcsOccupation::getDutyNm).collect(Collectors.joining(", ")),
            resumeToText(profile.getResume())
    );

    final String systemPrompt = """
        당신은 사용자의 프로필을 분석하여 커리어넷 API 검색에 가장 적합한 분류 코드를 추천하는 전문가입니다.
        사용자 정보와 선택 가능한 코드 목록을 기반으로, 각 분류에서 가장 관련성이 높은 코드 **하나씩만** 골라주세요.
        결과는 꼭 아래 JSON 형식으로만 반환하세요(설명/마크다운 금지).

        {
          "jobInfoCategoryCode": "...",
          "jobInfoAbilityCode": "...",
          "encyclopediaThemeCode": "..."
        }
        """;

    final String userPrompt = """
        [사용자 정보]
        %s

        [선택 가능한 '직업 정보' 코드 목록(jobCategories)]
        %s

        [선택 가능한 '직업 능력' 코드 목록(abilities)]
        %s

        [선택 가능한 '직업 백과' 테마 목록(themes)]
        %s
        """.formatted(userContext, jobInfoCodesJson, jobInfoCodesJson, encyclopediaCodesJson);

    final String sessionKey = "careernet-codes:" + Optional.ofNullable(profile.getId()).orElse(0L);

    return callAssistant(sessionKey, systemPrompt, userPrompt)
            .map(this::stripCodeFence)
            .flatMap(resp -> {
              try {
                Map<String, String> codes = objectMapper.readValue(resp, new TypeReference<Map<String,String>>(){});
                return Mono.just(codes);
              } catch (JsonProcessingException e) {
                log.error("GPT 응답 JSON 파싱 실패: {}", resp, e);
                return Mono.error(new IllegalStateException("GPT 응답 파싱 실패", e));
              }
            });
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // NCS 코드 추천 (희망 직무 텍스트만)
  // ─────────────────────────────────────────────────────────────────────────────
  public Mono<Set<String>> recommendDesiredJobCodeUsingAssistant(String desiredJob) {
    final String userPrompt = "희망 직무: [" + desiredJob + "] 에 적합한 NCS 직무 코드를 추천해줘. 결과는 코드만 콤마(,)로 나열해줘.";
    return getNcsCodesFromAssistant("ncs-by-desiredjob:" + desiredJob, userPrompt);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // NCS 코드 추천 (Profile 기반, 숙련도 반영)
  // ─────────────────────────────────────────────────────────────────────────────
  public Mono<Set<String>> recommendNcsCodeUsingAssistant(Profile profile) {
    final String skillsWithProficiency = profile.getProfileSkills().stream()
            .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
            .collect(Collectors.joining(", "));

    final String certificates = profile.getProfileCertificates().stream()
            .map(pc -> pc.getCertificate().getJmfldnm())
            .collect(Collectors.joining(", "));

    final String userPrompt = """
        기술스택: [%s], 자격증: [%s], 이력서: [%s] 에 적합한 NCS 직무 코드를 추천해줘.
        기술스택은 **숙련도**를 참고해서 더 정확하게 추천해줘.
        결과는 코드만 콤마(,)로 나열해줘.
        """.formatted(
            skillsWithProficiency,
            certificates,
            resumeToText(profile.getResume())
    );

    final String sessionKey = "ncs-by-profile:" + Optional.ofNullable(profile.getId()).orElse(0L);
    return getNcsCodesFromAssistant(sessionKey, userPrompt);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // 키워드 생성
  // ─────────────────────────────────────────────────────────────────────────────
  public Mono<Set<String>> generateKeyword(Profile profile) {
    final ProfileResponse dto = ProfileResponse.from(profile);
    final String userJson;
    try {
      userJson = objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("사용자 JSON 직렬화 실패", e);
    }

    final String systemPrompt = """
        너는 커리어 분석 전문가야.
        아래 사용자 정보를 보고, 이 사람의 직무/역량에 적합한 핵심 키워드 목록을 한국어로 뽑아줘.
        최대 10개 이내로, 중복 없이 Set<String> 배열 형식으로 반환해. 예: ["데이터 분석", "Spring Boot", "REST API"]
        """;

    final String userPrompt = """
        {
          "user": %s
        }
        """.formatted(userJson);

    final String sessionKey = "keywords:" + Optional.ofNullable(profile.getId()).orElse(0L);

    return callAssistant(sessionKey, systemPrompt, userPrompt)
            .map(this::stripCodeFence)
            .map(resp -> {
              try {
                return objectMapper.readValue(resp, new TypeReference<Set<String>>() {});
              } catch (JsonProcessingException e) {
                log.error("GPT 응답 파싱 실패: {}", resp);
                throw new RuntimeException("GPT 응답 파싱 오류", e);
              }
            });
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // 공통 유틸/헬퍼
  // ─────────────────────────────────────────────────────────────────────────────

  /** Assistants v2 호출 – system+user 프롬프트를 하나로 합쳐 전달 */
  private Mono<String> callAssistant(String sessionKey, String systemPrompt, String userPrompt) {
    try {
      // 프롬프트 검증
      inputValidator.validateUserInput(systemPrompt);
      inputValidator.validateUserInput(userPrompt);
      inputValidator.validatePromptLength(systemPrompt, userPrompt);

      final String input = """
          [SYSTEM]
          %s

          [USER]
          %s
          """.formatted(systemPrompt, userPrompt).trim();

      secureLogger.logPromptSummary(sessionKey, "callAssistant",
                                  String.format("SystemPrompt: %d chars, UserPrompt: %d chars",
                                               systemPrompt.length(), userPrompt.length()));

      return openAiClient.askAssistant(sessionKey, input);

    } catch (Exception e) {
      secureLogger.logValidationFailure(sessionKey, "ASSISTANT_CALL", e.getMessage());
      return Mono.error(e);
    }
  }

  /** NCS 코드 추출 호출 헬퍼 */
  private Mono<Set<String>> getNcsCodesFromAssistant(String sessionKey, String userPrompt) {
    return openAiClient.askAssistant(sessionKey, userPrompt)
            .map(this::stripCodeFence)
            .map(this::extractValidNcsCodes)
            .map(HashSet::new);
  }

  /** 응답이 ```json ... ``` 등 코드펜스로 감싸져 있으면 제거 */
  private String stripCodeFence(String raw) {
    if (raw == null) return "";
    String trimmed = raw.trim();
    if (trimmed.startsWith("```")) {
      // ```json ... ``` 또는 ``` ... ```
      trimmed = trimmed.replaceFirst("^```json\\s*", "");
      trimmed = trimmed.replaceFirst("^```\\s*", "");
      if (trimmed.endsWith("```")) {
        trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
      }
    }
    return trimmed;
  }

  /** 문자열에서 8자리 숫자(NCS 코드)만 뽑기 */
  public List<String> extractValidNcsCodes(String raw) {
    List<String> result = new ArrayList<>();
    if (raw == null) return result;
    Matcher matcher = Pattern.compile("\\b\\d{8}\\b").matcher(raw);
    while (matcher.find()) {
      result.add(matcher.group());
    }
    return result;
  }

  public String resumeToText(Resume resume) {
    if (resume == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();

    // Introduction
    if (resume.getIntroduction() != null && resume.getIntroduction().getContent() != null) {
      sb.append("자기소개: ").append(resume.getIntroduction().getContent()).append("\n\n");
    }

    // Education
    if (resume.getEducation() != null) {
      Education edu = resume.getEducation();
      sb.append("학력: ")
              .append(edu.getSchool()).append(" ")
              .append(edu.getMajor()).append(" (")
              .append(edu.getStatus()).append(")\n")
              .append("  기간: ").append(formatPeriod(edu.getPeriod())).append("\n\n");
    }

    // Projects
    if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
      sb.append("프로젝트:\n");
      for (Project p : resume.getProjects()) {
        String techStackString = p.getTechStack().stream()
                .map(Skill::getName)
                .collect(Collectors.joining(", "));

        sb.append("- ").append(p.getName()).append(" (").append(formatPeriod(p.getPeriod())).append(")\n");
        sb.append("  역할: ").append(p.getRole()).append("\n");
        sb.append("  설명: ").append(p.getDescription()).append("\n");
        sb.append("  기술스택: ").append(techStackString).append("\n");

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

    // Activities
    if (resume.getActivities() != null && !resume.getActivities().isEmpty()) {
      sb.append("대외활동:\n");
      for (Activity a : resume.getActivities()) {
        sb.append("- ")
                .append(a.getTitle()).append(" (").append(a.getOrganization()).append(")\n")
                .append("  기간: ").append(formatPeriod(a.getPeriod())).append("\n")
                .append("  내용: ").append(a.getDescription()).append("\n");
      }
    }

    return sb.toString().trim();
  }

  private String formatPeriod(Period period) {
    if (period == null || period.getStartDate() == null) {
      return "";
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM");
    String startDate = period.getStartDate().format(formatter);
    if (period.getEndDate() == null) {
      return startDate + " - 진행 중";
    }
    String endDate = period.getEndDate().format(formatter);
    return startDate + " - " + endDate;
  }
}