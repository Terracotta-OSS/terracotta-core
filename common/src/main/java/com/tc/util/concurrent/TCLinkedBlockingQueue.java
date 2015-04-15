/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util.concurrent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TCLinkedBlockingQueue implements TCQueue {
  private final LinkedBlockingQueue queue;

  public TCLinkedBlockingQueue() {
    queue = new LinkedBlockingQueue();
  }

  public TCLinkedBlockingQueue(int capacity) {
    queue = new LinkedBlockingQueue(capacity);
  }

  @Override
  public boolean offer(Object obj, long timeout) throws InterruptedException {
    return queue.offer(obj, timeout, TimeUnit.MILLISECONDS);
  }

  @Override
  public Object peek() {
    return queue.peek();
  }

  @Override
  public Object poll(long timeout) throws InterruptedException {
    return queue.poll(timeout, TimeUnit.MILLISECONDS);
  }

  @Override
  public void put(Object obj) throws InterruptedException {
    queue.put(obj);
  }

  @Override
  public Object take() throws InterruptedException {
    return queue.take();
  }

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public boolean isEmpty() {
    return queue.isEmpty();
  }

}
