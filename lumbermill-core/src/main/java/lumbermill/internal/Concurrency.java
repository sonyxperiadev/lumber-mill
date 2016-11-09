package lumbermill.internal;

import lumbermill.api.Observables;
import rx.Observable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class Concurrency {

    private static ExecutorService IO_EXECUTOR = Executors.newCachedThreadPool();

    private static <T>  CompletableFuture<T> runOnIO(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, IO_EXECUTOR);
    }

    public static <T> Observable<T> ioJob(Supplier<T> supplier) {
        return Observables.observe(runOnIO(supplier));
    }
}
