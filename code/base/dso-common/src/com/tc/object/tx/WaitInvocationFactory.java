/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.util.Assert;

public class WaitInvocationFactory {
  public WaitInvocation newWaitInvocation(int waitArgCount, long millis, int nanos) {
    switch (waitArgCount) {
      case 2: {
        return new WaitInvocation(millis, nanos);
      }
      case 1: {
        return new WaitInvocation(millis);
      }
      case 0: {
        return new WaitInvocation();
      }
      default: {
        throw Assert.failure("Invalid wait argument count: " + waitArgCount);
      }
    }
  }
}