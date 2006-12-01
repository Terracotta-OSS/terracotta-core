/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;

/**
 * Common base class for TCComm implementations
 *
 * @author teck
 */
abstract class AbstractTCComm implements TCComm, TCListenerEventListener {
  protected static final TCLogger logger  = TCLogging.getLogger(TCComm.class);
  private volatile boolean        started = false;

  public final boolean isStarted() {
    return started;
  }

  public final boolean isStopped() {
    return !started;
  }

  public final synchronized void start() {
    if (!started) {
      started = true;
      if (logger.isDebugEnabled()) {
        logger.debug("Start requested");
      }

      startImpl();
    }
  }

  public final synchronized void stop() {
    if (started) {
      started = false;
      if (logger.isDebugEnabled()) {
        logger.debug("Stop requested");
      }

      stopImpl();
    }
  }

  public void closeEvent(TCListenerEvent event) {
    // override me if desired
  }

  void listenerAdded(TCListener listener) {
    // override me if desired
  }

  protected abstract void startImpl();

  protected abstract void stopImpl();
}