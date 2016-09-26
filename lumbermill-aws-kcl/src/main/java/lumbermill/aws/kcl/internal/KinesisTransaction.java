package lumbermill.aws.kcl.internal;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KinesisTransaction {
    final static Logger LOG = LoggerFactory.getLogger(KinesisTransaction.class);

    private static final long BACKOFF_TIME_IN_MILLIS = 3000L;
    private static final int NUM_RETRIES = 30;


    public KinesisTransaction(boolean dry) {
        this.dry = dry;
    }

    public final boolean dry;

    public void checkpoint(IRecordProcessorCheckpointer checkpointer) {

        if (dry) {
            LOG.debug ("Dry mode, no checkpointing");
            return;
        }

        /** This snippet is from the Kinesis Client Library example code */
        for (int i = 0; i < NUM_RETRIES; i++) {
            try {
                checkpointer.checkpoint();
                break;
            } catch (ShutdownException se) {
                // Ignore checkpoint if the processor instance has been shutdown (fail over).
                LOG.warn("Caught shutdown exception, skipping checkpoint.", se);
                break;
            } catch (ThrottlingException e) {
                // Backoff and re-attempt checkpoint upon transient failures
                if (i >= (NUM_RETRIES - 1)) {
                    LOG.error("Checkpoint failed after " + (i + 1) + "attempts.", e);
                    break;
                } else {
                    LOG.warn("Transient issue when checkpointing - attempt " + (i + 1) + " of " + NUM_RETRIES, e);
                }
            } catch (InvalidStateException e) {
                // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
                LOG.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
                break;
            }
            try {
                Thread.sleep(BACKOFF_TIME_IN_MILLIS);
            } catch (InterruptedException e) {
                LOG.debug("Interrupted sleep", e);
            }
        }
        LOG.trace("Checkpointing kinesis done");
    }
}
