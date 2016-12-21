/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.core;

import com.tc.logging.LossyTCLogger;
import com.tc.logging.LossyTCLogger.LossyTCLoggerType;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.Arrays;


/**
 * The whole intention of this class is to manage the workerThreads for each Listener
 * 
 * @author Manoj G
 */
public class TCWorkerCommManager {
  private static final TCLogger   logger             = TCLogging.getLogger(TCWorkerCommManager.class);
  private static final TCLogger   lossyLogger        = new LossyTCLogger(logger, 10, LossyTCLoggerType.COUNT_BASED,
                                                                         false);

  private static final String     WORKER_NAME_PREFIX = "TCWorkerComm # ";

  private final int               totalWorkerComm;
  private final CoreNIOServices[] workerCommThreads;
  private final SetOnceFlag       started            = new SetOnceFlag();
  private final SetOnceFlag       stopped            = new SetOnceFlag();

  TCWorkerCommManager(String name, int workerCommCount, SocketParams socketParams) {
    if (workerCommCount <= 0) { throw new IllegalArgumentException("invalid worker count: " + workerCommCount); }
    logger.info("Creating " + workerCommCount + " worker comm threads for " + name);
    this.totalWorkerComm = workerCommCount;
    this.workerCommThreads = new CoreNIOServices[workerCommCount];
    for (int i = 0; i < this.workerCommThreads.length; i++) {
      this.workerCommThreads[i] = new CoreNIOServices(name + ":" + WORKER_NAME_PREFIX + i, this, socketParams);
    }
  }

  public CoreNIOServices getNextWorkerComm() {
    CoreNIOServices leastWeightWorkerComm = getLeastWeightWorkerComm();
    // We can't fail to get the least.
    Assert.assertTrue(null != leastWeightWorkerComm);

    String message = "Selecting " + leastWeightWorkerComm + "  from " + Arrays.asList(this.workerCommThreads);
    if (logger.isDebugEnabled()) {
      logger.debug(message);
    } else {
      lossyLogger.info(message);
    }

    return leastWeightWorkerComm;
  }

  /**
   * Finds the underlying {@link CoreNIOServices} worker comm thread with the lowest weight.  Note that this might be
   * called, concurrently, so it is really just a best-efforts attempt (since 2 threads could get the same answer or
   * a previously-requesting thread changes the result underneath the currently-requesting thread).
   * 
   * @return The CoreNIOServices with the least weight (at least when scanned).
   */
  private CoreNIOServices getLeastWeightWorkerComm() {
    CoreNIOServices selectedWorkerComm = null;
    int leastValue = Integer.MAX_VALUE;
    for (CoreNIOServices workerComm : workerCommThreads) {
      int presentValue = workerComm.getWeight();
      if (presentValue < leastValue) {
        selectedWorkerComm = workerComm;
        leastValue = presentValue;
      }
    }
    return selectedWorkerComm;
  }

  public synchronized void start() {
    if (this.started.attemptSet()) {
      for (CoreNIOServices workerCommThread : this.workerCommThreads) {
        workerCommThread.start();
      }
    } else {
      throw new IllegalStateException("already started");
    }
  }

  public synchronized void stop() {
    if (!this.started.isSet()) { return; }

    if (this.stopped.attemptSet()) {
      for (int i = 0; i < this.totalWorkerComm; i++) {
        this.workerCommThreads[i].requestStop();
      }
    }
  }

  protected CoreNIOServices getWorkerComm(int workerCommId) {
    return this.workerCommThreads[workerCommId];
  }

  protected int getWeightForWorkerComm(int workerCommId) {
    return this.workerCommThreads[workerCommId].getWeight();
  }

  protected long getTotalBytesReadByWorkerComm(int workerCommId) {
    return this.workerCommThreads[workerCommId].getTotalBytesRead();
  }

  protected long getTotalBytesWrittenByWorkerComm(int workerCommId) {
    return this.workerCommThreads[workerCommId].getTotalBytesWritten();
  }

}
