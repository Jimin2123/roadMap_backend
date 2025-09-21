package com.shingu.roadmap.apis.openai.config;

import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OpenAiClientConfig {

    private final OpenAiConfig config;
    private final SecureLogger secureLogger;

    @Bean
    @Qualifier("openAiWebClient")
    public WebClient openAiWebClient() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("openai-pool")
                .maxConnections(config.getMaxConnections())
                .maxIdleTime(config.getMaxIdleTime())
                .maxLifeTime(config.getMaxLifeTime())
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .pendingAcquireMaxCount(config.getMaxConnections() * 2)
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout().toMillis())
                .responseTimeout(config.getReadTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(config.getReadTimeout().toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(config.getWriteTimeout().toSeconds(), TimeUnit.SECONDS)))
                .compress(true) // gzip 압축 활성화
                .keepAlive(true); // Keep-Alive 활성화

        return WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(headers -> {
                    headers.setBearerAuth(config.getSecureApiKey());
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.add("OpenAI-Beta", "assistants=v2");
                    headers.add("User-Agent", "RoadMap-App/1.0");
                    headers.add("Accept-Encoding", "gzip, deflate");
                })
                .filter(addRequestIdFilter())
                .filter(logRequestFilter())
                .filter(logResponseFilter())
                .filter(handleErrorFilter())
                .build();
    }

    private ExchangeFilterFunction addRequestIdFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            return Mono.just(ClientRequest.from(request)
                    .header("X-Request-ID", requestId)
                    .attribute("requestId", requestId)
                    .build());
        });
    }

    private ExchangeFilterFunction logRequestFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (secureLogger.shouldLogDebug()) {
                String requestId = request.attribute("requestId").map(Object::toString).orElse("unknown");
                secureLogger.logApiCall(requestId, request.url().getPath(), 0);

                log.debug("OpenAI HTTP Request - ID: {}, Method: {}, URL: {}",
                         requestId, request.method(), request.url());
            }
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponseFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (secureLogger.shouldLogDebug()) {
                String requestId = response.request().getHeaders().getFirst("X-Request-ID");
                if (requestId == null) {
                    requestId = "unknown";
                }

                log.debug("OpenAI HTTP Response - ID: {}, Status: {}, Headers: {}",
                         requestId, response.statusCode(),
                         response.headers().asHttpHeaders().toSingleValueMap());
            }
            return Mono.just(response);
        });
    }

    private ExchangeFilterFunction handleErrorFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                String requestId = response.request().getHeaders().getFirst("X-Request-ID");
                if (requestId == null) {
                    requestId = "unknown";
                }

                secureLogger.logApiError(requestId, "HTTP_ERROR",
                                       response.statusCode().toString(),
                                       "HTTP error response received");

                log.warn("OpenAI HTTP Error - ID: {}, Status: {}, URL: {}",
                        requestId, response.statusCode(), response.request().getURI());
            }
            return Mono.just(response);
        });
    }

    @Bean
    @Qualifier("openAiConnectionProvider")
    public ConnectionProvider openAiConnectionProvider() {
        return ConnectionProvider.builder("openai-connection-pool")
                .maxConnections(config.getMaxConnections())
                .maxIdleTime(config.getMaxIdleTime())
                .maxLifeTime(config.getMaxLifeTime())
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .pendingAcquireMaxCount(config.getMaxConnections() * 2)
                .evictInBackground(Duration.ofSeconds(30))
                .build();
    }
}
