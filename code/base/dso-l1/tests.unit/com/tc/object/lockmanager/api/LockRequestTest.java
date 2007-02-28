/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import java.util.Random;

import junit.framework.TestCase;

public class LockRequestTest extends TestCase {
  public void testEqualsAndHashCode() throws Exception {
    Random random = new Random();
    int instanceCount = 1000000;

    LockRequest notEqual = new LockRequest(new LockID("nothing like me"), new ThreadID(Integer.MAX_VALUE),
                                           LockLevel.WRITE);
    long[] samples = new long[instanceCount];
    for (int i = 0; i < instanceCount; i++) {
      LockID lid = new LockID(random.nextInt(Integer.MAX_VALUE - 1) + "");
      ThreadID tid = new ThreadID(random.nextInt(Integer.MAX_VALUE - 1));
      int lockType = (i % 2 == 0) ? LockLevel.READ : LockLevel.WRITE;

      long t0 = System.currentTimeMillis();
      LockRequest a = new LockRequest(lid, tid, lockType);
      samples[i] = System.currentTimeMillis() - t0;

      LockRequest b = new LockRequest(new LockID(lid.asString()), new ThreadID(tid.toLong()), lockType);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertFalse(a.equals(notEqual));
      assertFalse(b.equals(notEqual));
    }
    long min = 0, max = 0, sum = 0;
    for (int i = 0; i < samples.length; i++) {
      if (samples[i] < min) min = samples[i];
      if (samples[i] > max) max = samples[i];
      sum += samples[i];
    }
    double mean = sum / samples.length;
    System.out.println("min: " + min + ", max: " + max + ", mean: " + mean);
  }
}
