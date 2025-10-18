# To Yourism

![main](https://github.com/user-attachments/assets/4e41307b-1241-4acf-aa25-63557e26ed62)

## To_Yoursim

프로젝트 개요

> ‘To_Yoursim’은 공공데이터 API를 활용해 국내 관광 정보를 제공하고, 사용자들이 자신의 여행 경험을 공유하며 소통할 수 있는 안드로이드 커뮤니티 애플리케이션입니다.
> 
> 
> ‘한국관광공사_국문 관광정보 서비스’ API를 연동하여 신뢰성 있는 관광 정보를 제공하고, 사용자 작성 게시글을 통해 생생한 여행 팁과 후기를 나눌 수 있는 통합 여행 정보 플랫폼을 목표로 합니다.
> 
- 개발 기간: 2024.06 ~ 2024.08 / 2025.10 (유지보수)
- 역할: 안드로이드 앱 클라이언트 개발, 서버 및 DB 설계/구축 (1인 개발)
- 주요 기술: Kotlin, Retrofit2, OkHttp3, Glide, Google Login API, Node.js, MySQL

---

### 주요 기능

1. 관광 정보 조회 (API 연동)
    - ‘한국관광공사’ API를 활용해 국내 지역별 관광지 정보 조회
2. 간편 로그인 및 사용자 인증
    - Google Login API를 연동하여 사용자 정보 수집 및 간편 로그인/회원가입
3. 게시글 관리 및 필터링
    - 사진, 내용, 지역 등을 포함한 여행 후기 게시글 작성, 조회, 수정, 삭제 (CRUD)
    - 지역, 좋아요 수, 최신순 등 다양한 조건으로 게시글 필터링
    - 메인 화면에 좋아요 수가 높은 인기 게시글 노출
4. MyPage 기능
    - 서버 DB와 연동하여 사용자 정보(닉네임, 프로필 이미지 등) 조회 및 수정

| 메인 화면 | 게시글 목록 | 게시글 작성 |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/a8f8efd9-59c2-4eca-88a0-c309c7848c6e" width="200"/> | <img src="https://github.com/user-attachments/assets/cf93a640-b1b3-4eca-ad37-1ba6f78040ae" width="200"/> | <img src="https://github.com/user-attachments/assets/0c56b1bb-9b8d-4a67-bdcd-64f9f6ad1e24" width="200"/> | 

---

### 적용 기술 및 아키텍처

| 구분 | 내용 |
| --- | --- |
| Language | Kotlin |
| IDE | Android Studio |
| Architecture | 클라이언트-서버 아키텍처 |
| Networking | Retrofit2, OkHttp3, Gson |
| API | 한국관광공사 API, Google Login API |
| Image Handling | Glide (이미지 로딩 및 캐싱) |
| Server-Side | Node.js, Express를 활용한 REST API 구축 |
| Database | MySQL (사용자 및 게시글 테이블 설계) |
| Build | Gradle, Kapt |

---

### 주요 트러블슈팅 및 해결 과정

1. 서버 주소 중앙화 및 빌드 시점 주입
    - 문제점: 다수의 파일에 서버 IP 하드코딩 → 유지보수 어려움, 휴먼 에러 발생 가능
    - 해결 과정:
        1. gradle.properties에 SERVER_URL 정의
        2. app/build.gradle.kts에서 buildConfigField로 BuildConfig 상수 자동 생성
        3. 앱 전체 코드에서 BuildConfig.SERVER_URL 사용
    - 결과: 유지보수성 및 확장성 대폭 향상, 서버 환경 변경에 유연 대응 가능
2. JDK 버전 업그레이드로 인한 Kapt 빌드 실패
    - 문제점: IllegalAccessError 발생
    - 해결 과정:
        1. gradle.properties의 org.gradle.jvmargs에 --add-opens, --add-exports 옵션 추가
    - 결과: JDK 버전 호환성 문제 해결, 안정적인 빌드 환경 확보

### Demo

[https://drive.google.com/file/d/1NNMVC0Y0opql5kCuGr4rfVJsm1uY81aP/view?usp=drive_link](https://drive.google.com/file/d/1NNMVC0Y0opql5kCuGr4rfVJsm1uY81aP/view?usp=drive_link)

[https://drive.google.com/file/d/1nsz0D2c6fvP9uSUX_HBtvQIMcl69QqzS/view?usp=drive_link](https://drive.google.com/file/d/1nsz0D2c6fvP9uSUX_HBtvQIMcl69QqzS/view?usp=drive_link)
