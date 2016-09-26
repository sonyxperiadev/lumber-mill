package lumbermill.aws.kcl.internal;

/**
 * From KCL Documentation
 *
 * The KCL relies on processRecords to handle any exceptions that arise from processing the data records.
 * If an exception is thrown from processRecords, the KCL skips over the data records that were passed prior
 * to the exception; that is, these records are not re-sent to the record processor that threw the exception
 * or to any other record processor in the consumer.
 *
 */

public enum ExceptionStrategy {

  /**
   * Simply blocks the current execution, this is instead of EXIT
   */
  BLOCK,

  /**
   * Continue processing knowing that something went wrong
   */
  CONTINUE_NO_CHECKPOINT,

  /**
   * Exit process (System.exit(1)) and restart to retry.
   */
  EXIT
}
