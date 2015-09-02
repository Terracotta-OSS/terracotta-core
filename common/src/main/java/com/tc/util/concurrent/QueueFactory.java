/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueFactory<E> {

  public BlockingQueue<E> createInstance() {
    return new LinkedBlockingQueue<E>();
  }

  public BlockingQueue<E> createInstance(int capacity) {
    return new LinkedBlockingQueue<E>(capacity);
  }

}
