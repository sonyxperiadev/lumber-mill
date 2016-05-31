package lumbermill;

import rx.Observable;
import rx.functions.Func1;

import java.util.Map;

/**
 * <p>Used to create retry strategies when using with Observable.retryWhen()</p>
 * It is designed to help you with the most common and simple uses cases regarding retries,
 * not with everything.
 *<br/><br/>
 * Great resources for more about retry-with that is useful
 * <br/>
 * https://gist.github.com/daschl/db9fcc9d2b932115b679#retry-with-delay
 * <br/>
 * http://blog.danlew.net/2016/01/25/rxjavas-repeatwhen-and-retrywhen-explained/
 */

public interface RetryStrategy {


    //TODO - Add possibility to exclude types as well
    // RetryStrategy excluding(Class....)

    /**
     * Creates a retry strategy with a Linear delay, simply it takes the
     * configured delay * attempt.
     *
     * The code below would retry after 100 and 200 ms.
     *
     * <pre>
     * Groovy usage:
     *  {@code
     * observable.retryWhen (
     *     exceptionOfTypes ([MyException.class, AnotherException.class])
     *          .withFixedDelay (
     *              attempts : 3,
     *              delay : 100
     *         )
     *     )
     * )
     * }
     * </pre>
     *
     * @param java.util.Map with optional 'attempts' (default = 3) and 'delay' (default = 1000)
     */
    Func1<Observable<? extends Throwable>, Observable<?>> withLinearDelay(Map map);

    /**
     * Creates a retry strategy with a fixed delay
     * <pre>
     * Groovy usage:
     *  {@code
     * observable.retryWhen (
     *     exceptionOfTypes ([MyException.class, AnotherException.class])
     *          .withFixedDelay (
     *              attempts : 3,
     *              delay : 100
     *         )
     *     )
     * )
     * }
     * </pre>
     *
     * @param java.util.Map with optional 'attempts' (default = 3) and 'delay' (default = 1000)
     */
    Func1<Observable<? extends Throwable>, Observable<?>> withFixedDelay(Map map);


    /**
     * Creates a retry strategy with exponential backoff in seconds.
     *
     * Setting seed to is the same as running withFixedDelay and a delay of 1000ms. Setting a value below 1 will give
     * you a shorter and shorter retry time.
     *
     *
     *
     * Sample below will retry after 2,4,8 and 16 seconds.
     * <pre>
     * Groovy usage:
     *  {@code
     * observable.retryWhen (
     *     exceptionOfTypes ([MyException.class, AnotherException.class])
     *          .withExponentialDelay (
     *              attempts : 5,
     *              seed : 2 // in seconds, float value, can be below 1
     *         )
     *     )
     * )
     * }
     * </pre>
     *
     * @param java.util.Map with optional 'attempts' (default = 3) and 'delay' (default = 1000)
     */
    Func1<Observable<? extends Throwable>, Observable<?>> withExponentialDelay(Map map);
    
    Func1<Observable<? extends Throwable>, Observable<?>> withExponentialDelay();

    Func1<Observable<? extends Throwable>, Observable<?>> withFixedDelay();

    Func1<Observable<? extends Throwable>, Observable<?>> withLinearDelay();
}
