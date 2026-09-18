package dev.programmers.runner.core.parser;

import dev.programmers.runner.core.domain.*;
import dev.programmers.runner.core.error.*;
import dev.programmers.runner.core.fetch.RawProblem;
import dev.programmers.runner.core.generator.JavaValueRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.util.*;
import java.util.function.Consumer;

public final class HtmlProblemParser implements ProblemParser {
    private final SignatureParser signatureParser;
    private final ValueParser values = new ValueParser();
    private final JavaValueRenderer validator = new JavaValueRenderer();
    private final Consumer<String> progress;

    public HtmlProblemParser() { this(new JavaSignatureParser(), ignored -> {}); }
    public HtmlProblemParser(SignatureParser signatureParser, Consumer<String> progress) {
        this.signatureParser = signatureParser;
        this.progress = progress;
    }

    @Override
    public Problem parse(RawProblem raw) {
        Document doc = Jsoup.parse(raw.html());
        progress.accept("Java 메서드 시그니처 분석 중...");
        String source = source(doc);
        MethodSignature method = signatureParser.parse(source);
        progress.accept("공개 입출력 예제 분석 중...");
        List<TestCase> tests = examples(doc, method);
        return new Problem(raw.url().problemId(), title(doc, raw.url().problemId()), method, tests, source);
    }

    private String source(Document doc) {
        // 언어를 명시한 원본 코드가 사용자 편집용 textarea보다 우선합니다.
        for (Element input : doc.select("input[data-language=java][value]")) {
            Element initial = doc.getElementById("initial_code_" + input.id());
            String text = initial == null ? input.attr("value") : initial.attr("value");
            if (!text.isBlank()) return text;
        }
        var candidates = new LinkedHashSet<String>();
        for (Element element : doc.select("textarea, pre, code, input[id^=initial_code_]")) {
            String text = element.tagName().equals("input") ? element.attr("value") : element.wholeText();
            if (!text.isBlank()) candidates.add(text);
        }
        ProblemParseException failure = null;
        for (String candidate : candidates) {
            try { signatureParser.parse(candidate); return candidate; }
            catch (ProblemParseException e) { failure = e; }
        }
        if (failure != null) throw failure;
        throw new ProblemParseException(ErrorCode.SIGNATURE_PARSE_FAILED, "Java 기본 코드를 찾지 못했습니다. 페이지 구조를 확인해 주세요.");
    }

    private String title(Document doc, long id) {
        Element metadata = doc.selectFirst("[data-lesson-title]");
        if (metadata != null && !metadata.attr("data-lesson-title").isBlank()) return metadata.attr("data-lesson-title");
        Element heading = doc.selectFirst(".challenge-title");
        if (heading != null && !heading.text().isBlank()) return heading.text();
        String title = doc.title().replaceFirst("^코딩테스트 연습\\s*-\\s*", "").replaceFirst("\\s*\\|.*$", "").strip();
        return title.isEmpty() ? "문제 " + id : title;
    }

    private List<TestCase> examples(Document doc, MethodSignature method) {
        var candidates = new ArrayList<Element>();
        var headed = new ArrayList<Element>();
        List<String> names = method.parameters().stream().map(Parameter::name).toList();
        for (Element table : doc.select("table")) {
            Element first = table.selectFirst("tr");
            if (first == null) continue;
            List<String> columns = cells(first).stream().map(Element::text).map(String::strip).toList();
            if (columns.size() != names.size() + 1 || new HashSet<>(columns).size() != columns.size()
                    || !columns.containsAll(names)) continue;
            String result = columns.stream().filter(c -> !names.contains(c)).findFirst().orElse("");
            if (!Set.of("result", "return", "answer", "returns", "결과", "반환값").contains(result.toLowerCase(Locale.ROOT)))
                continue;
            candidates.add(table);
            if (hasExampleHeading(table)) headed.add(table);
        }
        List<Element> selected = headed.isEmpty() ? candidates : headed;
        if (selected.size() != 1) throw exampleError("공개 예제 표를 하나로 식별할 수 없습니다.");
        Element table = selected.get(0);
        var rows = table.select("tr");
        List<String> columns = cells(rows.get(0)).stream().map(Element::text).map(String::strip).toList();
        int resultIndex = 0;
        while (names.contains(columns.get(resultIndex))) resultIndex++;
        var tests = new ArrayList<TestCase>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            var cells = cells(rows.get(rowIndex));
            if (cells.size() != columns.size() || cells.stream().anyMatch(c -> c.hasAttr("colspan") || c.hasAttr("rowspan")))
                throw exampleError("예제 표 " + rowIndex + "행의 열 개수가 맞지 않습니다.");
            try {
                var arguments = new ArrayList<Value>();
                for (Parameter parameter : method.parameters()) {
                    Value value = values.parse(cells.get(columns.indexOf(parameter.name())).wholeText().strip());
                    validator.render(parameter.type(), value);
                    arguments.add(value);
                }
                Value expected = values.parse(cells.get(resultIndex).wholeText().strip());
                validator.render(method.returnType(), expected);
                tests.add(new TestCase(arguments, expected));
            } catch (ProblemParseException e) {
                throw new ProblemParseException(ErrorCode.EXAMPLE_PARSE_FAILED, "예제 표 " + rowIndex + "행: " + e.getMessage(), e);
            }
        }
        if (tests.isEmpty()) throw exampleError("공개 예제가 없습니다.");
        return tests;
    }

    private static List<Element> cells(Element row) {
        return row.children().stream().filter(e -> e.tagName().equals("th") || e.tagName().equals("td")).toList();
    }

    private static boolean hasExampleHeading(Element table) {
        for (Element current = table; current != null; current = current.parent()) {
            for (Element previous = current.previousElementSibling(); previous != null; previous = previous.previousElementSibling()) {
                Element heading = previous.tagName().matches("h[1-6]") ? previous : previous.select("h1,h2,h3,h4,h5,h6").last();
                if (heading != null) return heading.text().replaceAll("\\s+", "").matches("입출력예(?:제)?");
            }
        }
        return false;
    }

    private static ProblemParseException exampleError(String detail) {
        return new ProblemParseException(ErrorCode.EXAMPLE_PARSE_FAILED, detail);
    }
}
