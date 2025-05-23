/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.logging.LossyTCLogger;
import com.tc.logging.LossyTCLogger.LossyTCLoggerType;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The whole intention of this class is to manage the workerThreads for each Listener
 * 
 * @author Manoj G
 */
public class TCWorkerCommManager {
  private static final Logger logger = LoggerFactory.getLogger(TCWorkerCommManager.class);
  private static final LossyTCLogger lossyLogger = new LossyTCLogger(logger, 10, LossyTCLoggerType.COUNT_BASED, false);

  private static final String     WORKER_NAME_PREFIX = "TCWorkerComm # ";

  private final int               totalWorkerComm;
  private final CoreNIOServices[] workerCommThreads;
  private final SetOnceFlag       started            = new SetOnceFlag();
  private final SetOnceFlag       stopped            = new SetOnceFlag();
  
  private boolean paused = false;

  TCWorkerCommManager(String name, int workerCommCount, SocketParams socketParams) {
    if (workerCommCount <= 0) { throw new IllegalArgumentException("invalid worker count: " + workerCommCount); }
    logger.info("Creating " + workerCommCount + " worker comm threads for " + name);
    this.totalWorkerComm = workerCommCount;
    this.workerCommThreads = new CoreNIOServices[workerCommCount];
    for (int i = 0; i < this.workerCommThreads.length; i++) {
      this.workerCommThreads[i] = new CoreNIOServices(name + " - " + WORKER_NAME_PREFIX + i, this, socketParams);
    }
  }

  public CoreNIOServices getNextWorkerComm() {
    CoreNIOServices leastWeightWorkerComm = null;
    
    while (leastWeightWorkerComm == null) {
      leastWeightWorkerComm = getLeastWeightWorkerComm();
    }
    // We can't fail to get the least.
    Assert.assertTrue(null != leastWeightWorkerComm);

    String message = "Selecting " + leastWeightWorkerComm + "  from " + Arrays.asList(this.workerCommThreads);
    if (logger.isDebugEnabled()) {
      logger.debug(message);
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
    for (CoreNIOServices workerComm : workerCommThreads) {
      if (workerComm.compareWeights(selectedWorkerComm)) {
        selectedWorkerComm = workerComm;
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
  
  public List<?> getState() {
    return Arrays.stream(workerCommThreads).map(s->s.getState()).collect(Collectors.toList());
  }
  
  synchronized void waitDuringPause() throws IOException {
    while (paused) {
      try {
        this.wait();
      } catch (InterruptedException ie) {
        throw new InterruptedIOException();
      }
    }
  }
  
  public boolean isOverweight(int weight) {
    if (weight <= totalWorkerComm) return false;
    for (CoreNIOServices c : workerCommThreads) {
      if (c.getWeight() * 2 < weight && c.getCongestionScore() == 0) return true;
    }
    return false;
  }
  
  public synchronized void pause() {
    paused = true;
  }
  
  public synchronized void unpause() {
    paused = false;
    this.notifyAll();
  }  
}
