package com.shingu.roadmap.resume.dto.request;

import java.time.LocalDate;

public record PeriodRequest(LocalDate startDate, LocalDate endDate) { }