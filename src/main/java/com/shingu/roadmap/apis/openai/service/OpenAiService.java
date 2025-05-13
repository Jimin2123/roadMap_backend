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

@Service
@RequiredArgsConstructor
public class OpenAiService {

  private final OpenAiClient openAiClient;

  public Mono<Set<String>> recommendDesiredJobCodeUsingAssistant(Member member) {
    String userPrompt = String.format(
            "희망직무: %s 에 적합한 NCS 직무 코드를 추천해줘. 결과는 코드만 콤마(,)로 나열해줘.",
            member.getProfile().getDesiredJob()
    );

    return openAiClient.generateAssistantResponse(userPrompt)
            .map(this::extractValidNcsCodes) // 문자열 -> List<String>
            .map(HashSet::new); // List -> Set 변환
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
