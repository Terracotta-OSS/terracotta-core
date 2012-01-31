/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.LossyTCLogger;
import com.tc.logging.LossyTCLogger.LossyTCLoggerType;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

  private final AtomicInteger     nextWorkerCommId   = new AtomicInteger();

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
    List<CoreNIOServices> leastWeightWorkerComms = getLeastWeightWorkerComms();
    CoreNIOServices rv;
    Assert.eval(leastWeightWorkerComms.size() >= 1);
    if (leastWeightWorkerComms.size() == 1) {
      rv = leastWeightWorkerComms.get(0);
    } else {
      rv = leastWeightWorkerComms.get(nextWorkerCommId.getAndIncrement() % leastWeightWorkerComms.size());
    }

    String message = "Selecting " + rv + "  from " + Arrays.asList(this.workerCommThreads);
    if (logger.isDebugEnabled()) {
      logger.debug(message);
    } else {
      lossyLogger.info(message);
    }

    return rv;
  }

  private List<CoreNIOServices> getLeastWeightWorkerComms() {
    List<CoreNIOServices> selectedWorkerComms = new ArrayList<CoreNIOServices>();
    int leastValue = Integer.MAX_VALUE;
    for (CoreNIOServices workerComm : workerCommThreads) {
      int presentValue = workerComm.getWeight();
      if (presentValue < leastValue) {
        selectedWorkerComms.clear();
        selectedWorkerComms.add(workerComm);
        leastValue = presentValue;
      } else if (presentValue == leastValue) {
        selectedWorkerComms.add(workerComm);
      }
    }
    return selectedWorkerComms;
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
