package io.github.sim2200.agenteval.evaluator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Picks the judge: Gemini when a key is present in the environment, the stub otherwise. */
@Configuration
public class JudgeConfig {

  private static final Logger log = LoggerFactory.getLogger(JudgeConfig.class);

  @Bean
  public JudgeClient judgeClient(
      @Value("${GEMINI_API_KEY:}") String apiKey,
      @Value("${agenteval.judge.model:gemini-2.5-flash}") String model) {
    if (apiKey == null || apiKey.isBlank()) {
      log.info("GEMINI_API_KEY not set: llm_judge checks use the deterministic stub judge");
      return new StubJudgeClient();
    }
    log.info("llm_judge checks use Gemini model {}", model);
    return new GeminiJudgeClient(apiKey, model);
  }
}
