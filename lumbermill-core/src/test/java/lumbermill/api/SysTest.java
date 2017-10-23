package lumbermill.api;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SysTest {

    @Test
    public void test_sys_default() {
        assertThat(Sys.env("__hope_does_not_exist", "it did not").string()).isEqualTo("it did not");
    }

    @Test
    public void test_sys_default_template() {
        assertThat(Sys.env("{__hope_does_not_exist || it did not}").string()).isEqualTo("it did not");
    }
}
