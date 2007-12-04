/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

/**
 * The whole intention of this class is to manage the workerThreads for each Listener
 * 
 * @author Manoj G
 */
public class TCWorkerCommManager {
  private static final TCLogger   logger                 = TCLogging.getLogger(TCWorkerCommManager.class);

  private static final String     WORKER_NAME_PREFIX     = "TCWorkerComm # ";
  private static final short      INVALID_WORKER_COMM_ID = -1;

  private final int               totalWorkerComm;
  private final CoreNIOServices[] workerCommThreads;
  private final int[]             workerCommClientCount;

  private int                     nextWorkerCommId;
  private boolean                 workerCommStarted      = false;

  TCWorkerCommManager(int workerCommCount) {
    if (workerCommCount <= 0) { throw new IllegalArgumentException("invalid worker count: " + workerCommCount); }

    logger.info("Creating " + workerCommCount + " worker comm threads.");

    this.nextWorkerCommId = INVALID_WORKER_COMM_ID;
    this.totalWorkerComm = workerCommCount;

    workerCommThreads = new CoreNIOServices[workerCommCount];
    workerCommClientCount = new int[workerCommCount];
  }

  public synchronized CoreNIOServices getNextFreeWorkerComm() {
    int iter = 0;
    do {
      nextWorkerCommId++;
      nextWorkerCommId = nextWorkerCommId % totalWorkerComm;

      iter += 1;
      if (iter >= 2 * totalWorkerComm) return null; // XXX: bug
    } while (workerCommThreads[nextWorkerCommId].isStarted() != true);
    workerCommClientCount[nextWorkerCommId]++;
    return workerCommThreads[nextWorkerCommId];
  }

  public void start() {

    workerCommStarted = true;

    for (int i = 0; i < workerCommThreads.length; i++) {
      workerCommThreads[i] = new CoreNIOServices(WORKER_NAME_PREFIX + i, this);
      workerCommThreads[i].start();
    }
  }

  public boolean isStarted() {
    if (workerCommStarted == true) {
      Assert.eval(totalWorkerComm > 0);
    }
    return workerCommStarted;
  }

  public void stop() {
    if (isStarted()) {
      for (int i = 0; i < totalWorkerComm; i++) {
        workerCommThreads[i].requestStop();
      }
    }
  }

  public int getActiveWorkerCommsCount(boolean employedCommsOnly) {
    int count = 0;
    for (int i = 0; i < totalWorkerComm; i++) {
      if (workerCommThreads[i].isStarted()) {
        if (employedCommsOnly) {
          if (workerCommClientCount[i] > 0) count += 1;
        } else {
          count += 1;
        }
      }
    }
    return count;
  }

  public int getClientCountForWorkerComm(int workerCommId) {
    if (workerCommId >= 0 && workerCommId < totalWorkerComm) { return workerCommClientCount[workerCommId]; }
    return 0;
  }

  public long getBytesReadByWorkerComm(int workerCommId) {
    if (workerCommId >= 0 && workerCommId < totalWorkerComm) { return workerCommThreads[workerCommId]
        .getTotalBytesRead(); }
    return 0;
  }

}
