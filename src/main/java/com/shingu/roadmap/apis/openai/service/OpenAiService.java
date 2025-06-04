package com.shingu.roadmap.apis.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.dto.request.GptUserProfileDto;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

  private final OpenAiClient openAiClient;
  private final ObjectMapper objectMapper;

  /**
   * 사용자 정보를 바탕으로 훈련과정을 추천합니다.
   * @param request 사용자 정보 및 훈련과정 리스트
   * @return 추천된 NCS 코드 목록 (Set)
   */
  public Mono<Set<String>> recommendTrainingCourse(TrainingRecommendationRequest request) {

    if( request == null || request.userProfile() == null || request.trainingCourses() == null) {
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
      ObjectWriter prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();

      String userJson = String.valueOf(GptUserProfileDto.from(request.userProfile()));
      String trainingsJson = prettyWriter.writeValueAsString(request.trainingCourses());

      userPrompt = """
                  {
                    "user": %s,
                    address: %s,
                    "trainings": %s
                  }
                  """.formatted(userJson, request.address(), trainingsJson);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("요청 직렬화 실패", e);
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

    return openAiClient.generateAssistantResponse(userPrompt)
            .map(this::extractValidNcsCodes) // 문자열 -> List<String>
            .map(HashSet::new); // List -> Set 변환
  }

  public Mono<Set<String>> recommendNcsCodeUsingAssistant(Set<String> skills, Set<String> certificates) {
    String userPrompt = String.format(
            "기술스택: [%s], 자격증: [%s] 에 적합한 NCS 직무 코드를 추천해줘. 결과는 코드만 콤마(,)로 나열해줘.",
            String.join(", ", skills),
            String.join(", ", certificates)
    );

    return openAiClient.generateAssistantResponse(userPrompt)
            .map(this::extractValidNcsCodes) // 문자열 -> List<String>
            .map(HashSet::new); // List -> Set 변환
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

  public List<String> extractValidNcsCodes(String raw) {
    List<String> result = new ArrayList<>();
    Matcher matcher = Pattern.compile("\\b\\d{8}\\b").matcher(raw); // 정확히 8자리 숫자만
    while (matcher.find()) {
      result.add(matcher.group());
    }
    return result;
  }
}