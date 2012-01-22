/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.util.Util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NoExceptionLinkedQueue extends LinkedBlockingQueue {

  @Override
  public void put(Object o) {
    boolean interrupted = false;
    while (true) {
      try {
        super.put(o);
        Util.selfInterruptIfNeeded(interrupted);
        return;
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
  }

  public boolean offer(Object o, long l) {
    try {
      return super.offer(o, l, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  public Object poll(long arg0) {
    try {
      return super.poll(arg0, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  @Override
  public Object take() {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return super.take();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(interrupted);
    }
  }

}
