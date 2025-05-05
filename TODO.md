## 2025/05/01 ~ 2025/05/03

- [x] OpenAI API Client 구현 → `openAiService.recommendNcsCodes(member).block()`로 사용
- [x] OpenAI에게 사용자의 기술스택, 자격증을 전달하고 NCS 직무코드를 발급받는 로직 구현 → `updateProfile()` 내에서 처리
- [x] NCS API Client 구현 → `NcsApiClient`, `NcsApiProperties`, `NcsApiClientConfig` 설정 완료
- [x] OpenAI에게 발급받은 NCS 직무코드를 NCS API에 전달하여 있는 코드인지 확인하는 로직 구현 → `filterValidNcsCodes()` 구현 완료
- [x] NCS 직무정보 API 데이터 받아와서 DB에 저장 → `fetchAndRegisterNcsOccupation()`로 구현
- [x] NCS 훈련기준 고려사항 API 데이터를 받아와서 DB에 저장 → 위 메서드 내에서 함께 처리
- [x] 직무정보 - 훈련기준 고려사항 관계형성 → `NcsOccupationStandardLink`를 통해 연결
- [x] 사용자의 NCS 직무코드와 매핑하는 로직 구현 → `Member.updateNcsOccupations(Set<NcsOccupation>)` 등으로 연결 완료

## 2025/05/05

- [x] Work24 API Client 구현
- [x] 훈련정보 API 데이터를 받아와 사용자의 NCS 직무코드와 매핑하여 사용자와 알맞는 훈련정보를 반환하는 로직을 구현

## 앞으로 구현해야할 로직

### 훈련/교육 관련
1. NCS 훈련기준 고려사항에서 "관련자격종목" 중에 실무에서 쓰이는 자격증을 뽑아내고 사용자가 보유한 자격증과 비교해 필요한 자격증을 추천해주는 로직을 구현, Q-net API를 통해 자격증 시험일정이나 비용을 알려줄수있도록 구현할 예정
2. 사용자의 보유 자격증, 기술스택, 희망직무, 정보에 따라 맞춤 훈련정보를 추천하는 로직 구현


2. 고용24 에서 채용 정보 받아오기
3. Q-Net API 적용
4. 사용자의 NCS 훈련기준 고려사항의 자격증 정보와 사용자의 자격증 목록과 비교하여 필요한 자격증 목록 추출
5. Q-Net에서 자격증 정보 받아오기
