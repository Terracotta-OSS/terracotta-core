/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * Latch that allows your to reset the latch so a call to acquire
 * may wait again.
 */
public class ResetableLatch extends Latch {

  /**
   * reset, to wait on acquire.
   */
  public synchronized void reset() {
    this.latched_ = false;
  }
  
}
