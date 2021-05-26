/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.services;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
class ResponseWaiter implements Future<Void> {
  private boolean done;
  private final long start = System.currentTimeMillis();

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public synchronized boolean isDone() {
    return done;
  }

  @Override
  public synchronized Void get() throws InterruptedException, ExecutionException {
    while (!done) {
      wait();
    }
    System.out.println("returning " + this);
    return null;
  }

  @Override
  public synchronized Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    long timeoutTime = unit.toNanos(timeout) + System.nanoTime();
    while (!done) {
      long waitTime = TimeUnit.NANOSECONDS.toMillis(timeoutTime - System.nanoTime());
      if (waitTime <= 0) {
        throw new TimeoutException();
      }
      wait(waitTime);
    }
    return null;
  }

  synchronized void done() {
    done = true;
    notifyAll();
  }

  @Override
  public String toString() {
    return "ResponseWaiter{" + System.identityHashCode(this) + ", time=" + (System.currentTimeMillis() - start) + '}';
  }
}
