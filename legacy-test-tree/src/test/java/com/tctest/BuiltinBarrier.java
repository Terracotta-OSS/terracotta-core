/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import java.util.concurrent.BrokenBarrierException;

public interface BuiltinBarrier {

  int await() throws InterruptedException, BrokenBarrierException;

}
