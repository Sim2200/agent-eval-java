package io.github.sim2200.agenteval.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A deterministic stand-in for the LLM judge, used when no API key is configured and in tests.
 *
 * <p>Rubrics can carry hints in the form {@code expects: a, b, c} (all must appear in the output)
 * and {@code rejects: x, y} (none may appear); without hints the stub passes any non-empty output.
 * It is deliberately simple: the point is that suites, scoring, persistence and the API behave
 * identically whether the judge is real or not.
 */
public class StubJudgeClient implements JudgeClient {

  @Override
  public String name() {
    return "stub";
  }

  @Override
  public Verdict judge(String input, String output, String rubric) {
    String lower = output == null ? "" : output.toLowerCase(Locale.ROOT);
    for (String term : hints(rubric, "expects:")) {
      if (!lower.contains(term)) {
        return new Verdict(false, "missing expected content \"" + term + "\"");
      }
    }
    for (String term : hints(rubric, "rejects:")) {
      if (lower.contains(term)) {
        return new Verdict(false, "contains rejected content \"" + term + "\"");
      }
    }
    if (lower.isBlank()) {
      return new Verdict(false, "empty output");
    }
    return new Verdict(true, "stub judge: rubric hints satisfied");
  }

  static List<String> hints(String rubric, String key) {
    String lower = rubric.toLowerCase(Locale.ROOT);
    int at = lower.indexOf(key);
    if (at < 0) {
      return List.of();
    }
    int end = lower.indexOf(';', at);
    String list = lower.substring(at + key.length(), end < 0 ? lower.length() : end);
    return Arrays.stream(list.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }
}
