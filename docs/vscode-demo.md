# VSCode 시연 검증 기록

[시연 GIF](media/vscode-demo.gif)는 실제 VSCode 확장 개발 호스트에서 실행한 주요 화면을 순서대로 편집한 것입니다. 단계별 한국어 자막을 추가했으며, 연속 녹화의 실제 소요 시간을 나타내지는 않습니다.

## 검증 환경

- 검증일: 2026-10-11
- macOS / Apple Silicon
- VSCode 1.123.0
- OpenJDK 21.0.2
- Node.js 24.7.0
- Programmers Local Runner 0.1.0, 저장소 소스에서 빌드
- 사용자 풀이와 분리한 별도 Gradle Java 프로젝트 사용

## 결과

| 검증 항목 | 결과 |
| --- | --- |
| 저장소 전체 테스트 (`./gradlew test`) | 148개 통과, 실패·오류·건너뜀 0개 |
| CLI 빌드 (`./gradlew :programmers-cli:installDist`) | 성공 |
| 확장 의존성 설치 및 TypeScript 빌드 (`npm ci`, `npm run compile`) | 성공 |
| `Programmers: Import Problem` | 실제 12909 문제의 Java 기본 코드와 공개 예제 4개 가져오기 성공 |
| 성공 후 파일 자동 열기 | 생성된 `Solution.java`가 자동으로 열림 |
| 생성된 테스트 확인 | JUnit 5 매개변수화 테스트의 입력·기대값 4개 확인 |
| 지원하지 않는 URL 입력 | `https://example.com/problem/12909`를 입력하면 검증 오류를 표시하고 진행을 차단 |
| `Programmers: Import Problem from Clipboard` | 클립보드의 같은 문제 URL로 실행되어 기존 파일 보호 알림 표시 |
| 중복 가져오기 | `Open` / `Overwrite` / `Cancel` 선택지 확인, `Open`으로 기존 풀이 열기 성공 |
| 기본 풀이로 생성된 테스트 실행 | 예제 4개 중 2개 실패: 항상 `true`를 반환하는 기본 코드의 오답을 검출 |
| 풀이 구현 후 VSCode 터미널에서 테스트 실행 | 예제 4개 모두 통과, 실패·오류·건너뜀 0개, `BUILD SUCCESSFUL` 확인 |

중복 알림의 `Overwrite`와 `Cancel` 선택 후 동작, Windows 환경은 이번 시연에서 직접 검증하지 않았습니다.

## 재현 방법

저장소 루트에서 CLI를 준비합니다.

```bash
./gradlew test :programmers-cli:installDist
```

확장을 빌드합니다.

```bash
cd programmers-vscode
npm ci
npm run compile
cd ..
```

풀이용 Gradle 프로젝트의 VSCode 워크스페이스 설정에서 `programmers.cliPath`를 빌드된 `programmers-cli/build/install/programmers/bin/programmers`의 **절대 경로**로 지정합니다. 예제 프로젝트를 사용할 경우 `.vscode/settings.json`에 설정하면 됩니다.

저장소 루트에서 개발 호스트를 실행합니다.

```bash
code --new-window \
  --extensionDevelopmentPath "$PWD/programmers-vscode" \
  "$PWD/examples/java-workspace"
```

1. 개발 호스트의 명령 팔레트에서 `Programmers: Import Problem`을 실행합니다.
2. `https://school.programmers.co.kr/learn/courses/30/lessons/12909`를 입력합니다.
3. `src/main/java/programmers/p12909/Solution.java`와 `src/test/java/programmers/p12909/SolutionTest.java`를 확인합니다.
4. 같은 URL을 복사하고 `Programmers: Import Problem from Clipboard`를 실행하면 기존 파일 보호 선택지를 확인할 수 있습니다. `Open`을 선택하면 기존 풀이가 열립니다.
5. 생성된 테스트는 유지하고 `Solution.java`에 풀이를 작성합니다.
6. 저장소 루트에서 `./gradlew -p examples/java-workspace test`를 실행합니다.

시연에서는 별도 프로젝트에 Gradle Wrapper를 복사하여 VSCode 터미널에서 `./gradlew test --rerun-tasks --console=plain`을 실행했습니다. 테스트 결과는 해당 프로젝트의 `build/reports/tests/test/index.html`에서 확인할 수 있습니다.
