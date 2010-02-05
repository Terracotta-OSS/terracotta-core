/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.LossyTCLogger;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.LossyTCLogger.LossyTCLoggerType;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
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

  private int                     nextWorkerCommId   = 0;

  TCWorkerCommManager(String name, int workerCommCount, SocketParams socketParams) {
    if (workerCommCount <= 0) { throw new IllegalArgumentException("invalid worker count: " + workerCommCount); }
    logger.info("Creating " + workerCommCount + " worker comm threads for " + name);
    this.totalWorkerComm = workerCommCount;
    this.workerCommThreads = new CoreNIOServices[workerCommCount];
    for (int i = 0; i < this.workerCommThreads.length; i++) {
      this.workerCommThreads[i] = new CoreNIOServices(name + ":" + WORKER_NAME_PREFIX + i, this, socketParams);
    }
  }

  public synchronized CoreNIOServices getNextWorkerComm() {
    CoreNIOServices[] leastWeightWorkerComms = getLeastWeightWorkerComms(this.workerCommThreads);
    CoreNIOServices rv;
    Assert.eval(leastWeightWorkerComms.length >= 1);
    if (leastWeightWorkerComms.length == 1) {
      rv = leastWeightWorkerComms[0];
    } else {
      rv = leastWeightWorkerComms[this.nextWorkerCommId++ % leastWeightWorkerComms.length];
    }

    String message = "Selecting " + rv + "  from " + Arrays.asList(this.workerCommThreads);
    if (logger.isDebugEnabled()) {
      logger.debug(message);
    } else {
      lossyLogger.info(message);
    }

    return rv;
  }

  private CoreNIOServices[] getLeastWeightWorkerComms(CoreNIOServices[] workerComms) {
    ArrayList<CoreNIOServices> selectedWorkerComms = new ArrayList<CoreNIOServices>();
    int leastValue = Integer.MAX_VALUE;
    for (CoreNIOServices workerComm : workerComms) {
      int presentValue = workerComm.getWeight();
      if (presentValue < leastValue) {
        selectedWorkerComms.clear();
        selectedWorkerComms.add(workerComm);
        leastValue = presentValue;
      } else if (presentValue == leastValue) {
        selectedWorkerComms.add(workerComm);
      }
    }

    return selectedWorkerComms.toArray(new CoreNIOServices[selectedWorkerComms.size()]);
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

  protected synchronized CoreNIOServices getWorkerComm(int workerCommId) {
    return this.workerCommThreads[workerCommId];
  }

  protected synchronized int getWeightForWorkerComm(int workerCommId) {
    return this.workerCommThreads[workerCommId].getWeight();
  }

  protected synchronized long getTotalBytesReadByWorkerComm(int workerCommId) {
    return this.workerCommThreads[workerCommId].getTotalBytesRead();
  }

  protected synchronized long getTotalBytesWrittenByWorkerComm(int workerCommId) {
    return this.workerCommThreads[workerCommId].getTotalBytesWritten();
  }

}
