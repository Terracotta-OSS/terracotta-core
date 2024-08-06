/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
