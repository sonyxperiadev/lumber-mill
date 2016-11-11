/*
 * Copyright 2016 Sony Mobile Communications, Inc., Inc.
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
package lumbermill.aws.kcl.internal;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import lumbermill.api.Codecs;
import lumbermill.aws.kcl.KCL;
import lumbermill.aws.kcl.KCL.UnitOfWorkListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Processes records and checkpoints progress.
 */
public class RecordProcessor implements IRecordProcessor {

    private final static Logger LOG = LoggerFactory.getLogger(RecordProcessor.class);
    private final ExceptionStrategy exceptionStrategy;
    private final KCL.Metrics metricsCallback;
    private String kinesisShardId;
    private KinesisTransaction transaction;

    private final UnitOfWorkListener unitOfWorkListener;

    public RecordProcessor(UnitOfWorkListener unitOfWorkListener,
                           ExceptionStrategy exceptionStrategy,
                           KCL.Metrics metricsCallback,
                           boolean dry) {
        this.unitOfWorkListener = unitOfWorkListener;
        this.exceptionStrategy = exceptionStrategy;
        this.metricsCallback = metricsCallback;
        this.transaction = new KinesisTransaction(dry);
    }

    @Override
    public void initialize(InitializationInput initializationInput) {
        LOG.info("Init RecordProcessor " + initializationInput.getShardId());
        this.kinesisShardId = initializationInput.getShardId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
       try {
           List<Record> records = processRecordsInput.getRecords();
           Thread.currentThread().setName(kinesisShardId);
           int bytes = calculateSize(records);

           LOG.debug("Got {} records ({} bytes) and is behind latest with {}",
                   records.size(), bytes, printTextBehindLatest(processRecordsInput));

           metricsCallback.shardBehindMs (kinesisShardId, processRecordsInput.getMillisBehindLatest());

           Observable observable = Observable.create(subscriber -> {
               try {
                   for (Record record : records) {
                       subscriber.onNext(Codecs.BYTES.from(record.getData().array())
                               .put("_shardId", kinesisShardId));
                   }
                   subscriber.onCompleted();
                   metricsCallback.recordsProcessed (kinesisShardId, records.size());
                   metricsCallback.bytesProcessed (kinesisShardId,bytes);
               } catch (RuntimeException e) {
                   subscriber.onError(e);
               }
           });

           unitOfWorkListener.apply(observable).toBlocking().subscribe();
           transaction.checkpoint(processRecordsInput.getCheckpointer());
       } catch (RuntimeException t) {
           doOnError(t);
       }
    }


    private String printTextBehindLatest(ProcessRecordsInput processRecordsInput) {
        return processRecordsInput.getMillisBehindLatest() < 60000
                ? String.format("%s secs", TimeUnit.MILLISECONDS.toSeconds(processRecordsInput.getMillisBehindLatest()))
                : String.format("%s min", TimeUnit.MILLISECONDS.toMinutes(processRecordsInput.getMillisBehindLatest()));
    }


    private void doOnError(RuntimeException e) {
        if (exceptionStrategy == ExceptionStrategy.CONTINUE_NO_CHECKPOINT) {
            LOG.error ("Got unexpected exception but will CONTINUE with processing", e);
            throw e;
        } else if (exceptionStrategy == ExceptionStrategy.BLOCK) {
            // This will totally fail since we might not be on Kinesis thread.
            LOG.error ("Got unexpected exception and will block until manually killed", e);
            blockForever();
        } else if (exceptionStrategy == ExceptionStrategy.EXIT) {
            LOG.error ("Got unexpected exception and will now exit with System.exit()", e);
            System.exit(1);
        }
    }


    private void blockForever() {
        //noinspection InfiniteLoopStatement
        for (; ;) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
                // Interrupted for a reason, but we will just block again...
                LOG.info("Blocked processing thread interrupted");
                Thread.currentThread().interrupt();
            }
        }
    }

    private int calculateSize(List<Record> records) {
        int bytes = 0;

        for (Record r : records) {
            bytes += r.getData().remaining();
        }

        // We get it in binary, but it's actually sent as Base64
        return bytes * 3 / 2;
    }

    @Override
    public void shutdown(ShutdownInput input) {

        Thread.currentThread().setName(kinesisShardId);
        if (input.getShutdownReason() == ShutdownReason.ZOMBIE) {
            /* This happens because we have lost our lease. Either because of re-balancing
            * (there is another NomNom running on another host), or because we have failed
            * failed to renew our leases, which would happen if we are too busy.
            *
            * It happens when a new version of NomNom is deployed, since the two versions
            * will run side by side for a few seconds before the old one is terminated. And
            * the new one will try to take a few leases at startup.
            */
            LOG.warn("We're a ZOMBIE - someone stole our lease. Quitting.");
        } else {
            /* This happens when a shard is split or merged, meaning that it stops existing
             * and we have other shards to process instead. Very rare.
             */
            LOG.warn("Shard is shutting down, reason: {}", input.getShutdownReason());
            try {
                input.getCheckpointer().checkpoint();
            } catch (Exception e) {
                LOG.error("Failed to checkpoint after shard shutdown", e);
            }
        }
        LOG.info("");
    }

}