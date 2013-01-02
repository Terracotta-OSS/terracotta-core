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

  @Override
  public String getIdentifier() {
    return this.identifier;
  }

  @Override
  public void enableTriggerToken() {
    synchronized (triggerToken) {
      triggerToken.set(true);
    }
  }

  @Override
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

  @Override
  public synchronized boolean isComplete() {
    return this.complete;
  }

  @Override
  public synchronized void complete() {
    this.complete = true;
    notifyAll();
  }

  @Override
  public void cancel() {
    complete();
  }

  @Override
  public TickerTokenKey getKey() {
    return this.key;
  }

  @Override
  public synchronized String toString() {
    return "TickerTokenHandleImpl [ " + this.identifier + " , " + this.key + " ] : completed :  " + this.complete;
  }
}
