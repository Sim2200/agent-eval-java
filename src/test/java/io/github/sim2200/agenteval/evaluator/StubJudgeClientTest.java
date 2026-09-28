package io.github.sim2200.agenteval.evaluator;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StubJudgeClientTest {

  private StubJudgeClient client;

  @BeforeEach
  void setUp() {
    client = new StubJudgeClient();
  }

  @Test
  void expectsHintsAllPresent() {
    String rubric = "expects: apple, orange, banana";
    String output = "I got apple, orange, and banana";

    JudgeClient.Verdict verdict = client.judge("input", output, rubric);

    assertThat(verdict.satisfied()).isTrue();
  }

  @Test
  void expectsHintsMissingOne() {
    String rubric = "expects: apple, orange, banana";
    String output = "I got apple and orange";

    JudgeClient.Verdict verdict = client.judge("input", output, rubric);

    assertThat(verdict.satisfied()).isFalse();
    assertThat(verdict.reason()).contains("banana");
  }

  @Test
  void rejectsHintPresent() {
    String rubric = "rejects: forbidden, banned";
    String output = "This is forbidden content";

    JudgeClient.Verdict verdict = client.judge("input", output, rubric);

    assertThat(verdict.satisfied()).isFalse();
    assertThat(verdict.reason()).contains("forbidden");
  }

  @Test
  void emptyOutput() {
    String rubric = "expects: something";
    String output = "";

    JudgeClient.Verdict verdict = client.judge("input", output, rubric);

    assertThat(verdict.satisfied()).isFalse();
  }

  @Test
  void noHintsNonEmptyOutput() {
    String rubric = "Some rubric without hints";
    String output = "Any output here";

    JudgeClient.Verdict verdict = client.judge("input", output, rubric);

    assertThat(verdict.satisfied()).isTrue();
  }

  @Test
  void hintsParsingWithSemicolonSeparator() {
    String rubric = "expects: a, b; other text rejects: x, y";
    String output = "a and b";

    JudgeClient.Verdict verdict = client.judge("input", output, rubric);

    assertThat(verdict.satisfied()).isTrue();
  }

  @Test
  void caseInsensitiveHints() {
    String rubric = "expects: APPLE, ORANGE";
    String output = "I have apple and orange";

    JudgeClient.Verdict verdict = client.judge("input", output, rubric);

    assertThat(verdict.satisfied()).isTrue();
  }
}
