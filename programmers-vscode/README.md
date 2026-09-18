# Programmers Local Runner (VSCode Extension)

프로그래머스 문제 URL로부터 `Solution.java`와 JUnit 5 테스트를 자동 생성하는 `programmers` CLI를 VSCode Command Palette에서 바로 실행할 수 있게 해줍니다.

이 확장은 문제 페이지 파싱 로직을 직접 갖지 않습니다. 모든 분석과 코드 생성은 `programmers-core` / `programmers-cli`가 담당하며, 확장은 CLI를 호출하고 결과를 사용자에게 보여주는 얇은 래퍼입니다.

## 명령

Command Palette(`Cmd+Shift+P` / `Ctrl+Shift+P`)에서 실행합니다.

- `Programmers: Import Problem` — URL을 직접 입력해 Import
- `Programmers: Import Problem from Clipboard` — 클립보드에 복사된 문제 URL을 바로 Import

Import가 성공하면 `Solution.java`가 자동으로 열립니다. 이미 생성된 문제라면 `Open` / `Overwrite` / `Cancel` 중 선택할 수 있습니다.

## CLI 준비

이 확장은 `programmers` CLI 실행 파일을 필요로 합니다. 다음 순서로 찾습니다.

1. 설정 `programmers.cliPath`에 지정한 경로
2. 열려 있는 워크스페이스의 `programmers-cli/build/install/programmers/bin/programmers` (이 저장소를 직접 연 경우)
3. PATH에 등록된 `programmers`

CLI를 찾지 못하면 저장소 루트에서 `./gradlew :programmers-cli:installDist`를 실행할지 물어봅니다.

## 설정

| 설정 | 설명 | 기본값 |
| --- | --- | --- |
| `programmers.cliPath` | CLI 실행 파일 경로를 직접 지정 | 빈 값 (자동 탐색) |
| `programmers.outputPath` | 생성 기준 경로 (워크스페이스 루트 기준 상대 경로) | 빈 값 (워크스페이스 루트) |
| `programmers.openSolutionAfterImport` | Import 후 Solution.java 자동 열기 | `true` |

## 개발

```bash
npm install
npm run compile
```

`F5`(Run Extension)로 Extension Development Host를 실행해 확인할 수 있습니다.
