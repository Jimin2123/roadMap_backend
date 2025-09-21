package com.shingu.roadmap.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.security.event.SecurityEventPublisher;
import com.shingu.roadmap.security.filter.EnhancedJwtAuthenticationFilter;
import com.shingu.roadmap.security.filter.SecurityHeadersFilter;
import com.shingu.roadmap.security.handler.JwtAccessDeniedHandler;
import com.shingu.roadmap.security.handler.JwtAuthenticationEntryPoint;
import com.shingu.roadmap.security.jwt.JwtUtil;
import com.shingu.roadmap.security.service.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@OpenAPIDefinition(
        info = @Info(title = "Spark API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth") // 기본 인증 적용
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtUtil jwtUtil;
  private final CustomUserDetailsService customUserDetailsService;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
  private final SecurityEventPublisher securityEventPublisher;
  private final ObjectMapper objectMapper;
  private final Environment environment;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .cors(Customizer.withDefaults()) // CORS 설정
            .authorizeHttpRequests(req -> req
                    .requestMatchers(
                            "/api/v1/member", // 회원 가입
                            "/api/v1/auth/login", // 로그인
                            "/api/v1/auth/refreshToken", // 리프레시 토큰
                            "/api/v1/jobs", // 채용 공고 조회
                            "/api/v1/internship", // 인턴십 공고 조회
                            "/api/v1/training", // 교육 과정 조회
                            "/api/v1/policy", // 정책 조회
                            "/api/v1/certificate/exam-schedule", // 자격증 시험 일정 조회
                            "/api-docs/**",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(securityHeadersFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(enhancedJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration config = new CorsConfiguration();

    if (corsProperties.getAllowedOrigins().isEmpty()) {
      // 개발 환경에서만 모든 origin 허용
      if (isDevEnvironment()) {
        config.addAllowedOriginPattern("*");
      } else {
        throw new IllegalStateException("Production environment must specify allowed origins");
      }
    } else {
      corsProperties.getAllowedOrigins().forEach(config::addAllowedOrigin);
    }

    corsProperties.getAllowedMethods().forEach(config::addAllowedMethod);
    corsProperties.getAllowedHeaders().forEach(config::addAllowedHeader);
    corsProperties.getExposedHeaders().forEach(config::addExposedHeader);
    config.setAllowCredentials(corsProperties.isAllowCredentials());
    config.setMaxAge(corsProperties.getMaxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  private boolean isDevEnvironment() {
    String[] activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length == 0 ||
           java.util.Arrays.asList(activeProfiles).contains("dev") ||
           java.util.Arrays.asList(activeProfiles).contains("local");
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public EnhancedJwtAuthenticationFilter enhancedJwtAuthenticationFilter() {
    return new EnhancedJwtAuthenticationFilter(
        jwtUtil,
        customUserDetailsService,
        securityEventPublisher,
        objectMapper
    );
  }

  @Bean
  public SecurityHeadersFilter securityHeadersFilter() {
    return new SecurityHeadersFilter(environment);
  }
}