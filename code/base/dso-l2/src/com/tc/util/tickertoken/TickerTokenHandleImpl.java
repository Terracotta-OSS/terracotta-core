/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken;

import java.util.concurrent.atomic.AtomicBoolean;

public class TickerTokenHandleImpl implements TickerTokenHandle {

  private final TickerTokenKey key;
  private final String         identifier;
  private final AtomicBoolean  triggerToken;
  private boolean              complete = false;

  public TickerTokenHandleImpl(String identifier, TickerTokenKey key, AtomicBoolean triggerToken) {
    this.identifier = identifier;
    this.triggerToken = triggerToken;
    this.key = key;
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public void enableTriggerToken() {
    synchronized (triggerToken) {
      triggerToken.set(true);
    }
  }

  public synchronized void waitTillComplete() {
    enableTriggerToken();
    while (!this.complete) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  public synchronized boolean isComplete() {
    return this.complete;
  }

  public synchronized void complete() {
    this.complete = true;
    notifyAll();
  }

  public void cancel() {
    complete();
  }

  public TickerTokenKey getKey() {
    return this.key;
  }

  @Override
  public synchronized String toString() {
    return "TickerTokenHandleImpl [ " + this.identifier + " , " + this.key + " ] : completed :  " + this.complete;
  }
}
