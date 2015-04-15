/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

/**
 * Implementation for TCComm. Manages communication threads for new connection and listeners at a high level.
 * 
 * @author teck
 * @author mgovinda
 */
class TCCommImpl implements TCComm {

  private final TCWorkerCommManager workerCommMgr;
  private final CoreNIOServices     commThread;
  private final String              commThreadName = "TCComm Main Selector Thread";
  private static final TCLogger     logger         = TCLogging.getLogger(TCCommImpl.class);

  private volatile boolean          started        = false;

  TCCommImpl(String name, int workerCommCount, SocketParams socketParams) {
    if (workerCommCount > 0) {
      workerCommMgr = new TCWorkerCommManager(name, workerCommCount, socketParams);
    } else {
      logger.info("Comm Worker Threads NOT requested");
      workerCommMgr = null;
    }

    this.commThread = new CoreNIOServices(name + ":" + commThreadName, workerCommMgr, socketParams);
  }

  protected int getWeightForWorkerComm(int workerCommId) {
    if (workerCommMgr != null) { return workerCommMgr.getWeightForWorkerComm(workerCommId); }
    return 0;
  }

  protected CoreNIOServices getWorkerComm(int workerCommId) {
    if (workerCommMgr != null) { return workerCommMgr.getWorkerComm(workerCommId); }
    return null;
  }

  protected long getTotalbytesReadByWorkerComm(int workerCommId) {
    if (workerCommMgr != null) { return workerCommMgr.getTotalBytesReadByWorkerComm(workerCommId); }
    return 0;
  }

  protected long getTotalbytesWrittenByWorkerComm(int workerCommId) {
    if (workerCommMgr != null) { return workerCommMgr.getTotalBytesWrittenByWorkerComm(workerCommId); }
    return 0;
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public boolean isStopped() {
    return !started;
  }

  @Override
  public final synchronized void start() {
    if (!started) {
      started = true;
      if (logger.isDebugEnabled()) {
        logger.debug("Start requested");
      }

      // The worker comm threads
      if (workerCommMgr != null) {
        workerCommMgr.start();
      }

      // The Main Listener
      commThread.start();
    }
  }

  @Override
  public final synchronized void stop() {
    if (started) {
      started = false;
      if (logger.isDebugEnabled()) {
        logger.debug("Stop requested");
      }
      commThread.requestStop();
      if (workerCommMgr != null) {
        workerCommMgr.stop();
      }
    }
  }

  public CoreNIOServices nioServiceThreadForNewConnection() {
    // For now we're always assuming that client side comms use the main selector
    return commThread;
  }

  public CoreNIOServices nioServiceThreadForNewListener() {
    return commThread;
  }

}
