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
package com.tc.async.impl;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.tc.async.api.AbstractEventHandler;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author steve
 */
public class TestEventHandler<EC> extends AbstractEventHandler<EC> {
  private final List<EC> contexts = new LinkedList<EC>();

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.async.api.EventHandler#handleEvent(com.tc.async.api.EventContext)
   */
  @Override
  public synchronized void handleEvent(EC context) {
    System.out.println("Received " + context);
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

  public synchronized List<EC> getContexts() {
    return new ArrayList<EC>(contexts);
  }
}