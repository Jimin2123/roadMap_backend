# 🚀 청년 취업 역량 증진 길라잡이: 길라JOB

당신의 커리어 여정에 든든한 나침반이 되어줄 **길라JOB**입니다. 길라JOB은 당신의 가능성을 발견하고, 꿈의 직무를 향한 최적의 경로를 제시합니다.

## ✨ 주요 기능

- **AI 기반 심층 역량 분석 (Deep Competency Analysis)**
  : 이력서와 프로필 정보를 기반으로 사용자의 현재 역량을 정밀하게 분석합니다.

- **데이터 기반 직무 적합도 진단 (Data-driven Job Fitness Diagnosis)**
  : NCS(국가직무능력표준)과 커리어넷 직무백과 등의 데이터를 기반으로 희망 직무에서 요구하는 역량과 사용자 역량의 일치도를 상세 리포트로 제공하여 전략적인 준비를 가능하게 합니다.

- **개인화된 성장 경로 설계 (Personalized Career Roadmap)**
  : 역량 분석 결과를 바탕으로, 목표 직무에 도달하기 위한 최적의 학습 경로를 제시합니다. 필요한 자격증, 추천 강의, 관련 프로젝트 등 구체적이고 실행 가능한 액션 플랜을 추천합니다.

- **지능형 채용 공고 추천 (Intelligent Job Matching)**
  : 분석된 사용자의 역량과 '사람인' API를 통해 수집된 채용 공고 데이터를 매칭하여, 가장 적합한 채용공고를 추천합니다.

## 🔧 기술 스택

### Main Tech Stack

| 구분         | 기술                | 설명                                |
| :----------- | :------------------ | :---------------------------------- |
| **Backend**  | Java 21             | 메인 프로그래밍 언어                |
|              | Spring Boot 3.4.4   | 애플리케이션 프레임워크             |
|              | Spring Web          | RESTful API 및 웹 애플리케이션 개발 |
|              | Spring Data JPA     | ORM을 통한 데이터베이스 연동        |
|              | Spring Security     | 인증 및 인가 처리                   |
|              | JWT (jjwt)          | JSON Web Token을 이용한 사용자 인증 |
| **Database** | MariaDB             | 주 데이터베이스                     |
|              | Redis               | 캐싱 및 Refresh Token 저장          |
| **Testing**  | JUnit 5             | 자바 애플리케이션 단위 테스트       |
| **API Docs** | Springdoc (Swagger) | API 문서 자동화                     |
| **Build**    | Gradle              | 빌드 및 의존성 관리 도구            |
| **Etc**      | Lombok              | 보일러플레이트 코드 감소            |
|              | Jsoup               | HTML 파싱 라이브러리                |
|              | Jackson             | XML/JSON 데이터 처리                |

### External APIs

| API                        | 용도                                                          |
| :------------------------- | :------------------------------------------------------------ |
| **Spring AI**              | 스프링 애플리케이션에 AI 기능을 쉽게 통합하기 위한 라이브러리 |
| **OpenAI**                 | ChatGPT 모델을 활용한 AI 기능 구현                            |
| **사람인 (Saramin)**       | 채용 공고 정보 조회                                           |
| **Work24**                 | 고용 정보 및 일자리 데이터 조회                               |
| **Q-Net**                  | 국가기술자격 정보 및 시험 일정 조회                           |
| **CareerNet**              | 직업 및 진로 정보 조회                                        |
| **NCS (국가직무능력표준)** | 직무 기술 및 역량 정보 조회                                   |
| **YouthPolicy**            | 청년 지원 정책 정보 조회                                      |

## 📂 프로젝트 구조

```
src/
├── main/
│   ├── java/com/shingu/roadmap/
│   │   ├── apis/          # 외부 API 연동
│   │   │   ├── careernet/
│   │   │   ├── ncs/
│   │   │   ├── openai/
│   │   │   ├── qnet/
│   │   │   ├── saramin/
│   │   │   ├── work24/
│   │   │   └── youthPolicy/
│   │   ├── auth/          # 인증 및 인가 (JWT)
│   │   ├── common/        # 공통 모듈 (예외 처리, DTO 등)
│   │   ├── member/        # 회원 관련 기능
│   │   ├── policy/        # 정책 관련 기능
│   │   ├── resume/        # 이력서 관련 기능
│   │   ├── security/      # Spring Security 설정
│   │   ├── task/          # 직무 및 채용 공고
│   │   └── training/      # 교육 및 훈련
│   └── resources/         # application.yml, 정적 파일
├── test/                  # 테스트 코드
├── build.gradle           # 프로젝트 의존성 및 빌드 설정
└── README.md              # 프로젝트 소개
```
