/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

public class TickerTokenHandleImpl implements TickerTokenHandle {

  private boolean        complete = false;
  private TickerTokenKey key;

  public synchronized void waitTillComplete() {
    while (!this.complete) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  public synchronized void complete() {
    this.complete = true;
    notifyAll();
  }

  public void cancel() {
    complete();
  }

  public synchronized TickerTokenKey getKey() {
    return this.key;
  }

  public synchronized void setKey(TickerTokenKey aKey) {
    Assert.assertNull(this.key);
    this.key = aKey;
  }
}
