package lumbermill.aws.kcl;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;

import lumbermill.api.Event;
import lumbermill.aws.kcl.internal.ExceptionStrategy;
import lumbermill.aws.kcl.internal.KinesisConsumerBootstrap;
import rx.Observable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@SuppressWarnings("unused")
public class KCL {

    private final KinesisClientLibConfiguration config;

    private ExceptionStrategy exceptionStrategy = ExceptionStrategy.EXIT;

    private Metrics callback = new NoOptMetrics ();
    private boolean dry = false;

    public static KCL create (KinesisClientLibConfiguration configuration) {
        return new KCL (configuration);
    }

    private KCL (KinesisClientLibConfiguration configuration) {
        this.config = configuration;
    }


    public KCL exitOnUnhandledException () {
        this.exceptionStrategy = ExceptionStrategy.EXIT;
        return this;
    }

    public KCL continueOnUnhandledException () {
        this.exceptionStrategy = ExceptionStrategy.CONTINUE_NO_CHECKPOINT;
        return this;
    }

    public KCL blockForeverOnUnhandledException () {
        this.exceptionStrategy = ExceptionStrategy.BLOCK;
        return this;
    }


    public KCL withMetrics (Metrics metrics) {
        this.callback = metrics;
        return this;
    }

    public KCL dry () {
        this.dry = true;
        return this;
    }

    public KCL dry (boolean dry) {
        this.dry = dry;
        return this;
    }

    /**
     * Starts the KCL consumer.
     *
     * @param unitOfWorkListener - UnitOfWorkListener that should be used.
     */
    public void handleRecordBatch (UnitOfWorkListener unitOfWorkListener) {
        new KinesisConsumerBootstrap (config, unitOfWorkListener, exceptionStrategy, callback, dry).start ();
    }

    public static String workerId () {
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost ().getCanonicalHostName ();
        } catch (UnknownHostException e) {
        }
        return String.format ("%s:%d:%s", hostname, System.currentTimeMillis () / 1000,
          UUID.randomUUID ().toString ().replace ("-", ""));
    }

    public interface UnitOfWorkListener {

        /**
         * Method invoked with an observable for each batch (kinesis poll).
         */
        Observable<? extends Event> apply(Observable<? extends Event> observable);
    }

    /**
     * Simple callback interface for receiving cnt and gauge from RecordProcessor
     */
    public interface Metrics {

        void bytesProcessed(String shardId, long value);
        void recordsProcessed(String shardId, long value);
        void shardBehindMs(String shardId, long value);
    }

    private static class NoOptMetrics implements Metrics {
        @Override
        public void bytesProcessed (String shardId, long value) {
        }

        @Override
        public void recordsProcessed (String shardId, long value) {
        }

        @Override
        public void shardBehindMs (String shardId, long value) {
        }
    }
}
