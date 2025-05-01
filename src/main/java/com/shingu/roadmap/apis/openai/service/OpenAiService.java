package com.shingu.roadmap.apis.openai.service;

import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
            "기술: %s, 자격증: %s 에 적합한 NCS 직무 코드를 추천해줘. 결과는 코드만 콤마(,)로 나열해줘.",
            String.join(", ", member.getSkills()),
            String.join(", ", member.getCertificates())
    );

    String sytemPrompt = "당신은 사용자의 기술 및 자격증을 바탕으로 적절한 NCS 직무 코드를 반환하는 AI입니다." +
            "가장 관련성 높은 코드부터 우선순위가 높은 순서로 정렬하여 반환하세요. " +
            "결과는 오직 코드만, 콤마로 구분해서 반환하세요.";

    List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", sytemPrompt),
            Map.of("role", "user", "content", promptText)
    );

    return openAiClient.generateChatCompletion(messages)
            .map(response -> Set.of(response.split("\\s*,\\s*")));
  }
}
