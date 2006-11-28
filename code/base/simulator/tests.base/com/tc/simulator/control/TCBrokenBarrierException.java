/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.control;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;

public class TCBrokenBarrierException extends Exception {

  public TCBrokenBarrierException(BrokenBarrierException e) {
    super(e);
  }

}
