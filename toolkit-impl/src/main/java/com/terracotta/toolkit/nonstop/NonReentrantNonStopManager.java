/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.internal.nonstop.NonStopManager;

import java.util.concurrent.atomic.AtomicBoolean;

public class NonReentrantNonStopManager implements NonStopManager {
  private final NonStopManager             delegate;
  private final ThreadLocal<AtomicBoolean> threadStatus = new ThreadLocal<AtomicBoolean>() {
                                                          @Override
                                                          protected AtomicBoolean initialValue() {
                                                            return new AtomicBoolean(true);
                                                          }
                                                        };

  public NonReentrantNonStopManager(NonStopManager delegate) {
    this.delegate = delegate;
  }

  @Override
  public void begin(long timeout) {
    if (threadStatus.get().compareAndSet(true, false)) {
      delegate.begin(timeout);
    } else {
      throw new IllegalStateException("The thread has already called begin");
    }
  }

  @Override
  public void finish() {
    if (!threadStatus.get().get()) {
      delegate.finish();
    } else {
      throw new IllegalStateException("The thread has not called begin");
    }
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }
}
