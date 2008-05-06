/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.SetOnceFlag;

/**
 * The whole intention of this class is to manage the workerThreads for each Listener
 *
 * @author Manoj G
 */
public class TCWorkerCommManager {
  private static final TCLogger   logger                 = TCLogging.getLogger(TCWorkerCommManager.class);

  private static final String     WORKER_NAME_PREFIX     = "TCWorkerComm # ";

  private final int               totalWorkerComm;
  private final CoreNIOServices[] workerCommThreads;
  private final int[]             workerCommClientCount;
  private final SetOnceFlag       started                = new SetOnceFlag();
  private final SetOnceFlag       stopped                = new SetOnceFlag();

  private int                     nextWorkerCommId = 0;

  TCWorkerCommManager(int workerCommCount, SocketParams socketParams) {
    if (workerCommCount <= 0) { throw new IllegalArgumentException("invalid worker count: " + workerCommCount); }

    logger.info("Creating " + workerCommCount + " worker comm threads.");

    this.totalWorkerComm = workerCommCount;

    workerCommThreads = new CoreNIOServices[workerCommCount];
    for (int i = 0; i < workerCommThreads.length; i++) {
      workerCommThreads[i] = new CoreNIOServices(WORKER_NAME_PREFIX + i, this, socketParams);
    }

    workerCommClientCount = new int[workerCommCount];
  }

  public synchronized CoreNIOServices getNextWorkerComm() {
    int id = nextWorkerCommId;
    nextWorkerCommId = ++nextWorkerCommId % totalWorkerComm;

    workerCommClientCount[id]++;
    return workerCommThreads[id];
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
    return workerCommClientCount[workerCommId];
  }

  public synchronized long getBytesReadByWorkerComm(int workerCommId) {
    return workerCommThreads[workerCommId].getTotalBytesRead();
  }

}
