# Programmers Local Runner

프로그래머스 URL에서 Java 기본 코드와 공개 입출력 예제를 가져와 `Solution.java`, JUnit 5 매개변수화 테스트를 생성하는 CLI입니다.

## 빠른 시작

JDK 17 이상이 필요합니다. Gradle 8.13 Wrapper를 포함하며, 최초 빌드 시 인터넷 연결이 필요합니다. JDK 21에서 빌드·검증했습니다.

프로젝트 루트에서 실행합니다.

```bash
./gradlew test :programmers-cli:installDist
export PATH="$PWD/programmers-cli/build/install/programmers/bin:$PATH"

programmers import \
  'https://school.programmers.co.kr/learn/courses/30/lessons/12909' \
  --output examples/java-workspace
```

생성되는 파일:

```text
examples/java-workspace/
├── src/main/java/programmers/p12909/Solution.java
└── src/test/java/programmers/p12909/SolutionTest.java
```

`Solution.java`에 풀이를 작성하고 프로젝트 루트에서 실행합니다.

```bash
./gradlew -p examples/java-workspace test
```

예제 워크스페이스의 JUnit 실행 환경은 이미 준비되어 있습니다. 생성 직후에는 기본 구현이 정답이 아니므로 일부 테스트가 실패하는 것이 정상입니다. 생성한 테스트 코드는 수정할 필요가 없습니다.

기존 Gradle Java 프로젝트에는 `--output /path/to/project`를 지정하세요. 해당 프로젝트에서 `./gradlew test` 또는 IDE의 JUnit 실행 기능을 사용하면 됩니다. 여러 Gradle 모듈이 있다면 Java 소스가 들어갈 **모듈 디렉터리**를 출력 경로로 지정합니다.

Windows에서는 `gradlew.bat`와 배포 디렉터리의 `programmers.bat`를 사용합니다. Windows 실행은 별도로 검증하지 않았습니다.

## CLI

```text
programmers import <URL> [--output <경로>] [--force] [--debug]
programmers --help
programmers --version
```

| 옵션 | 동작 |
| --- | --- |
| `--output` | 생성 기준 경로. 기본값은 현재 디렉터리 |
| `--force` | 해당 문제의 두 생성 파일을 덮어쓰기 |
| `--debug` | 조회·시그니처 분석·예제 분석·생성 로그 및 오류 상세 출력 |

URL에 다른 언어 쿼리나 fragment가 있어도 검증 후 Java 페이지를 요청합니다. HTTPS의 `school.programmers.co.kr/learn/courses/{courseId}/lessons/{problemId}` 형식만 지원합니다.

둘 중 하나라도 기존 파일이 있으면 기본적으로 실패하며 다른 파일도 생성하지 않습니다. `--force`는 작성한 풀이까지 교체하므로 재생성이 필요할 때 사용하세요. 생성 파일 준비 중 실패하면 기존 파일을 유지하고, 교체 중 입출력 오류가 발생하면 이미 변경한 파일의 복구를 시도합니다. 프로세스 강제 종료나 다른 프로세스의 동시 수정까지 보장하는 트랜잭션은 아닙니다.

종료 코드는 성공 `0`, Import 실패 `1`, 명령 사용 오류 `2`입니다.

## 지원 범위

- Java의 `Solution.solution(...)` 함수형 문제
- `int`, `long`, `double`, `boolean`, `String`
- 위 타입의 1차원·2차원 배열, 빈 배열, 가변 길이의 중첩 배열
- 참조 타입의 `null`, 다중 매개변수
- 공개 **입출력 예** 표 기반 JUnit 5 `@ParameterizedTest`
- Java 기본 코드의 본문·주석·import 유지, 패키지 추가 및 `Solution` 클래스 공개 처리
- 기본 코드 없는 직접 생성용 IR에는 반환 타입에 맞는 초기 구현 생성

`double`과 `double[]`은 JUnit의 정확한 값 비교를 사용합니다. 2차원 배열은 JUnit `assertArrayEquals(Object[], Object[])`의 중첩 비교를 사용합니다. 오차 허용 범위의 자동 추론은 하지 않습니다.

로그인, 제출, 정답 조회, 숨겨진 테스트, 표준입출력형 `main` 문제, 제네릭·사용자 정의 타입·3차원 배열은 지원하지 않습니다.

`programmers-vscode`는 이 CLI를 감싸는 VSCode 확장입니다. Command Palette에서 `Programmers: Import Problem` / `Programmers: Import Problem from Clipboard`를 실행할 수 있으며, 사용법은 [programmers-vscode/README.md](programmers-vscode/README.md)를 참고하세요.

## 기존 프로젝트의 Gradle 설정

기존 빌드 파일은 자동으로 수정하지 않습니다. JUnit 5가 없다면 Java 플러그인과 다음 설정을 추가하세요. 이미 설정했다면 그대로 사용합니다.

```groovy
repositories { mavenCentral() }

dependencies {
    testImplementation platform('org.junit:junit-bom:5.10.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test { useJUnitPlatform() }
```

## 구조

```text
programmers-core
├── domain     Problem / MethodSignature / Type / Value
├── fetch      ProblemFetcher → RawProblem
├── parser     HTML / Java 시그니처 / 예제 값 분석
├── generator  Java 및 JUnit 소스 생성
└── writer     경로 검증, 충돌 보호 및 파일 저장

programmers-cli → programmers-core
programmers-vscode        CLI를 감싸는 VSCode 확장 (npm/TypeScript, Gradle 빌드 대상 아님)
examples/java-workspace   풀이용 Gradle 프로젝트
```

Core에는 CLI 및 VSCode API 의존성이 없습니다. `ProblemImporter`가 단계들을 연결하며 Fetcher, Parser, Writer는 인터페이스로 교체할 수 있습니다.

### 파서 정책

1. HTTP 조회 시 `language=java`를 지정합니다. 연결 제한은 10초, 요청 제한은 30초입니다.
2. Java 언어가 명시된 코드 요소와 원본 `initial_code`를 우선 사용합니다. 없으면 textarea 및 코드 블록을 검사합니다.
3. JavaParser의 구문 트리에서 `Solution.solution`의 반환형·매개변수를 읽습니다. Java 기본 코드를 확보할 수 없으면 명시적으로 실패합니다. 설명문에서 타입을 추측하는 기능은 후속 확장 지점입니다.
4. 표의 컬럼 이름을 시그니처와 대조하고 `입출력 예` 제목을 함께 확인합니다. 컬럼 순서가 달라도 이름으로 매칭하며, 후보 표가 모호하면 실패합니다.
5. 예제 값을 JSON 형태의 값 트리로 변환한 뒤 시그니처의 타입·숫자 범위를 검증합니다. 문자열 내부 공백을 유지하고 Java 문자열 이스케이프를 적용합니다.

페이지 구조가 바뀌면 `HtmlProblemParser`와 HTML fixture 테스트를 먼저 확인하세요. 요청이 차단되거나 로그인이 필요한 문제는 `FETCH_FAILED` 또는 파싱 오류로 보고합니다.

## 오류 코드

| 코드 | 의미 |
| --- | --- |
| `INVALID_URL` | 지원 URL 형식이 아님 |
| `FETCH_FAILED` | 네트워크, 시간 초과, HTTP 또는 응답 형식 오류 |
| `UNSUPPORTED_PROBLEM` | 지원하지 않는 Java 타입·문제 형태 |
| `SIGNATURE_PARSE_FAILED` | Java 기본 코드나 단일 solution 메서드를 식별할 수 없음 |
| `EXAMPLE_PARSE_FAILED` | 예제 표·값·타입·범위 불일치 |
| `FILE_ALREADY_EXISTS` | 기존 생성 파일 존재 |
| `FILE_WRITE_FAILED` | 경로·권한·파일 저장 오류 |

## 검증

```bash
./gradlew test
./gradlew :programmers-cli:installDist
./gradlew :programmers-cli:distZip
```

테스트는 외부 네트워크를 사용하지 않습니다.

- URL·시그니처·값 파서와 타입별 Java 렌더링
- 실제 문제 `12909`, `150370`, `468381`의 최소 HTML fixture 회귀 검증
- 생성 코드 스냅샷 비교
- 15가지 지원 타입의 생성 소스를 Java 컴파일러로 컴파일하고 JUnit 실행
- 실제 `12909`, `150370` 문제에서 Solution만 구현해 공개 예제 통과 확인
- `468381`의 6개 테스트 생성·컴파일·실행 및 기본 구현의 예상 실패 확인
- 중첩 배열의 잘못된 결과가 실제 assertion 실패로 검출되는지 확인
- HTTP 오류·중단·CLI 옵션·기존 파일 보호·쓰기 실패·심볼릭 링크 경로 검증

테스트 보고서는 각 모듈의 `build/reports/tests/test/index.html`에 생성됩니다.

HTML fixture 출처와 관리 기준은 [fixture 안내](programmers-core/src/test/resources/problems/README.md)에 있습니다.
