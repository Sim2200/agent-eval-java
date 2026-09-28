package io.github.sim2200.agenteval.suite;

import static org.assertj.core.api.Assertions.*;

import io.github.sim2200.agenteval.suite.SuiteParser.SuiteFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuiteParserTest {

  private SuiteParser parser;

  @BeforeEach
  void setUp() {
    parser = new SuiteParser();
  }

  @Test
  void parsesExampleFromJavadoc() {
    String yaml =
        """
        id: booking-agent
        name: Customer-service booking agent
        cases:
          - id: b-001
            input: "I need to move my flight to Friday"
            output: "I can help reschedule to Friday"
            checks:
              - type: contains
                text: "Friday"
              - type: must_not_fire
                phrases: ["refund"]
                should_pass: false
        """;

    SuiteParser.Suite suite = parser.parse(yaml);

    assertThat(suite.id()).isEqualTo("booking-agent");
    assertThat(suite.name()).isEqualTo("Customer-service booking agent");
    assertThat(suite.cases()).hasSize(1);

    assertThat(suite.cases().get(0).id()).isEqualTo("b-001");
    assertThat(suite.cases().get(0).checks()).hasSize(2);
    assertThat(suite.cases().get(0).checks().get(1).shouldPass()).isFalse();
  }

  @Test
  void parsingBookingAgent() throws Exception {
    String yaml = Files.readString(Path.of("suites/booking-agent.yaml"));
    SuiteParser.Suite suite = parser.parse(yaml);

    assertThat(suite.cases().size()).isGreaterThan(0);
    for (var testCase : suite.cases()) {
      for (var check : testCase.checks()) {
        assertThat(check.type())
            .isIn("contains", "regex", "json_schema", "must_not_fire", "llm_judge");
      }
    }
  }

  @Test
  void parsingToolCallingAgent() throws Exception {
    String yaml = Files.readString(Path.of("suites/tool-calling-agent.yaml"));
    SuiteParser.Suite suite = parser.parse(yaml);

    assertThat(suite.cases().size()).isGreaterThan(0);
    for (var testCase : suite.cases()) {
      for (var check : testCase.checks()) {
        assertThat(check.type())
            .isIn("contains", "regex", "json_schema", "must_not_fire", "llm_judge");
      }
    }
  }

  @Test
  void parsingSummarizerAgent() throws Exception {
    String yaml = Files.readString(Path.of("suites/summarizer.yaml"));
    SuiteParser.Suite suite = parser.parse(yaml);

    assertThat(suite.cases().size()).isGreaterThan(0);
    for (var testCase : suite.cases()) {
      for (var check : testCase.checks()) {
        assertThat(check.type())
            .isIn("contains", "regex", "json_schema", "must_not_fire", "llm_judge");
      }
    }
  }

  @Test
  void invalidYaml() {
    assertThatThrownBy(() -> parser.parse("not: [valid yaml: "))
        .isInstanceOf(SuiteFormatException.class)
        .hasMessageContaining("YAML");
  }

  @Test
  void missingId() {
    String yaml =
        """
        name: Test
        cases:
          - id: c1
            output: "test"
            checks:
              - type: contains
                text: "test"
        """;

    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(SuiteFormatException.class)
        .hasMessageContaining("id");
  }

  @Test
  void noCases() {
    String yaml =
        """
        id: test
        cases: []
        """;

    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(SuiteFormatException.class)
        .hasMessageContaining("cases");
  }

  @Test
  void duplicateCaseId() {
    String yaml =
        """
        id: test
        cases:
          - id: c1
            output: "test"
            checks:
              - type: contains
                text: "test"
          - id: c1
            output: "test"
            checks:
              - type: contains
                text: "test"
        """;

    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(SuiteFormatException.class)
        .hasMessageContaining("duplicate");
  }

  @Test
  void caseWithoutChecks() {
    String yaml =
        """
        id: test
        cases:
          - id: c1
            output: "test"
            checks: []
        """;

    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(SuiteFormatException.class)
        .hasMessageContaining("checks");
  }

  @Test
  void checkWithoutType() {
    String yaml =
        """
        id: test
        cases:
          - id: c1
            output: "test"
            checks:
              - text: "test"
        """;

    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(SuiteFormatException.class)
        .hasMessageContaining("type");
  }

  @Test
  void shouldPassDefaultTrue() {
    String yaml =
        """
        id: test
        cases:
          - id: c1
            output: "test"
            checks:
              - type: contains
                text: "test"
        """;

    SuiteParser.Suite suite = parser.parse(yaml);

    assertThat(suite.cases().get(0).checks().get(0).shouldPass()).isTrue();
  }

  @Test
  void shouldPassExplicitFalse() {
    String yaml =
        """
        id: test
        cases:
          - id: c1
            output: "test"
            checks:
              - type: contains
                text: "test"
                should_pass: false
        """;

    SuiteParser.Suite suite = parser.parse(yaml);

    assertThat(suite.cases().get(0).checks().get(0).shouldPass()).isFalse();
  }

  @Test
  void transcriptAcceptedAsOutput() {
    String yaml =
        """
        id: test
        cases:
          - id: c1
            transcript: "recorded transcript"
            checks:
              - type: contains
                text: "test"
        """;

    SuiteParser.Suite suite = parser.parse(yaml);

    assertThat(suite.cases().get(0).output()).isEqualTo("recorded transcript");
  }

  @Test
  void extraCheckKeysBecomesParams() {
    String yaml =
        """
        id: test
        cases:
          - id: c1
            output: "test"
            checks:
              - type: json_schema
                schema: {"type": "object"}
                extra_param: "value"
        """;

    SuiteParser.Suite suite = parser.parse(yaml);

    assertThat(suite.cases().get(0).checks().get(0).param("extra_param")).isEqualTo("value");
  }
}
