/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

import com.tc.util.Assert;

public class TCByteBufferTestUtil {

  public static void checkEquals(TCByteBuffer[] expected, TCByteBuffer[] actual) {
    System.out.println("expected length = " + expected.length + " actual length = " + actual.length);
    int j = 0;
    for (int i = 0; i < expected.length; i++) {
      while (expected[i].remaining() > 0) {
        byte expectedValue = expected[i].get();
        while (actual[j].remaining() == 0) {
          j++;
        }
        if (actual[j].get() != expectedValue) { throw new AssertionError("Data is not the same " + i + " " + j + " "
                                                                         + expected[i] + " " + actual[j]
                                                                         + " expected Value = " + expectedValue); }
      }
    }
    Assert.assertEquals(actual.length, j + 1);
    Assert.assertEquals(0, actual[j].remaining());
  }
  
}
