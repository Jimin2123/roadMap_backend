package com.shingu.roadmap.apis.openai.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * OpenAI API 캐시를 위한 고성능 커스텀 키 생성기
 *
 * 주요 특징:
 * - 사용자 민감정보 해시화
 * - 캐시 키 버전 관리
 * - 성능 최적화된 해시 생성
 * - 상황별 맞춤형 키 생성 전략
 * - 캐시 무효화 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCacheKeyGenerator implements KeyGenerator {

    private final ObjectMapper objectMapper;

    // 성능 최적화를 위한 해시 캐시
    private final Map<String, String> hashCache = new ConcurrentHashMap<>();

    // 캐시 키 버전 관리
    // 주의: CACHE_VERSION 변경 시 기존 캐시 데이터와 호환성 문제가 발생할 수 있습니다.
    // 버전 변경 시 수행해야 할 작업:
    // 1. Redis 캐시 전체 또는 openai:* 패턴 캐시 무효화 (FLUSHDB 또는 패턴 삭제)
    // 2. 애플리케이션 재시작
    // 3. 캐시 워밍업 모니터링
    private static final String CACHE_VERSION = "v1.2";

    // 캐시 키 최대 길이 (Redis 권장사항)
    private static final int MAX_KEY_LENGTH = 250;

    // 해시 길이 (성능과 충돌 확률의 균형)
    private static final int HASH_LENGTH = 12;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String methodName = method.getName();

        try {
            String baseKey = CACHE_VERSION + ":" + methodName + ":";

            switch (methodName) {
                case "recommendTrainingCourse":
                    return baseKey + generateTrainingRecommendationKey((TrainingRecommendationRequest) params[0]);
                case "recommendNcsCodeUsingAssistant":
                case "recommendSearchCodes":
                case "generateKeyword":
                    if (params[0] instanceof Profile profile) {
                        return baseKey + generateProfileBasedKey(profile);
                    } else if (params[0] instanceof ProfileResponse profileResponse) {
                        return baseKey + generateProfileResponseBasedKey(profileResponse);
                    }
                    break;
                case "recommendDesiredJobCodeUsingAssistant":
                    return baseKey + generateStringBasedKey((String) params[0]);
                default:
                    return generateDefaultKey(target, method, params);
            }
        } catch (Exception e) {
            log.warn("캐시 키 생성 실패, 안전한 기본 키 사용. Method: {}, Error: {}", methodName, e.getMessage());
            return generateSafeDefaultKey(target, method, params);
        }

        // generateDefaultKey도 예외 처리가 필요할 수 있음
        try {
            return generateDefaultKey(target, method, params);
        } catch (Exception e) {
            log.error("기본 캐시 키 생성도 실패, 비상 키 사용. Method: {}, Error: {}", methodName, e.getMessage());
            return generateSafeDefaultKey(target, method, params);
        }
    }

    /**
     * 훈련과정 추천용 캐시 키 생성
     * 사용자 프로필 + 주소 + 훈련과정 목록의 최적화된 해시값 사용
     */
    private String generateTrainingRecommendationKey(TrainingRecommendationRequest request) {
        if (request == null) {
            return "error:null_request";
        }

        try {
            StringBuilder keyBuilder = new StringBuilder();

            // 사용자 프로필 해시
            if (request.userProfile() != null) {
                keyBuilder.append(hashProfileResponse(request.userProfile()));
            } else {
                keyBuilder.append("no_profile");
            }

            keyBuilder.append(":");

            // 주소 해시 (지역 정보가 추천에 중요함)
            if (StringUtils.hasText(request.address())) {
                keyBuilder.append(hashStringOptimized(request.address()));
            } else {
                keyBuilder.append("no_addr");
            }

            keyBuilder.append(":");

            // 훈련과정 목록 해시 (훈련과정 ID들의 정렬된 해시)
            if (request.trainingCourses() != null && !request.trainingCourses().isEmpty()) {
                String coursesHash = hashTrainingCourses(request.trainingCourses());
                keyBuilder.append(coursesHash);
            } else {
                keyBuilder.append("no_courses");
            }

            String finalKey = keyBuilder.toString();
            return ensureKeyLength(finalKey);

        } catch (Exception e) {
            log.error("훈련과정 추천 캐시 키 생성 실패", e);
            return "error:" + generateErrorKey();
        }
    }

    /**
     * Profile 도메인 객체 기반 캐시 키 생성
     * 도메인 객체의 연관관계를 고려한 최적화된 해시 생성
     */
    private String generateProfileBasedKey(Profile profile) {
        if (profile == null) {
            return "error:null_profile";
        }

        try {
            ProfileCacheKey cacheKey = ProfileCacheKey.fromDomain(profile);
            String profileHash = hashObject(cacheKey);
            String profileId = profile.getId() != null ? profile.getId().toString() : "anon";

            return profileHash + ":" + profileId;
        } catch (Exception e) {
            log.error("Profile 도메인 기반 캐시 키 생성 실패. ProfileId: {}",
                     profile.getId(), e);
            return "error:" + generateErrorKey();
        }
    }

    /**
     * ProfileResponse DTO 기반 캐시 키 생성
     */
    private String generateProfileResponseBasedKey(ProfileResponse profileResponse) {
        if (profileResponse == null) {
            return "error:null_profile_response";
        }

        try {
            String profileHash = hashProfileResponse(profileResponse);
            return profileHash;
        } catch (Exception e) {
            log.error("ProfileResponse 기반 캐시 키 생성 실패", e);
            return "error:" + generateErrorKey();
        }
    }

    /**
     * 문자열 기반 캐시 키 생성 (희망 직무 등)
     */
    private String generateStringBasedKey(String input) {
        if (!StringUtils.hasText(input)) {
            return "error:empty_string";
        }

        return hashStringOptimized(input.trim());
    }

    /**
     * 기본 캐시 키 생성 (fallback)
     */
    private String generateDefaultKey(Object target, Method method, Object... params) {
        String className = target.getClass().getSimpleName();
        String methodName = method.getName();
        int paramsHash = Arrays.hashCode(params);

        return String.format("%s:%s:%s:%d",
                           CACHE_VERSION, className, methodName, paramsHash);
    }

    /**
     * 안전한 기본 캐시 키 생성 (에러 상황에서 사용)
     */
    private String generateSafeDefaultKey(Object target, Method method, Object... params) {
        try {
            return String.format("%s:safe:%s:%s:%d",
                               CACHE_VERSION,
                               target.getClass().getSimpleName(),
                               method.getName(),
                               Objects.hash(params));
        } catch (Exception e) {
            return CACHE_VERSION + ":emergency:" + System.currentTimeMillis();
        }
    }

    /**
     * ProfileResponse 객체의 핵심 정보를 해시화
     */
    private String hashProfileResponse(ProfileResponse profileResponse) {
        try {
            ProfileResponseCacheKey cacheKey = ProfileResponseCacheKey.from(profileResponse);
            return hashObject(cacheKey);
        } catch (Exception e) {
            log.warn("ProfileResponse 해시 생성 실패, 간단한 해시 사용", e);
            return hashStringOptimized(String.valueOf(profileResponse.hashCode()));
        }
    }

    /**
     * 훈련과정 목록의 최적화된 해시 생성
     */
    private String hashTrainingCourses(List<?> trainingCourses) {
        if (trainingCourses == null || trainingCourses.isEmpty()) {
            return "empty";
        }

        try {
            // 훈련과정 ID들만 추출하여 정렬된 해시 생성
            List<String> courseIds = trainingCourses.stream()
                .map(course -> {
                    try {
                        // GptTrainingCourseDto의 구조에 맞게 ID 추출
                        return String.valueOf(course.hashCode());
                    } catch (Exception e) {
                        return "unknown";
                    }
                })
                .sorted()
                .toList();

            String joined = String.join(",", courseIds);
            return hashStringOptimized(joined);
        } catch (Exception e) {
            log.warn("훈련과정 목록 해시 생성 실패", e);
            return hashStringOptimized(String.valueOf(trainingCourses.hashCode()));
        }
    }

    /**
     * 객체를 JSON으로 직렬화 후 최적화된 해시화
     */
    private String hashObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            String json = objectMapper.writeValueAsString(obj);
            return hashStringOptimized(json);
        } catch (JsonProcessingException e) {
            log.warn("객체 JSON 직렬화 실패, toString 해시 사용. Object: {}",
                     obj.getClass().getSimpleName(), e);
            return hashStringOptimized(obj.toString());
        }
    }

    /**
     * 성능 최적화된 해시 생성
     * 캐시를 활용하여 동일한 입력에 대한 재계산 방지
     */
    private String hashStringOptimized(String input) {
        if (input == null || input.isEmpty()) {
            return "empty";
        }

        // 성능 최적화: 짧은 문자열은 캐시에서 조회
        if (input.length() < 100) {
            return hashCache.computeIfAbsent(input, this::computeHash);
        }

        return computeHash(input);
    }

    /**
     * 실제 해시 계산 (SHA-256 사용)
     */
    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // HEX 문자열 변환 최적화
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            // 지정된 길이로 자르기
            return sb.substring(0, Math.min(HASH_LENGTH, sb.length()));

        } catch (Exception e) {
            log.error("해시 계산 실패, Java hashCode 사용", e);
            return String.format("%08x", input.hashCode()).substring(0, 8);
        }
    }

    /**
     * 캐시 키 길이 제한 및 검증
     */
    private String ensureKeyLength(String key) {
        if (key == null) {
            return "null_key";
        }

        if (key.length() <= MAX_KEY_LENGTH) {
            return key;
        }

        // 너무 긴 키는 해시로 축약
        String prefix = key.substring(0, MAX_KEY_LENGTH - HASH_LENGTH - 1);
        String suffix = computeHash(key);
        return prefix + "_" + suffix;
    }

    /**
     * 에러 상황에서 사용할 고유 키 생성
     */
    private String generateErrorKey() {
        return System.currentTimeMillis() + "_" +
               Thread.currentThread().getId();
    }

    /**
     * Profile 도메인 객체용 캐시 키 생성 객체
     * 연관관계 로딩 없이 효율적인 키 생성
     */
    private record ProfileCacheKey(
        String educationLevel,
        List<String> skillsWithProficiency,
        List<String> certificateNames,
        List<String> desiredJobCodes,
        List<String> userCapabilityCodes,
        List<String> desiredCapabilityCodes,
        String resumeSignature
    ) {
        public static ProfileCacheKey fromDomain(Profile profile) {
            if (profile == null) {
                return new ProfileCacheKey(null, Collections.emptyList(),
                                         Collections.emptyList(), Collections.emptyList(),
                                         Collections.emptyList(), Collections.emptyList(), "empty");
            }

            try {
                // 스킬 + 숙련도 정보 (정렬하여 일관성 보장)
                List<String> skillsWithProficiency = profile.getProfileSkills().stream()
                    .map(ps -> ps.getSkill().getName() + ":" + ps.getProficiency())
                    .sorted()
                    .collect(Collectors.toList());

                // 자격증 이름 (정렬하여 일관성 보장)
                List<String> certificateNames = profile.getProfileCertificates().stream()
                    .map(pc -> pc.getCertificate().getJmfldnm())
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

                // 희망 직무 코드
                List<String> desiredJobCodes = profile.getDesiredJobs().stream()
                    .map(job -> String.valueOf(job.getCode()))
                    .sorted()
                    .collect(Collectors.toList());

                // 사용자 역량 NCS 코드
                List<String> userCapabilityCodes = profile.getUserCapabilities().stream()
                    .map(ncs -> ncs.getDutyCd() + ":" + ncs.getDutyNm())
                    .sorted()
                    .collect(Collectors.toList());

                // 희망 역량 NCS 코드
                List<String> desiredCapabilityCodes = profile.getDesiredCapabilities().stream()
                    .map(ncs -> ncs.getDutyCd() + ":" + ncs.getDutyNm())
                    .sorted()
                    .collect(Collectors.toList());

                // 이력서 시그니처 (무거운 내용 대신 핵심 정보만)
                String resumeSignature = generateResumeSignature(profile.getResume());

                return new ProfileCacheKey(
                    profile.getEducationLevel(),
                    skillsWithProficiency,
                    certificateNames,
                    desiredJobCodes,
                    userCapabilityCodes,
                    desiredCapabilityCodes,
                    resumeSignature
                );

            } catch (Exception e) {
                log.warn("Profile 캐시 키 생성 중 오류, 기본값 사용. ProfileId: {}",
                         profile.getId(), e);
                return new ProfileCacheKey(
                    "unknown", Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), "error"
                );
            }
        }

        private static String generateResumeSignature(Object resume) {
            if (resume == null) {
                return "empty";
            }

            try {
                // 이력서의 내용을 기반으로 일관성 있는 시그니처 생성
                // hashCode()는 JVM에 따라 달라질 수 있으므로 SHA-256 해시 사용
                String resumeContent = resume.toString();
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(resumeContent.getBytes(StandardCharsets.UTF_8));

                // 해시를 16진수로 변환하여 반환 (처음 12자리만 사용)
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(6, hash.length); i++) {
                    sb.append(String.format("%02x", hash[i]));
                }
                return sb.toString();
            } catch (Exception e) {
                // 예외 발생 시 현재 시간을 기반으로 한 대체 시그니처 생성
                return "fallback_" + String.valueOf(System.currentTimeMillis()).substring(8);
            }
        }
    }

    /**
     * ProfileResponse DTO용 캐시 키 생성 객체
     */
    private record ProfileResponseCacheKey(
        String educationLevel,
        Set<String> desiredJobNames,
        Set<String> certificateNames,
        Set<String> skillsWithProficiency,
        Set<String> capabilityCodes
    ) {
        public static ProfileResponseCacheKey from(ProfileResponse profileResponse) {
            if (profileResponse == null) {
                return new ProfileResponseCacheKey(null, Collections.emptySet(),
                                                 Collections.emptySet(), Collections.emptySet(),
                                                 Collections.emptySet());
            }

            try {
                Set<String> desiredJobNames = profileResponse.desiredJob() != null ?
                    profileResponse.desiredJob().stream()
                        .map(job -> job.name())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()) :
                    Collections.emptySet();

                Set<String> certificateNames = profileResponse.certificates() != null ?
                    profileResponse.certificates().stream()
                        .map(cert -> cert.name())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()) :
                    Collections.emptySet();

                Set<String> skillsWithProficiency = profileResponse.skills() != null ?
                    profileResponse.skills().stream()
                        .map(skill -> skill.name() + ":" + skill.proficiency())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()) :
                    Collections.emptySet();

                Set<String> capabilityCodes = new HashSet<>();
                if (profileResponse.userCapabilities() != null) {
                    capabilityCodes.addAll(
                        profileResponse.userCapabilities().stream()
                            .map(cap -> "user:" + cap.code())
                            .collect(Collectors.toSet())
                    );
                }
                if (profileResponse.desiredCapabilities() != null) {
                    capabilityCodes.addAll(
                        profileResponse.desiredCapabilities().stream()
                            .map(cap -> "desired:" + cap.code())
                            .collect(Collectors.toSet())
                    );
                }

                return new ProfileResponseCacheKey(
                    profileResponse.educationLevel(),
                    desiredJobNames,
                    certificateNames,
                    skillsWithProficiency,
                    capabilityCodes
                );

            } catch (Exception e) {
                log.warn("ProfileResponse 캐시 키 생성 중 오류, 기본값 사용", e);
                return new ProfileResponseCacheKey(
                    "unknown", Collections.emptySet(), Collections.emptySet(),
                    Collections.emptySet(), Collections.emptySet()
                );
            }
        }
    }
}