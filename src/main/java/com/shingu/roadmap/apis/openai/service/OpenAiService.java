package com.shingu.roadmap.apis.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
   * @param request 사용자 정보 및 훈련과정 리스트
   * @return 추천된 NCS 코드 목록 (Set)
   */
  public Mono<Set<String>> recommendTrainingCourse(TrainingRecommendationRequest request) {
    String systemPrompt = """
        당신은 사용자의 희망 직무에 따라 부족한 역량을 보완할 수 있는 훈련과정을 추천하는 AI입니다.
        
        입력받은 사용자 정보(skills, certificates, desiredJob, ncsCodes)와 훈련과정 리스트를 기반으로, 사용자가 아직 보유하지 않은 기술이나 자격증을 학습할 수 있는 훈련과정 중 가장 적합한 5개를 골라주세요.
        
        선정 기준은 다음과 같습니다:
        - 희망 직무(desiredJob) 또는 NCS 코드에서 요구되는 역량 중, 사용자가 아직 갖추지 못한 기술(skill), 자격증(certificates)과 관련된 과정
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

      String userJson = prettyWriter.writeValueAsString(request.userProfile());
      String trainingsJson = prettyWriter.writeValueAsString(request.trainingCourses());

      userPrompt = """
                  {
                    "user": %s,
                    "trainings": %s
                  }
                  """.formatted(userJson, trainingsJson);
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

  public Mono<Set<String>> reassessNcsCodes(Member member, Set<NcsOccupation> currentNcsCodes) {

    String skills = String.join(", ", member.getSkills());
    String certificates = String.join(", ", member.getCertificates());

    String userPrompt = String.format("""
    사용자의 기술 스택은 [%s]이고,
    자격증은 [%s]입니다.
    현재 추천된 NCS 코드는 다음과 같습니다:
    %s
    이 정보가 적합한지 검토하고, 필요시 더 적합한 NCS 코드를 콤마(,)로 반환해주세요.
    """,
            skills,
            certificates,
            currentNcsCodes.stream()
                    .map(code -> String.format("[%s: \"%s\"]", code.getDutyCd(), code.getDutyNm()))
                    .collect(Collectors.joining("\n"))
    );

    String systemPrompt = """
        NCS 직무 코드 추천 전문가입니다.
        기술과 자격증 정보를 기반으로 기존 NCS코드가 적절하면 그대로, 아니라면 더 적합한 코드를 반환하세요
        직접적인 상위 개념뿐만 아니라 하위 개념 코드(세분화된 직무 코드)도 함께 포함해 주세요
        반드시 결과는 코드만 콤마로 구분해 주세요. 설명은 포함하지 마세요.
        """;

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
    );

    return openAiClient.generateChatCompletion(messages)
            .map(response -> Set.of(response.split("\\s*,\\s*")));
  }

  public Mono<Set<String>> recommendNcsCodeUsingAssistant(Member member) {
    String userPrompt = String.format(
            "기술스택: [%s], 자격증: [%s] 에 적합한 NCS 직무 코드를 추천해줘. 결과는 코드만 콤마(,)로 나열해줘.",
            String.join(", ", member.getSkills()),
            String.join(", ", member.getCertificates())
    );

    return openAiClient.generateAssistantResponse(userPrompt)
            .map(this::extractValidNcsCodes) // 문자열 -> List<String>
            .map(HashSet::new); // List -> Set 변환
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
