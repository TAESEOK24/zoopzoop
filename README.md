# ZoopZoop

ZoopZoop은 복지 정책과 공공 지원 제도를 탐색하고, 사용자 상황에 맞는 정책을 추천받을 수 있도록 만든 웹 서비스입니다.

이 프로젝트는 다음 기능을 중심으로 구성되어 있습니다.

- 정책 검색 및 상세 조회
- 사용자 활동 기반 개인화 추천
- AI 기반 정책 안내 챗봇
- 커뮤니티 게시글 및 댓글
- 회원가입, 로그인, 마이페이지

저장소는 React 프론트엔드와 Spring Boot 백엔드로 분리되어 있습니다.

## 프로젝트 구조

```text
zoopzoop/
├─ Front/
│  ├─ client/      # React 웹 애플리케이션
│  └─ src/         # 별도 Java 파일 영역 (메인 웹앱 아님)
└─ Back/           # Spring Boot 백엔드
```

## 기술 스택

프론트엔드

- React 18
- react-router-dom
- axios
- Tailwind CSS
- lucide-react

백엔드

- Java 25 toolchain
- Spring Boot 3
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT 인증
- Spring AI / OpenAI 연동

## 주요 기능

### 1. 정책 검색 및 조회

사용자는 다음 기능을 사용할 수 있습니다.

- 키워드 검색
- 정책 유형 필터링
- 연령, 지역, 특수 조건 필터링
- 정책 상세 정보 조회

관련 프론트 파일

- `Front/client/src/pages/Policy`
- `Front/client/src/api/policies.js`

관련 백엔드 파일

- `Back/src/main/java/com/zoopzoop/zoopzoop/domain/policy`

### 2. 개인화 추천

백엔드는 사용자의 최근 검색 기록과 조회 기록을 바탕으로 추천 정책 목록을 생성합니다.

관련 파일

- `Front/client/src/api/recommendations.js`
- `Back/src/main/java/com/zoopzoop/zoopzoop/domain/recommendation`

### 3. AI 챗봇

백엔드에는 정책 탐색용 챗봇 로직과 OpenAI 연동 로직이 포함되어 있습니다.

관련 파일

- `Front/client/src/pages/AIChat`
- `Front/client/src/api/chatbot.js`
- `Back/src/main/java/com/zoopzoop/zoopzoop/domain/chatbot`

주의

- 현재 프론트 챗봇 화면은 페이지 내부에서 목업 응답을 사용하고 있습니다.
- 백엔드 챗봇 API는 별도로 구현되어 있습니다.
- 즉, UI와 실제 챗봇 API 연결은 아직 완전히 정리된 상태가 아닙니다.

### 4. 커뮤니티

사용자는 다음 기능을 사용할 수 있습니다.

- 게시글 목록 조회
- 게시글 작성, 수정, 삭제
- 댓글 조회, 작성, 수정, 삭제

관련 파일

- `Front/client/src/pages/Community`
- `Front/client/src/api/community.js`
- `Back/src/main/java/com/zoopzoop/zoopzoop/domain/community`

### 5. 인증 및 마이페이지

사용자는 다음 기능을 사용할 수 있습니다.

- 회원가입
- 로그인
- JWT 기반 인증
- 사용자 정보 조회

관련 파일

- `Front/client/src/pages/Login`
- `Front/client/src/pages/Signup`
- `Front/client/src/pages/MyPage`
- `Back/src/main/java/com/zoopzoop/zoopzoop/domain/auth`
- `Back/src/main/java/com/zoopzoop/zoopzoop/domain/user`

## 실행 방법

### 1. 프론트엔드 실행

프론트 앱 경로로 이동합니다.

```bash
cd Front/client
```

의존성을 설치합니다.

```bash
npm install
```

개발 서버를 실행합니다.

```bash
npm start
```

기본 주소

- `http://localhost:3000`

### 2. 백엔드 실행

백엔드 경로로 이동합니다.

```bash
cd Back
```

Gradle Wrapper로 실행합니다.

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat bootRun
```

기본 주소

- `http://localhost:8080`

## 설정 파일

주요 백엔드 설정 파일은 아래 경로에 있습니다.

- `Back/src/main/resources/application.properties`
- `Back/src/main/resources/application-local.properties`
- `Back/src/main/resources/application-dev.properties`

현재 코드 기준으로 백엔드는 다음 항목에 의존합니다.

- PostgreSQL 데이터베이스
- JWT 설정값
- OpenAI API Key
- 정부 정책 데이터 연동용 API Key

## 현재 구현 상태

현재 코드 기준으로 보면 다음 정도까지 구현되어 있습니다.

- 정책 목록 조회 및 필터링 구현
- 정책 상세 조회 API 구현
- 커뮤니티 게시글 / 댓글 CRUD 구현
- 회원가입 / 로그인 구현
- 추천 로직 백엔드 구현
- 챗봇 로직 백엔드 구현

다만 일부 화면은 아직 완전히 연결되지 않았습니다.

## 현재 확인된 미완성 / 불일치 지점

- 프론트 정책 상세 라우팅 경로가 일관되지 않습니다.
- 챗봇 화면이 실제 백엔드 API 대신 목업 응답을 사용합니다.
- 마이페이지의 활동 데이터 일부가 하드코딩되어 있습니다.
- 한글 문자열 일부에 인코딩 문제가 보입니다.
- 민감한 설정값이 properties 파일에 직접 들어가 있어 환경변수 분리가 필요합니다.

## 참고 사항

- `Front/.readme` 파일은 예전 초안으로 보이며, 현재 저장소 구조와 정확히 일치하지 않습니다.
- `Front/client/node_modules`, `Back/build` 같은 산출물 디렉터리는 소스 코드가 아닙니다.
