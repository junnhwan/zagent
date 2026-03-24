package io.wanjune.zagent.agent.strategy.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class StrategyHelperTest {

    @Test
    void abbreviateForLogReturnsNullPlaceholder() {
        assertThat(StrategyHelper.abbreviateForLog(null)).isEqualTo("<null>");
    }

    @Test
    void abbreviateForLogTruncatesLongText() {
        String longText = "a".repeat(300);
        String result = StrategyHelper.abbreviateForLog(longText);
        assertThat(result).hasSizeLessThanOrEqualTo(210);
        assertThat(result).endsWith("...(截断)");
    }

    @Test
    void abbreviateForLogKeepsShortText() {
        assertThat(StrategyHelper.abbreviateForLog("short")).isEqualTo("short");
    }

    @Test
    void sendStageEventHandlesNullEmitter() {
        assertThatNoException().isThrownBy(() ->
                StrategyHelper.sendStageEvent(null, "test", "done", 1, 1, "content", "session1"));
    }

    @Test
    void sendStageEventHandlesNullEmitterWithPayload() {
        assertThatNoException().isThrownBy(() ->
                StrategyHelper.sendStageEvent(null, "test", "done", 1, 1, "content", null, "session1"));
    }

    @Test
    void getStepPromptReturnsDefaultWhenMapNull() {
        assertThat(StrategyHelper.getStepPrompt(null, "key", "default")).isEqualTo("default");
    }

    @Test
    void getStepPromptReturnsDefaultWhenKeyMissing() {
        assertThat(StrategyHelper.getStepPrompt(java.util.Map.of(), "key", "default")).isEqualTo("default");
    }

    @Test
    void getStepPromptReturnsDbValue() {
        assertThat(StrategyHelper.getStepPrompt(java.util.Map.of("key", "db_value"), "key", "default"))
                .isEqualTo("db_value");
    }

    @Test
    void getStepPromptReturnsDefaultWhenBlank() {
        assertThat(StrategyHelper.getStepPrompt(java.util.Map.of("key", "  "), "key", "default"))
                .isEqualTo("default");
    }
}
