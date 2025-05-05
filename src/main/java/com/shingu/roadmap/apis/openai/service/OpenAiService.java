package com.shingu.roadmap.apis.openai.service;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenAiService {

  private final OpenAiClient openAiClient;

  /**
   * 기술 스택과 자격증을 기반으로 NCS 코드 추천
   * @param member 기술 및 자격증 정보가 포함된 Member 객체
   * @return 추천된 NCS 코드 목록 (Set)
   */
  public Mono<Set<String>> recommendNcsCodes(Member member) {
    String promptText = String.format(
            "기술스택: [%s], 자격증: [%s] 에 적합한 NCS 직무 코드를 추천해줘. 결과는 코드만 콤마(,)로 나열해줘.",
            String.join(", ", member.getSkills()),
            String.join(", ", member.getCertificates())
    );

    String sytemPrompt = """
    NCS 직무 코드 추천 전문가입니다.
    기술/자격 기반으로 가장 관련 있는 NCS 코드를 반환하세요.
    상위·하위 개념 모두 고려하되, 무관한 코드는 제외하세요.
    결과는 오직 코드만, 콤마로 구분해서 반환하세요.
    """;

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", sytemPrompt),
            Map.of("role", "user", "content", promptText)
    );

    return openAiClient.generateChatCompletion(messages)
            .map(response -> Set.of(response.split("\\s*,\\s*")));
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
