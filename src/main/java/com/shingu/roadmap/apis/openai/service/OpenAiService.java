package com.shingu.roadmap.apis.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.careernet.service.CareerNetCodeProvider;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.dto.request.GptUserPromptRequest;
import com.shingu.roadmap.apis.openai.dto.request.GptUserProfileDto;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
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

  /**
   * 사용자 정보를 바탕으로 훈련과정을 추천합니다.
   * (이 메서드는 GptUserProfileDto를 사용하며, 해당 DTO는 이미 수정되었으므로 이 메서드는 변경할 필요가 없습니다.)
   */
  public Mono<Set<String>> recommendTrainingCourse(TrainingRecommendationRequest request) {
    // ... (기존 코드와 동일)
    if (request == null || request.userProfile() == null || request.trainingCourses() == null) {
      return Mono.error(new IllegalArgumentException("요청 정보가 올바르지 않습니다."));
    }

    String systemPrompt = """
            당신은 사용자의 희망 직무에 따라 부족한 역량을 보완할 수 있는 훈련과정을 추천하는 AI입니다.
            
            입력받은 사용자 정보(skills, certificates, desiredJob, ncsCodes)와 훈련과정 리스트를 기반으로, 사용자가 아직 보유하지 않은 기술이나 자격증을 학습할 수 있는 훈련과정 중 가장 적합한 5개를 골라주세요.
            
            선정 기준은 다음과 같습니다:
            - 희망 직무(desiredJob) 또는 NCS 코드에서 요구되는 역량 중, 사용자가 아직 갖추지 못한 기술(skill), 자격증(certificates)과 관련된 과정
            - address를 참조해서 지역적으로 가능한 훈련과정을 추천해줘야함
            - 훈련 제목 또는 내용에 부족한 역량이 언급되어야 함
            - 이미 보유한 역량과 **중복되지 않는 과정**을 우선적으로 고려하세요.
    
            ⚠️ 반환하는 "trprId"는 반드시 입력으로 주어진 trainings 배열 안에 실제 존재하는 값이어야 합니다.
            임의로 생성하거나 목록에 없는 ID를 반환하지 마세요.
            
            결과는 다음 형식으로 반환하세요: ["trprId"]
            설명은 생략하고 결과만 반환하세요.
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
            .map(response -> {
              try {
                return objectMapper.readValue(response, new TypeReference<Set<String>>() {});
              } catch (JsonProcessingException e) {
                log.error("GPT 응답 파싱 실패: {}", response);
                throw new RuntimeException("GPT 응답 파싱 오류", e);
              }
            });
  }

  /**
   * Profile 객체를 받아 사용자의 상세 정보를 바탕으로
   * 커리어넷 API 검색에 적합한 '직업 정보' 및 '직업 백과' 분류 코드를 추천합니다.
   * (이 메서드는 Profile 객체를 직접 사용하므로, Profile 구조 변경에 맞춰 수정되었습니다.)
   */
  public Mono<Map<String, String>> recommendSearchCodes(Profile profile) {
    // 1. jobInfomationSearchCode.json, jobEncyclopediaSearchCode.json 파일 내용을 읽어옵니다.
    // (실제 구현에서는 ResourceLoader를 사용해 클래스패스에서 파일을 읽어옵니다.)
    String jobInfoCodesJson = careerNetCodeProvider.getJobInfoCodesJson();
    String encyclopediaCodesJson = careerNetCodeProvider.getEncyclopediaCodesJson();

    // 2. Profile 정보를 바탕으로 AI에게 전달할 상세한 사용자 정보를 만듭니다.
    // 기존의 resumeToText, 스킬, 자격증, NCS 코드 등을 모두 활용합니다.
    String userContext = """
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

    // 3. AI에게 전달할 프롬프트를 구성합니다.
    String systemPrompt = """
        당신은 사용자의 프로필을 분석하여 커리어넷 API 검색에 가장 적합한 분류 코드를 추천하는 전문가입니다.
        사용자 정보와 선택 가능한 코드 목록을 기반으로, 각 분류에서 가장 관련성이 높은 코드 **하나만** 골라주세요.
        결과는 반드시 다음 JSON 형식으로만 반환해야 합니다. 설명은 절대 추가하지 마세요.
        "```json" 과 같은 마크다운 기호는 절대 포함하지 마세요.
        
        {
          "jobInfoCategoryCode": "...",
          "jobInfoAbilityCode": "...",
          "encyclopediaThemeCode": "..."
        }
        """;

    String userPrompt = """
          [사용자 정보]
          %s
          
          [선택 가능한 '직업 정보' 코드 목록(jobCategories)]
          %s
          
          [선택 가능한 '직업 능력' 코드 목록(abilities)]
          %s
          
          [선택 가능한 '직업 백과' 테마 목록(themes)]
          %s
          """.formatted(userContext, jobInfoCodesJson, jobInfoCodesJson, encyclopediaCodesJson);


    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
    );

    // 4. OpenAI API를 호출하고 결과를 Map으로 파싱하여 반환합니다.
    return openAiClient.generateChatCompletion(messages)
            .flatMap(resp -> {
              try {

                String raw = resp.trim();

                // GPT가 ```json ... ``` 으로 감쌌을 경우 제거
                if (raw.startsWith("```")) {
                  raw = raw.replaceAll("```json", "")
                          .replaceAll("```", "")
                          .trim();
                }

                Map<String, String> codes = objectMapper.readValue(resp, new TypeReference<Map<String,String>>(){});
                // optional: 방어적 널 처리
                return Mono.just(codes);
              } catch (JsonProcessingException e) {
                log.error("GPT 응답 JSON 파싱 실패: {}", resp, e);
                return Mono.error(new IllegalStateException("GPT 응답 파싱 실패", e));
              }
            });
  }

  public Mono<Set<String>> recommendDesiredJobCodeUsingAssistant(String desiredJob) {
    String userPrompt = String.format(
            "희망 직무: [%s] 에 적합한 NCS 직무 코드를 추천해줘. 결과는 코드만 콤마(,)로 나열해줘.",
            desiredJob
    );
    return getNcsCodesFromAssistant(userPrompt);
  }

  /**
   * 메서드 시그니처 및 내부 로직 변경
   * 개별 정보 대신 Profile 객체를 직접 받아 숙련도를 포함한 프롬프트를 생성합니다.
   */
  public Mono<Set<String>> recommendNcsCodeUsingAssistant(Profile profile) {
    // 기술 스택과 숙련도를 함께 문자열로 만듭니다. ex: "Java (ADVANCED), Spring (INTERMEDIATE)"
    String skillsWithProficiency = profile.getProfileSkills().stream()
            .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
            .collect(Collectors.joining(", "));

    // 자격증 정보를 문자열로 만듭니다.
    String certificates = profile.getProfileCertificates().stream()
            .map(pc -> pc.getCertificate().getJmfldnm())
            .collect(Collectors.joining(", "));

    String userPrompt = String.format(
            // 프롬프트에 숙련도를 참고하라는 내용을 추가하여 AI의 정확도를 높입니다.
            "기술스택: [%s], 자격증: [%s], 이력서: [%s] 에 적합한 NCS 직무 코드를 추천해줘. 기술스택은 숙련도를 참고해서 더 정확하게 추천해줘. 결과는 코드만 콤마(,)로 나열해줘.",
            skillsWithProficiency,
            certificates,
            resumeToText(profile.getResume())
    );
    return getNcsCodesFromAssistant(userPrompt);
  }


  public Mono<Set<String>> generateKeyword(Profile profile) {
    ProfileResponse dto = ProfileResponse.from(profile);
    String userJson;
    try {
      userJson = objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("사용자 JSON 직렬화 실패", e);
    }

    String systemPrompt = """
            너는 커리어 분석 전문가야.
            아래 사용자 정보를 보고, 이 사람의 직무/역량에 적합한 핵심 키워드 목록을 한국어로 뽑아줘.
            최대 10개 이내로, 중복 없이 Set<String> 배열 형식으로 반환해. 예: ["데이터 분석", "Spring Boot", "REST API"]
            """;

    String userPrompt = """
            {
              "user": %s
            }
            """.formatted(userJson);

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
    );

    return openAiClient.generateChatCompletion(messages)
            .map(response -> {
              try {
                return objectMapper.readValue(response, new TypeReference<Set<String>>() {});
              } catch (JsonProcessingException e) {
                log.error("GPT 응답 파싱 실패: {}", response);
                throw new RuntimeException("GPT 응답 파싱 오류", e);
              }
            });
  }

  /**
   * AI가 응답한 문자열에서 유효한 NCS 코드를 추출합니다.
   * @param raw
   * @return
   */
  public List<String> extractValidNcsCodes(String raw) {
    List<String> result = new ArrayList<>();
    // 정확히 8자리 숫자만 추출 (NCS 분류코드가 8자리라고 가정)
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

    // Introduction (자기소개)
    if (resume.getIntroduction() != null && resume.getIntroduction().getContent() != null) {
      sb.append("자기소개: ").append(resume.getIntroduction().getContent()).append("\n\n");
    }

    // Education (학력)
    if (resume.getEducation() != null) {
      Education edu = resume.getEducation();
      sb.append("학력: ")
              .append(edu.getSchool()).append(" ")
              .append(edu.getMajor()).append(" (")
              .append(edu.getStatus()).append(")\n")
              .append("  기간: ").append(formatPeriod(edu.getPeriod())).append("\n\n");
    }

    // Projects (프로젝트)
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

    // Activities (대외활동)
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

  private Mono<Set<String>> getNcsCodesFromAssistant(String userPrompt) {
    return openAiClient.generateAssistantResponse(userPrompt)
            .map(this::extractValidNcsCodes)
            .map(HashSet::new);
  }
}