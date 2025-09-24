package com.shingu.roadmap.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.security.dto.SecurityErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
                     AccessDeniedException accessDeniedException) throws IOException {

    log.warn("Access denied for request: {} {}, User: {}, Reason: {}",
        request.getMethod(), request.getRequestURI(),
        request.getRemoteUser() != null ? request.getRemoteUser() : "Anonymous",
        accessDeniedException.getMessage());

    SecurityErrorResponse errorResponse = SecurityErrorResponse.accessDenied(
        "요청하신 리소스에 대한 접근 권한이 없습니다."
    );

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
    response.getWriter().write(jsonResponse);
  }
}