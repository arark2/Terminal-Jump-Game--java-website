TERMINAL JUMP (Java + TeaVM)

1. 압축을 풀고 IntelliJ의 File > Open에서 build.gradle을 선택한다.
2. Open as Project / Trust Project가 나오면 선택한다.
3. File > Settings에서 Gradle을 검색한다.
4. Distribution은 gradle-wrapper.properties를 선택하고 Gradle JVM은 JDK 21을 선택한다.
   JDK 21이 없으면 Download JDK에서 Version 21을 내려받는다.
   Project Structure의 Project SDK도 21로 설정한다.
5. View > Tool Windows > Gradle을 연다.
6. Gradle 창의 Sync / Reload All Gradle Projects를 누른다.
   Gradle과 TeaVM 라이브러리는 인터넷으로 자동 다운로드된다.
7. Gradle 창에서 terminal-jump > Tasks > build > build를 더블클릭한다.
8. BUILD SUCCESSFUL이 나오면 build/libs/terminal-jump.war를 찾는다.
9. war 파일을 복사하고 확장자를 zip으로 바꾼 뒤 압축을 푼다.
10. 풀린 폴더의 index.html을 크롬으로 연다.
    js/game.js, style.css, assets를 같은 상대 경로로 유지한다.

원본 src/main/webapp/index.html은 변환 전이므로 바로 열지 않는다.
GameManager.main 옆의 실행 버튼 대신 Gradle build 태스크를 사용한다.
브라우저 확인 후 공개 주소가 필요하면 출력된 웹 파일들을 웹 호스팅에 올린다.

OOP 네 가지 특징은 OOP설명.txt 참고.
게임 소스 전체의 Java 컴파일 및 15개 로직 검사는 통과했다.
이번 수정에는 Gradle Wrapper 8.14.3과 브라우저 진입점/스크립트 경로 수정이 포함된다.
이 환경에서 Gradle 전체 빌드는 외부 플러그인 의존성 해결 단계에서 실패했다.
따라서 TeaVM 변환 완료와 실제 브라우저 실행 성공을 보증한 상태는 아니다.
