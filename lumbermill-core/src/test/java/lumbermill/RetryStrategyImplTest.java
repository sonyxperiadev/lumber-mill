package lumbermill;

import lumbermill.internal.MapWrap;
import lumbermill.internal.RetryStrategyImpl;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class RetryStrategyImplTest {

    @Test
    public void test_verify_retry_invoked_correct_number_of_times_when_not_specified_class() throws InterruptedException {
        assertThat(run_and_return_nr_of_invocations(new RuntimeException(), withFixedDelayFunction(2)))
                .isEqualTo(2);

        assertThat(run_and_return_nr_of_invocations(new RuntimeException(), withExponentialBackoff(2)))
                .isEqualTo(2);

        assertThat(run_and_return_nr_of_invocations(new RuntimeException(), withLinearDelay(2)))
                .isEqualTo(2);
    }

    @Test
    public void test_retry_not_invoked_when_throw_superclass_of_specified_class() throws InterruptedException {
        assertThat(run_and_return_nr_of_invocations(new RuntimeException(),
                withFixedDelayFunction(2,IllegalStateException.class)))
                .isEqualTo(1);

        assertThat(run_and_return_nr_of_invocations(new RuntimeException(),
                withExponentialBackoff(2,IllegalStateException.class)))
                .isEqualTo(1);

        assertThat(run_and_return_nr_of_invocations(new RuntimeException(),
                withLinearDelay(2,IllegalStateException.class)))
                .isEqualTo(1);
    }

    @Test
    public void test_retry_is_invoked_when_throw_same_as_specified_class() throws InterruptedException {
        assertThat(run_and_return_nr_of_invocations(new IllegalStateException(),
                withFixedDelayFunction(4,IllegalStateException.class)))
                .isEqualTo(4);

        assertThat(run_and_return_nr_of_invocations(new IllegalStateException(),
                withExponentialBackoff(4,IllegalStateException.class)))
                .isEqualTo(4);

        assertThat(run_and_return_nr_of_invocations(new IllegalStateException(),
                withLinearDelay(4,IllegalStateException.class)))
                .isEqualTo(4);
    }

    @Test
    public void test_retry_is_invoked_when_throw_subclass_of_specified_class() throws InterruptedException {
        assertThat(run_and_return_nr_of_invocations(new IllegalStateException(),
                withFixedDelayFunction(2, RuntimeException.class)))
                .isEqualTo(2);

        assertThat(run_and_return_nr_of_invocations(new IllegalStateException(),
                withExponentialBackoff(4, RuntimeException.class)))
                .isEqualTo(4);

        assertThat(run_and_return_nr_of_invocations(new IllegalStateException(),
                withLinearDelay(4, RuntimeException.class)))
                .isEqualTo(4);
    }


    private Func1<Observable<? extends Throwable>, Observable<?>> withFixedDelayFunction(
            int withAttempts, Class<? extends Throwable>... retryOn) {
        return new RetryStrategyImpl()
                .retryOn(asList(retryOn))
                .withFixedDelay(MapWrap.of("attempts", withAttempts, "delay", 100).toMap());
    }

    private Func1<Observable<? extends Throwable>, Observable<?>> withExponentialBackoff(int withAttempts, Class<? extends Throwable>... retryOn) {
       return new RetryStrategyImpl()
                .retryOn(asList(retryOn))
                .withExponentialDelay(MapWrap.of(
                        "attempts", withAttempts,
                        "seed", 0.01).toMap());
    }

    private Func1<Observable<? extends Throwable>, Observable<?>> withLinearDelay(int withAttempts, Class<? extends Throwable>... retryOn) {
        return new RetryStrategyImpl()
                .retryOn(asList(retryOn))
                .withLinearDelay(MapWrap.of(
                        "attempts", withAttempts,
                        "delay", 100).toMap());
    }
    /**
     * Runs the command. Note since it will always invoke at least ONCE, the minimal number that will be returned
     * is 1, meaning one attempt was performed.
     */
    private int run_and_return_nr_of_invocations(RuntimeException toThrow,
                                                 Func1<Observable<? extends Throwable>, Observable<?>> retryFunction) {

        AtomicInteger attempts     = new AtomicInteger(0);
        AtomicInteger firstAttempt = new AtomicInteger(0);

        try {
            Observable.just("hello","world")
                    .buffer(2)
                    .doOnNext(s3 -> firstAttempt.incrementAndGet())
                    .cache()
                    .doOnNext( s1 -> attempts.incrementAndGet())
                    .doOnNext( s2 -> {throw toThrow;})
                    .retryWhen (retryFunction)
                    .toBlocking()
                    .subscribe();
        } catch (Throwable e) {
            if (!toThrow.getClass().equals(e.getClass())) {
                // Indicates that we got another exception than the one we asked to throw.
                // This could also indicate that the test has been changed so this exception check
                // does not work any more. Check!
                e.printStackTrace();
                throw e;
            }
        }
        assertThat(firstAttempt.get()).isEqualTo(1);
        return attempts.get();
    }
}
