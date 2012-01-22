/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;

import java.util.LinkedList;

/**
 * A stack with a fixed depth (pushing beyond the depth of the stack will discard oldest item)
 */
public class LossyStack {

  private final LinkedList data = new LinkedList();
  private final int        maxDepth;

  public LossyStack(int depth) {
    if (depth < 1) { throw new IllegalArgumentException("stack depth must be greater than or equal to 1"); }
    this.maxDepth = depth;
  }

  public synchronized void push(Object obj) {
    // we could slightly optimize the mostRecent() call by specifically storing the reference
    // to the last object added in a dedicated variable
    data.addFirst(obj);
    if (data.size() > maxDepth) {
      data.removeLast();
    }
  }

  public synchronized Object pop() {
    if (data.isEmpty()) { throw new IllegalStateException("stack empty"); }
    return data.removeFirst();
  }

  public synchronized Object[] toArray(Object[] type) {
    return data.toArray(type);
  }

  public synchronized Object peek() {
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
