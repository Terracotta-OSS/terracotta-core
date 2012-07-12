/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.impl;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author steve
 */
public class TestEventHandler extends AbstractEventHandler {
  private final List<EventContext> contexts = new LinkedList<EventContext>();

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.async.api.EventHandler#handleEvent(com.tc.async.api.EventContext)
   */
  @Override
  public synchronized void handleEvent(EventContext context) {
    contexts.add(context);
    notifyAll();
  }

  public synchronized void waitForEventContextCount(int count, long timeout, TimeUnit unit) throws InterruptedException {
    if (count < 0) { throw new AssertionError("Count can not be less than 0. count=" + count); }
    long timeoutNanos = unit.toNanos(timeout);
    long start = System.nanoTime();
    while (contexts.size() < count && (System.nanoTime() - start < timeoutNanos)) {
      wait(NANOSECONDS.toMillis(System.nanoTime() - start));
    }
  }

  public synchronized List<EventContext> getContexts() {
    return new ArrayList<EventContext>(contexts);
  }
}