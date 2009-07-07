/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.LossyTCLogger;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
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
  private static final TCLogger   lossyLogger        = new LossyTCLogger(logger, 10, LossyTCLogger.COUNT_BASED, false);

  private static final String     WORKER_NAME_PREFIX = "TCWorkerComm # ";

  private final int               totalWorkerComm;
  private final CoreNIOServices[] workerCommThreads;
  private final SetOnceFlag       started            = new SetOnceFlag();
  private final SetOnceFlag       stopped            = new SetOnceFlag();

  private int                     nextWorkerCommId   = 0;

  TCWorkerCommManager(int workerCommCount, SocketParams socketParams) {
    if (workerCommCount <= 0) { throw new IllegalArgumentException("invalid worker count: " + workerCommCount); }
    logger.info("Creating " + workerCommCount + " worker comm threads.");
    this.totalWorkerComm = workerCommCount;
    workerCommThreads = new CoreNIOServices[workerCommCount];
    for (int i = 0; i < workerCommThreads.length; i++) {
      workerCommThreads[i] = new CoreNIOServices(WORKER_NAME_PREFIX + i, this, socketParams);
    }
  }

  public synchronized CoreNIOServices getNextWorkerComm() {
    CoreNIOServices[] leastWeightWorkerComms = getLeastWeightWorkerComms(workerCommThreads);
    CoreNIOServices rv;
    Assert.eval(leastWeightWorkerComms.length >= 1);
    if (leastWeightWorkerComms.length == 1) {
      rv = leastWeightWorkerComms[0];
    } else {
      rv = leastWeightWorkerComms[nextWorkerCommId++ % leastWeightWorkerComms.length];
    }

    String message = "Selecting " + rv + "  from " + Arrays.asList(workerCommThreads);
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
    for (int i = 0; i < workerComms.length; i++) {
      int presentValue = workerComms[i].getWeight();
      if (presentValue < leastValue) {
        selectedWorkerComms.clear();
        selectedWorkerComms.add(workerComms[i]);
        leastValue = presentValue;
      } else if (presentValue == leastValue) {
        selectedWorkerComms.add(workerComms[i]);
      }
    }

    return selectedWorkerComms.toArray(new CoreNIOServices[selectedWorkerComms.size()]);
  }

  public synchronized void start() {
    if (started.attemptSet()) {
      for (int i = 0; i < workerCommThreads.length; i++) {
        workerCommThreads[i].start();
      }
    } else {
      throw new IllegalStateException("already started");
    }
  }

  public synchronized void stop() {
    if (!started.isSet()) { return; }

    if (stopped.attemptSet()) {
      for (int i = 0; i < totalWorkerComm; i++) {
        workerCommThreads[i].requestStop();
      }
    }
  }

  public synchronized int getClientCountForWorkerComm(int workerCommId) {
    return workerCommThreads[workerCommId].getWeight();
  }

  public synchronized long getBytesReadByWorkerComm(int workerCommId) {
    return workerCommThreads[workerCommId].getTotalBytesRead();
  }

}
