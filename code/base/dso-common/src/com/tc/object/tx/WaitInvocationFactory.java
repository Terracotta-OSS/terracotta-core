/*
 * Created on May 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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