package com.shingu.roadmap.apis.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  // ... (resumeToText, formatPeriod 등 나머지 헬퍼 메서드는 기존과 동일)
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