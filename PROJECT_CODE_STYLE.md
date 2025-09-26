# RoadMap 프로젝트 코드 스타일 가이드

이 문서는 RoadMap 프로젝트의 코드 스타일, 규칙, 프로그래밍 패턴을 정의하여 일관성 있고 고품질의 코드를 유지하는 것을 목표로 합니다. 모든 기여자는 이 가이드를 숙지하고 준수해야 합니다.

## 개요

본 프로젝트는 **Java 21**과 **Spring Boot 3.4**를 기반으로 하는 현대적인 웹 애플리케이션입니다. **Domain-Driven Design (DDD)** 사상을 적극적으로 채택하여 비즈니스 도메인 중심으로 코드를 구성하고 있으며, 코드 작성 전 **문서화를 의무화**하는 등 매우 체계적이고 엄격한 개발 프로세스를 따르고 있습니다. 전반적으로 코드의 가독성, 유지보수성, 보안을 중요하게 생각하는 스타일을 가집니다.

---

## 1. 포매팅 규칙 (Formatting Rules)

- **들여쓰기**: Java 코드에서 일관되게 **4개의 공백(space)**을 사용합니다. 탭(tab) 문자는 사용하지 않습니다.
- **괄호 스타일**: K&R(Kernighan & Ritchie) 스타일을 따릅니다. 즉, 여는 중괄호(`{`)는 클래스나 메소드 선언과 같은 줄에 위치합니다.
- **줄 바꿈**: 메소드 사이, 그리고 논리적으로 구분되는 코드 블록 사이에 한 줄의 공백을 두어 가독성을 높입니다.
- **최대 줄 길이**: 한 줄은 120자를 넘지 않도록 노력합니다.
- **자동 포매터**: 특정 포매터가 강제되지는 않으나, IntelliJ IDE의 기본 Java 포매팅 규칙을 따르는 것을 권장합니다.

## 2. 명명 규칙 (Naming Conventions)

- **패키지**: 소문자를 사용하며, `com.shingu.roadmap.{도메인}.{레이어}` 구조를 따릅니다. (예: `com.shingu.roadmap.auth.controller`)
- **클래스 및 인터페이스**: 파스칼 케이스(PascalCase)를 사용합니다. (예: `AuthController`, `Account`, `AuthControllerSwagger`)
- **메소드 및 변수**: 카멜 케이스(camelCase)를 사용합니다. (예: `login`, `authService`, `loginRequest`)
- **상수**: 대문자 스네이크 케이스(UPPER_SNAKE_CASE)를 사용합니다. (예: `REFRESH_COOKIE_NAME`)
- **DTO (Data Transfer Object)**: 기능에 따라 `Request`, `Response` 접미사를 명확하게 사용합니다. (예: `LoginRequest`, `LoginResponse`)
- **데이터베이스**: 테이블과 인덱스 이름은 소문자 스네이크 케이스(snake_case)를 사용합니다. (예: `account`, `idx_account_email`)

## 3. 프로그래밍 원칙 및 패턴 (Programming Principles & Patterns)

- **Domain-Driven Design (DDD)**: 프로젝트 구조의 핵심 원칙입니다. `auth`, `member`, `training` 등 비즈니스 도메인별로 패키지를 명확히 분리합니다.
- **계층형 아키텍처 (Layered Architecture)**: 각 도메인 내부는 `controller` -> `service` -> `repository`의 전통적인 계층 구조를 따릅니다.
- **의존성 주입 (Dependency Injection)**: Lombok의 `@RequiredArgsConstructor`를 사용한 **생성자 주입**을 적극적으로 활용하여 불변성과 테스트 용이성을 확보합니다.
- **RESTful API 디자인**: 리소스 기반의 명확한 URL(e.g., `/api/v1/auth/login`), HTTP 메소드(`@PostMapping`), 적절한 상태 코드 반환 등 REST 원칙을 준수합니다.
- **DTO 패턴**: Controller와 Service 계층에서 DTO를 사용하여 API 명세와 내부 도메인 모델(`@Entity`)을 명확히 분리합니다.
- **인터페이스 기반 Controller**: `AuthController implements AuthControllerSwagger`처럼, API 명세(Swagger 어노테이션)를 담는 인터페이스를 분리하여 Controller 구현체의 가독성을 높입니다.
- **빌더 패턴 (Builder Pattern)**: `@Builder(toBuilder = true)`를 사용하여 객체 생성의 유연성과 안정성을 높입니다.
- **보안**: JWT 기반 인증을 사용하며, Refresh Token은 CSRF 공격에 대비해 `HttpOnly`, `Secure`, `SameSite` 속성이 적용된 보안 쿠키로 관리합니다.

## 4. 문서화 규칙 (Documentation Rules)

- **[필수] 선-개발 문서화**: 기능 개발, 리팩토링, 버그 수정 등 **모든 코드 작업 전에** GITHUB Issue 템플릿을 사용하여 관련 문서를 **반드시 작성**해야 합니다.
- **API 문서화**: `springdoc-openapi` (Swagger)를 사용합니다. 어노테이션을 별도의 인터페이스(`AuthControllerSwagger`)에 분리하여 관리합니다.
- **코드 주석**: Javadoc 형식(`/** ... */`)을 사용하여 메소드의 목적, 파라미터, 반환 값, 그리고 특히 **보안 관련 고려사항("왜" 이렇게 구현했는지)**을 명확히 설명합니다.
- **커밋 메시지**: 루트 디렉토리의 `CommitMessage.md` 파일에 정의된 표준화된 커밋 메시지 규칙을 따릅니다.

## 5. 프로젝트 구조 및 설정 스타일 (Project Structure & Configuration)

- **빌드 시스템**: Gradle을 사용하며, 프로젝트 내 `gradlew` 래퍼를 통해 일관된 빌드 환경을 유지합니다.
- **설정 파일**: `application.yml`을 기본으로 사용하며, `spring.profiles.active`를 통해 `dev`, `exam` 등 환경별 설정을 분리합니다.
- **환경 변수**: `dotenv-java` 라이브러리를 사용하여 `.env` 파일 시스템을 통해 민감한 정보를 관리합니다.
- **모듈 구조**: DDD에 기반한 높은 모듈성을 가집니다. 최상위 패키지가 비즈니스 도메인 경계를 형성합니다.
- **데이터베이스 스키마**: `src/main/resources/database` 경로에 번호가 매겨진 `.sql` 파일을 통해 스키마 변경 이력을 관리합니다.

## 6. 테스트 스타일 (Testing Style)

- **테스트 프레임워크**: `spring-boot-starter-test`를 통해 JUnit 5, Mockito 등 표준 테스트 라이브러리를 사용합니다.
- **테스트 실행**: `./gradlew test` 명령어로 테스트를 실행합니다.
- **[필수] 빌드 검증**: 코드 수정 후에는 **반드시 `./gradlew build`를 실행**하여 컴파일 성공 및 모든 테스트 통과를 확인해야 합니다. 이는 코드 품질을 보증하는 핵심적인 절차입니다.
- **테스트 위치**: 표준 디렉토리인 `src/test/java`에 위치합니다.
