package lumbermill.internal;

import lumbermill.api.Observables;
import lumbermill.api.Timer;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class TimerTest {

    /**
     * Verifies bug found
     */
    @Test
    public void test_linear_timer_attempts() {

        Timer timer = Observables.linearTimer(100).create();
        timer.next().toBlocking().subscribe();
        assertThat(timer.attempt()).isEqualTo(1);
        timer.next().toBlocking().subscribe();
        assertThat(timer.attempt()).isEqualTo(2);
    }

}
