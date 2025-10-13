package com.shingu.roadmap.resume.domain.unit;

import com.shingu.roadmap.resume.domain.CompanyName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CompanyName Value Object Unit Tests")
class CompanyNameTest {

    @Test
    @DisplayName("정상적인 회사명으로 CompanyName 생성 성공")
    void createCompanyName_withValidName_success() {
        // given
        String validName = "삼성전자";

        // when
        CompanyName companyName = CompanyName.of(validName);

        // then
        assertThat(companyName).isNotNull();
        assertThat(companyName.getValue()).isEqualTo("삼성전자");
        assertThat(companyName.displayName()).isEqualTo("삼성전자");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("빈 문자열이나 null로 생성 시도 시 예외 발생")
    void createCompanyName_withBlankName_throwsException(String invalidName) {
        // when & then
        assertThatThrownBy(() -> CompanyName.of(invalidName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company name cannot be blank");
    }

    @Test
    @DisplayName("200자를 초과하는 회사명으로 생성 시도 시 예외 발생")
    void createCompanyName_withTooLongName_throwsException() {
        // given
        String tooLongName = "A".repeat(201);

        // when & then
        assertThatThrownBy(() -> CompanyName.of(tooLongName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company name cannot exceed 200 characters");
    }

    @Test
    @DisplayName("회사명 정규화: 앞뒤 공백 제거 및 연속된 공백을 하나로 축소")
    void normalization_trimAndCollapseSpaces() {
        // given
        String nameWithSpaces = "  Samsung   Electronics  ";

        // when
        CompanyName companyName = CompanyName.of(nameWithSpaces);

        // then
        assertThat(companyName.getValue()).isEqualTo("Samsung Electronics");
    }

    @Test
    @DisplayName("한국 회사명 약어 생성: 주식회사 접미사 제거")
    void abbreviation_removeKoreanSuffix_jusikhoesa() {
        // given
        CompanyName companyName = CompanyName.of("삼성전자 주식회사");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("한국 회사명 약어 생성: (주) 접두사 제거")
    void abbreviation_removeKoreanPrefix_ju() {
        // given
        CompanyName companyName = CompanyName.of("(주)네이버");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("네이버");
    }

    @Test
    @DisplayName("한국 회사명 약어 생성: (유) 접두사 제거")
    void abbreviation_removeKoreanPrefix_yu() {
        // given
        CompanyName companyName = CompanyName.of("(유)카카오");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("카카오");
    }

    @Test
    @DisplayName("영어 회사명 약어 생성: Co., Ltd. 제거")
    void abbreviation_removeEnglishSuffix_coLtd() {
        // given
        CompanyName companyName = CompanyName.of("Samsung Electronics Co., Ltd.");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("Samsung Electronics");
    }

    @Test
    @DisplayName("영어 회사명 약어 생성: Inc. 제거")
    void abbreviation_removeEnglishSuffix_inc() {
        // given
        CompanyName companyName = CompanyName.of("Google Inc.");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("Google");
    }

    @Test
    @DisplayName("영어 회사명 약어 생성: Corporation 제거")
    void abbreviation_removeEnglishSuffix_corporation() {
        // given
        CompanyName companyName = CompanyName.of("Microsoft Corporation");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("Microsoft");
    }

    @Test
    @DisplayName("영어 회사명 약어 생성: Corp. 제거")
    void abbreviation_removeEnglishSuffix_corp() {
        // given
        CompanyName companyName = CompanyName.of("Apple Corp.");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("Apple");
    }

    @Test
    @DisplayName("영어 회사명 약어 생성: LLC 제거")
    void abbreviation_removeEnglishSuffix_llc() {
        // given
        CompanyName companyName = CompanyName.of("Amazon LLC");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("Amazon");
    }

    @Test
    @DisplayName("복합 접미사 제거 후 빈 문자열이 되면 원본 값 반환")
    void abbreviation_emptyAfterRemoval_returnsOriginal() {
        // given
        CompanyName companyName = CompanyName.of("주식회사");

        // when
        String abbreviation = companyName.abbreviation();

        // then
        assertThat(abbreviation).isEqualTo("주식회사");
    }

    @Test
    @DisplayName("displayName은 정규화된 값을 반환")
    void displayName_returnsNormalizedValue() {
        // given
        CompanyName companyName = CompanyName.of("  네이버   ");

        // when
        String displayName = companyName.displayName();

        // then
        assertThat(displayName).isEqualTo("네이버");
    }

    @Test
    @DisplayName("toString은 정규화된 값을 반환")
    void toString_returnsNormalizedValue() {
        // given
        CompanyName companyName = CompanyName.of("카카오");

        // when
        String result = companyName.toString();

        // then
        assertThat(result).isEqualTo("카카오");
    }

    @Test
    @DisplayName("동일한 회사명을 가진 CompanyName 객체는 equals로 동일성 보장")
    void equals_sameValue_returnsTrue() {
        // given
        CompanyName name1 = CompanyName.of("삼성전자");
        CompanyName name2 = CompanyName.of("삼성전자");

        // when & then
        assertThat(name1).isEqualTo(name2);
    }

    @Test
    @DisplayName("동일한 회사명을 가진 CompanyName 객체는 hashCode로 동일성 보장")
    void hashCode_sameValue_returnsSameHashCode() {
        // given
        CompanyName name1 = CompanyName.of("삼성전자");
        CompanyName name2 = CompanyName.of("삼성전자");

        // when & then
        assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
    }

    @Test
    @DisplayName("정규화로 인해 동일해진 회사명은 equals로 동일성 보장")
    void equals_normalizedToSame_returnsTrue() {
        // given
        CompanyName name1 = CompanyName.of("삼성전자");
        CompanyName name2 = CompanyName.of("  삼성전자  ");

        // when & then
        assertThat(name1).isEqualTo(name2);
    }
}
