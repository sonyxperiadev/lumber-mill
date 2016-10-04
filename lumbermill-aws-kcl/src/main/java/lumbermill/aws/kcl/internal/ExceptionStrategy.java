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
