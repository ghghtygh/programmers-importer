# 실제 HTML 회귀 fixture

2026-09-15에 아래 공개 Java 문제 페이지에서 파싱에 필요한 **제목, 입출력 표, Java 기본 코드 요소**만 발췌했습니다. 문제 설명, 해설, 이미지, 추적 스크립트 및 사용자별 데이터는 포함하지 않습니다.

| 파일 | 원본 페이지 | 검증 대상 |
| --- | --- | --- |
| `12909.html` | https://school.programmers.co.kr/learn/courses/30/lessons/12909?language=java | String 입력, boolean 반환, 기본 접근 제한자 |
| `150370.html` | https://school.programmers.co.kr/learn/courses/30/lessons/150370?language=java | 다중 인수, 문자열 배열, int 배열 반환 |
| `468381.html` | https://school.programmers.co.kr/learn/courses/30/lessons/468381?language=java | int 2차원 배열, 공개 예제 6개 |

원문 콘텐츠의 권리는 원 권리자에게 있습니다. 외부 배포 시 스펙에 명시된 약관·저작권 검토는 별도로 필요합니다. 현재 파일은 로컬 개발 회귀 검증용이며 서비스의 콘텐츠 배포 허가를 의미하지 않습니다.

구조 변경 시 실제 페이지와 비교해 필요한 요소만 갱신하세요. 테스트 전용 합성 HTML은 `HtmlProblemParserTest`에 별도로 작성되어 있습니다.
