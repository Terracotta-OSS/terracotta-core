/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
        while (!actual[j].hasRemaining()) {
          j++;
          if (j >= actual.length) { throw new AssertionError("ran out of buffers: " + j); }
        }

        byte actualValue = actual[j].get();
        if (actualValue != expectedValue) {
          //
          throw new AssertionError("Data is not the same, " + actualValue + "!=" + expectedValue + " at expected[" + i
                                   + "] and actual[" + j + "]");
        }
      }
    }

    if (actual.length != 0) {
      Assert.assertEquals(actual.length, j + 1);
      Assert.assertEquals(0, actual[j].remaining());
    }
  }
}
