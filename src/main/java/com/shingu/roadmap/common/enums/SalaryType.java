package com.shingu.roadmap.common.enums;

public enum SalaryType {
  ANNUAL("연봉"),
  MONTHLY("월급"),
  DAILY("일급"),
  HOURLY("시급"),
  NEGOTIABLE("추후협의");

  private final String description;

  SalaryType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}