package lumbermill.aws.lambda;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LambdaWithConfigTest {
    @Test(expected = IllegalStateException.class)
    public void testMissingFile() {
        new LambdaWithConfig("missingConfig.json").getConfig("foo", null);
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingKey() {
        new LambdaWithConfig("test-config.json").getConfig("missing", null);
    }

    @Test
    public void testKey() {
        assertThat(new LambdaWithConfig("test-config.json").getConfig("foo", null)).isEqualTo("bar");
    }

    @Test
    public void testDefaultValue() {
        assertThat(new LambdaWithConfig("test-config.json").getConfig("missing", "default")).isEqualTo("default");
    }
}
