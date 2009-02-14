/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

public class TickerTokenHandleImpl implements TickerTokenHandle {

  boolean                complete = false;
  private Object         lock     = new Object();
  private TickerTokenKey key;

  public void waitTillComplete() {
    if (!complete) {
      synchronized (lock) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
    complete = false;
  }

  public void complete() {
    complete = true;
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  public void cancel() {
    complete();
  }

  public TickerTokenKey getKey() {
    return key;
  }

  public void setKey(TickerTokenKey key) {
    this.key = key;
  }

}
