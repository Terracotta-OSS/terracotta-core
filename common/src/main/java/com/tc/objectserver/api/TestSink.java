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
package com.tc.objectserver.api;

import com.tc.async.api.Sink;

import java.util.LinkedList;
import java.util.List;

/**
 * @author steve
 */
public class TestSink<EC> implements Sink<EC> {
  private final List<EC> queue = new LinkedList<EC>();

  @Override
  public void addToSink(EC context) {
    synchronized (queue) {
      queue.add(context);
      queue.notifyAll();
    }
  }

  public EC waitForAdd(long millis) throws InterruptedException {
    synchronized (queue) {
      if (queue.size() < 1) {
        queue.wait(millis);
      }
      return queue.size() < 1 ? null : (EC) queue.get(0);
    }
  }

  public EC take() throws InterruptedException {
    synchronized (queue) {
      while (queue.size() < 1) {
        queue.wait();
      }
      return queue.remove(0);
    }
  }

  public List<EC> getInternalQueue() {
    return queue;
  }
}
