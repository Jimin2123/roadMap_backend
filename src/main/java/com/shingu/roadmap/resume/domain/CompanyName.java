package com.shingu.roadmap.resume.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * CompanyName Value Object
 * <p>
 * Encapsulates company name validation, normalization, and business logic.
 * Immutable by design - use static factory method for creation.
 * Supports both Korean and English company name patterns.
 * </p>
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class CompanyName {

    @Column(name = "company_name", nullable = false, length = 200)
    private String value;

    /**
     * Creates a CompanyName value object with validation and normalization
     *
     * @param name Raw company name string
     * @return Validated and normalized CompanyName instance
     * @throws IllegalArgumentException if name is blank or exceeds maximum length
     */
    public static CompanyName of(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Company name cannot be blank");
        }

        // Normalize: trim and collapse multiple spaces into single space
        String normalized = name.trim().replaceAll("\\s+", " ");

        if (normalized.length() > 200) {
            throw new IllegalArgumentException("Company name cannot exceed 200 characters");
        }

        return new CompanyName(normalized);
    }

    /**
     * Returns the display name for UI presentation
     *
     * @return Normalized company name
     */
    public String displayName() {
        return value;
    }

    /**
     * Returns abbreviated version by removing common company suffixes
     * <p>
     * Examples:
     * - "삼성전자 주식회사" → "삼성전자"
     * - "(주)네이버" → "네이버"
     * - "Samsung Electronics Co., Ltd." → "Samsung Electronics"
     * - "Google Inc." → "Google"
     * </p>
     *
     * @return Abbreviated company name
     */
    public String abbreviation() {
        String name = value;

        // Remove common Korean company suffixes
        // 주식회사, 유한회사, (주), (유) patterns
        name = name.replaceAll("\\s*(주식회사|유한회사|\\(주\\)|\\(유\\))\\s*", " ").trim();

        // Remove common English company suffixes
        // Co., Ltd. / Corporation / Corp. / Inc. / LLC patterns
        name = name.replaceAll("\\s*(Co\\.,?\\s*Ltd\\.?|Corporation|Corp\\.?|Inc\\.?|LLC).*$", "").trim();

        // Collapse any remaining multiple spaces
        name = name.replaceAll("\\s+", " ").trim();

        // If still empty after suffix removal, return original value
        if (name.isEmpty()) {
            return value;
        }

        return name;
    }

    /**
     * Returns the normalized company name value
     * Required for JPA queries and comparisons
     *
     * @return Normalized company name string
     */
    @Override
    public String toString() {
        return value;
    }
}
