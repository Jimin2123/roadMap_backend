package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Portfolio;

public record PortfolioResponse(
        String title,
        String url
) {
  public static PortfolioResponse from(Portfolio p) {
    return new PortfolioResponse(p.getTitle(), p.getUrl());
  }
}
