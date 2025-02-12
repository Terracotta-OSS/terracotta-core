/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.stats;

import java.util.LinkedList;

/**
 * A stack with a fixed depth (pushing beyond the depth of the stack will discard oldest item)
 */
public class LossyStack<T> {

  private final LinkedList<T> data = new LinkedList<T>();
  private final int        maxDepth;

  public LossyStack(int depth) {
    if (depth < 1) { throw new IllegalArgumentException("stack depth must be greater than or equal to 1"); }
    this.maxDepth = depth;
  }

  public synchronized void push(T obj) {
    // we could slightly optimize the mostRecent() call by specifically storing the reference
    // to the last object added in a dedicated variable
    data.addFirst(obj);
    if (data.size() > maxDepth) {
      data.removeLast();
    }
  }

  public synchronized T pop() {
    if (data.isEmpty()) { throw new IllegalStateException("stack empty"); }
    return data.removeFirst();
  }

  public synchronized T[] toArray(T[] type) {
    return data.toArray(type);
  }

  public synchronized T peek() {
    if (data.isEmpty()) { return null; }
    return data.getFirst();
  }

  public synchronized boolean isEmtpy() {
    return data.isEmpty();
  }

  public synchronized int depth() {
    return data.size();
  }

}
