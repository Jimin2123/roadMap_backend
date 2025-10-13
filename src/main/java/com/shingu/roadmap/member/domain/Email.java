package com.shingu.roadmap.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.regex.Pattern;

/**
 * Email Value Object
 * <p>
 * Encapsulates email validation, normalization, and business logic.
 * Immutable by design - use static factory method for creation.
 * </p>
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class Email {

    /**
     * RFC 5322 simplified email pattern
     * Validates: local-part@domain.tld
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String value;

    /**
     * Creates an Email value object with validation and normalization
     *
     * @param email Raw email string
     * @return Validated and normalized Email instance
     * @throws IllegalArgumentException if email is blank or invalid format
     */
    public static Email of(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        String normalized = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        return new Email(normalized);
    }

    /**
     * Returns the local part of the email (before @)
     *
     * @return Local part (e.g., "user" from "user@example.com")
     */
    public String localPart() {
        return value.split("@")[0];
    }

    /**
     * Returns the domain part of the email (after @)
     *
     * @return Domain (e.g., "example.com" from "user@example.com")
     */
    public String domain() {
        return value.split("@")[1];
    }

    /**
     * Returns a masked version of the email for privacy/display
     * <p>
     * Examples:
     * - "ab@example.com" → "ab***@example.com"
     * - "user@example.com" → "us***@example.com"
     * - "a@example.com" → "a***@example.com"
     * </p>
     *
     * @return Masked email string
     */
    public String masked() {
        String[] parts = value.split("@");
        String local = parts[0];
        int visibleChars = Math.min(2, local.length());
        return local.substring(0, visibleChars) + "***@" + parts[1];
    }

    /**
     * Returns the normalized email value
     * Required for JPA queries and comparisons
     *
     * @return Normalized email string
     */
    @Override
    public String toString() {
        return value;
    }
}
