# 2025/05/01 ~ 2025/05/03

## 공공 포털 데이터 NCS API 사용하기

### PKIX path building failed

1. HTTPS 요청 시 인증 실패 에러 발생 해결
2. 개발환경에서만 SSL 인증문제 해결하는 코드를 추가
3. 개발환경에서만 아래와 같은 코드를 쓰고 프로덕션 코드에서는 지워야한다고 함.

```java
  @Bean
  public RestClient ncsRestClient(NcsApiProperties ncsApiProperties) {
    try {
      // 모든 인증서를 신뢰하는 TrustManager 설정
      TrustManager[] trustAllCerts = new TrustManager[]{
              new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
              }
      };

      // SSLContext 초기화
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustAllCerts, new SecureRandom());

      // HttpClient 생성
      HttpClient httpClient = HttpClient.newBuilder()
              .sslContext(sslContext)
              .build();

      // RestClient 생성
      return RestClient.builder()
              .requestFactory(new JdkClientHttpRequestFactory(httpClient))
              .baseUrl(ncsApiProperties.getBaseUrl())
              .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .build();
    }catch(Exception e) {
      throw new IllegalStateException("SSL 설정 중 오류가 발생했습니다.", e);
    }
  }
```

### SERVICE KEY IS NOT REGISTERED ERROR.[30]

1. Service Key가 2번 인코딩되어 생기는 문제해결
2. 브라우저에서는 정상 작동하는 URL이 RestClient에서는 정상 작동하지 않는 문제 해결

```java
  public NcsOccupationResponse getOccupation(String ncsCode) {
  UriComponentsBuilder builder = UriComponentsBuilder
          .fromUriString(ncsApiProperties.getBaseUrl() + "/openapi2.do")
          .queryParam("serviceKey", ncsApiProperties.getServiceKey().trim())
          .queryParam("pageNo", 1)
          .queryParam("numOfRows", 10)
          .queryParam("returnType", "JSON")
          .queryParam("dutyCd", ncsCode);

  String uri = builder.build(true).toUriString(); // <- 이 부분에서 에러가 있었음

  return restClient.get()
          .uri(uri)
          .retrieve()
          .body(NcsOccupationResponse.class);
}
```

처음에는 queryParam에서 인코딩을 자동으로 한번 더 해서 생긴 문제인줄알았는데

```java
URI uri = builder.build(true).encode().toUri();
```

이렇게 URI타입으로 받아오니까 문제가 해결되었음 왜 그런건지는 아직도 모르겠음.

> 이 문제를 해결하려고 10시간을 넘게 고생했는데 코드 한줄만 변경을 하니까 해결되서 기분이 좋으면서도 한편으로는 너무 허무했다.

## OpenAI API 사용하기

### GPT가 없는 NCS 코드 추천 혹은 사용자 정보와 아무 연관없는 NCS코드 추천

```text
1. 사용자가 입력한 정보와 관련된 NCS코드를 추천해주는 기능을 구현했는데, GPT가 관련없는 NCS코드를 추천하거나 없는 NCS코드를 추천하는 경우가 발생함
2. NCS코드를 추천해줘도 사용자가 입력한 정보와 관련이 없는 NCS코드를 추천하는 경우가 빈번하게 발생함
3. 임베딩을 활용해서 사용자 벡터와 NCS코드 벡터의 유사도를 계산하여 가장 유사한 NCS코드를 추천하는 방법을 고려중임 -> 실패
```

```text
1. OpenAi Stoarge와 Assistant를 활용하여 NCS코드 데이터(JSON)를 참조해 사용자와 관련된 NCS코드를 추천하는 방법을 사용했음 그나마 사용자에게 적합한 NCS코드를 추천해주는것같음
2. 하지만 NCS코드데이터를 너무 많이 참조하여 NCS코드를 한번 발급할때마다 평균 13000토큰을 사용하는 문제가 발생하였음.
3. 우선은 테스트 단계이니 정확도가 중요하다고 판단하여 추후에 수정할 수 있다면 수정할 예정임
```

### OpenAI Assistatns API WebClient 기반으로 비동기 호출 과정

1. `/threads` 생성 → 스레드 ID 획득
2. `/threads/{threadId}/messages` → 사용자 메시지 전송
3. `/threads/{threadId}/runs` → Assistant 실행 요청
4. `/threads/{threadId}/runs/{runId}` → 실행 완료 여부 polling
5. `/threads/{threadId}/messages` → 최종 응답 메시지 조회

```java
  public Mono<String> generateAssistantResponse(String userInput) {
  return openAiWebClient.post()
          .uri("/threads")
          .body(BodyInserters.fromValue(Collections.emptyMap()))
          .retrieve()
          .onStatus(HttpStatusCode::isError, response ->
                  response.bodyToMono(String.class).flatMap(body -> {
                    log.error("OpenAI /threads 호출 실패: {}", body);
                    return Mono.error(new RuntimeException("OpenAI 오류: " + body));
                  })
          )
          .bodyToMono(JsonNode.class)
          .map(json -> json.get("id").asText())
          .flatMap(threadId -> {
            return openAiWebClient.post()
                    .uri("/threads/" + threadId + "/messages")
                    .bodyValue(Map.of(
                            "role", "user",
                            "content", userInput
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .thenReturn(threadId);
          })
          .flatMap(threadId -> {
            return openAiWebClient.post()
                    .uri("/threads/" + threadId + "/runs")
                    .bodyValue(Map.of("assistant_id", config.getNcsCodeAssistantId()))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(json -> Map.entry(threadId, json.get("id").asText()));
          })
          .flatMap(entry -> waitForCompletion(entry.getKey(), entry.getValue()));
}
```

1. `/threads` 생성 후 스레드 ID를 획득합니다.

```java
openAiWebClient.post()
          .uri("/threads")
          .body(BodyInserters.fromValue(Collections.emptyMap()))
          .retrieve()
          .onStatus(HttpStatusCode::isError, response ->
                  response.bodyToMono(String.class).flatMap(body -> {
                    log.error("OpenAI /threads 호출 실패: {}", body);
                    return Mono.error(new RuntimeException("OpenAI 오류: " + body));
                  })
          )
```

- 처음에 `bodyValue(Map.of())`를 넣어주었더니 `400 Bad Request` 에러가 발생함이유는 `/threads` API가 빈 JSON을 요구하기 때문임
- `body(BodyInserters.fromValue(Collections.emptyMap()))`로 빈 JSON을 넣어주면 정상적으로 스레드 ID를 획득할 수 있음

2. `/threads/{threadId}/messages` API를 호출하여 사용자 메시지를 전송합니다.

```java
.bodyToMono(JsonNode.class)
.map(json -> json.get("id").asText())
.flatMap(threadId -> {
  return openAiWebClient.post()
                    .uri("/threads/" + threadId + "/messages")
                    .bodyValue(Map.of(
                                  "role", "user",
                                  "content", userInput
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .thenReturn(threadId);
})
```

- 스레드 ID를 획득한 후 `/threads/{threadId}/messages` API를 호출하여 사용자 메시지를 전송합니다.

3. `/threads/{threadId}/runs` API를 호출하여 Assistant 실행 요청을 보냅니다.

```java
.flatMap(threadId -> {
            return openAiWebClient.post()
                    .uri("/threads/" + threadId + "/runs")
                    .bodyValue(Map.of("assistant_id", config.getNcsCodeAssistantId()))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(json -> Map.entry(threadId, json.get("id").asText()));
})
```

4. `/threads/{threadId}/runs/{runId}` API를 호출하여 Assistant 실행 완료 여부를 polling합니다.

```java
.flatMap(entry -> waitForCompletion(entry.getKey(), entry.getValue()));
```

---

- `waitForCompletion` 메서드는 Assistant 실행 완료 여부를 polling하는 메서드입니다.

```java
  private Mono<String> waitForCompletion(String threadId, String runId) {
    return Mono.defer(() ->
            openAiWebClient.get()
                    .uri("/threads/" + threadId + "/runs/" + runId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
    ).flatMap(json -> {
      String status = json.get("status").asText();
      if ("completed".equals(status)) {
        return getMessagesFromThread(threadId);
      } else if ("failed".equals(status)) {
        return Mono.error(new IllegalStateException("Assistant run failed"));
      } else {
        return Mono.delay(Duration.ofSeconds(1)).then(waitForCompletion(threadId, runId));
      }
    });
  }
```

- `status`가 `completed`이면 `/threads/{threadId}/messages` API를 호출하여 Assistant의 응답 메시지를 조회합니다.
- `status`가 `failed`이면 에러를 발생시킵니다.
- 그 외의 경우에는 1초 후에 다시 polling합니다.
- 이렇게 하면 Assistant의 응답 메시지를 비동기적으로 받을 수 있습니다.

---

- `/threads/{threadId}/messages` API를 호출하여 Assistant의 응답 메시지를 조회합니다.

```java
  private Mono<String> getMessagesFromThread(String threadId) {
    return openAiWebClient.get()
            .uri("/threads/" + threadId + "/messages")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> {
              JsonNode data = json.get("data");
              for (JsonNode message : data) {
                if ("assistant".equals(message.get("role").asText())) {
                  return message.get("content").get(0).get("text").get("value").asText();
                }
              }
              return "[No assistant response]";
            });
  }
```

- `/threads/{threadId}/messages` API를 호출하여 Assistant의 응답 메시지를 조회합니다.
- `data` 배열에서 `role`이 `assistant`인 메시지를 찾아서 그 내용을 반환합니다.
- 만약 `assistant`의 응답이 없다면 `[No assistant response]`를 반환합니다.
- 이렇게 하면 Assistant의 응답 메시지를 비동기적으로 받을 수 있습니다.