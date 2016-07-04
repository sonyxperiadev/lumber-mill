package lumbermill;

/*
 * Copyright 2016 Sony Mobile Communications, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import lumbermill.api.Observables;
import lumbermill.api.Timer;
import lumbermill.internal.ExponentialTimer;
import lumbermill.internal.FixedTimer;
import lumbermill.internal.LinearTimer;
import lumbermill.internal.MapWrap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservablesTest {

    @Test
    public void test_create_linear_timer_from_config() {
        Timer.Factory timer = Observables.timer(MapWrap.of("policy", "linear", "delayMs", 100));
        assertThat(timer).isInstanceOf(LinearTimer.class);

    }

    @Test
    public void test_create_fixed_timer_from_config() {
        Timer.Factory timer = Observables.timer(MapWrap.of("policy", "fixed", "delayMs", 100));
        assertThat(timer).isInstanceOf(FixedTimer.class);

    }

    @Test
    public void test_create_exponential_timer_from_config() {
        Timer.Factory timer = Observables.timer(MapWrap.of("policy", "exponential", "delayMs", 1100));
        assertThat(timer).isInstanceOf(ExponentialTimer.class);

    }
}
