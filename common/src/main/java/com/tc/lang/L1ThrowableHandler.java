/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.lang;

import com.tc.logging.TCLogger;

import java.util.concurrent.Callable;

/**
 * A {@link ThrowableHandler} for Terracotta Client which avoids {@link System#exit(int)} on inconsistent state of
 * Terracotta Client. This handler will shutdown Terracotta Client instead through l1ShutdownCallable.
 */
public class L1ThrowableHandler extends ThrowableHandlerImpl {
  private final Callable<Void> l1ShutdownCallable;

  public L1ThrowableHandler(TCLogger logger, Callable<Void> l1ShutdownCallable) {
    super(logger);
    this.l1ShutdownCallable = l1ShutdownCallable;
  }

  @Override
  protected synchronized void exit(int status) {
    try {
      l1ShutdownCallable.call();
    } catch (Exception e) {
      logger.error("Exception while shutting down Terracotta Client", e);
    }
  }

}
