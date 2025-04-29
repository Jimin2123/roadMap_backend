## 프로젝트 구조

```bash
├── CommitMessage.md # 커밋 메세지 규칙
├── Dockerfile
├── HELP.md # 도움말 문서
├── README.md # 프로젝트 설명서
├── docker # 도커 관련 파일
│   ├── mariadb
│   │   ├── config
│   │   │   └── custom.cnf
│   │   ├── data
│   └── redis
│       ├── config
│       │   └── redis.conf
│       ├── data
│       └── log
├── docker-compose-local.yml
└── src
    ├── main
    │   └── roadmap
    │       ├── RoadMapApplication.java # # 애플리케이션 진입점
    │       ├── apis # api 관련 디렉토리
    │       │   ├── q-net
    │       │   └── work24
    │       ├── auth # 인증 관련 디렉토리
    │       │   ├── controller
    │       │   ├── domain
    │       │   ├── repository
    │       │   └── service
    │       ├── common # 공통 모듈
    │       │   ├── annotation # 어노테이션 관련
    │       │   ├── config # 설정 관련
    │       │   ├── enums # 열거형 관련
    │       │   ├── exception # 예외 처리 관련
    │       │   └── filter # 필터 관련
    │       ├── member # 회원 관련 디렉토리
    │       │   ├── controller
    │       │   ├── domain
    │       │   ├── repository
    │       │   └── service
    │       ├── portfolio # 포트폴리오 관련 디렉토리
    │       │   ├── controller
    │       │   ├── domain
    │       │   ├── repository
    │       │   └── service
    │       ├── training # 교육, 훈련 관련 디렉토리
    │       │   ├── controller
    │       │   ├── domain
    │       │   ├── repository
    │       │   └── service
    │       ├── task # 직무, 채용 관련 디렉토리
    │       │   ├── controller
    │       │   ├── domain
    │       │   ├── repository
    │       │   └── service
    │       └── utils
    │       └── resources # 정적파일
    │           ├── application-dev.yml
    │           ├── application.yml
    │           ├── static
    │           └── templates
    └── test
```
