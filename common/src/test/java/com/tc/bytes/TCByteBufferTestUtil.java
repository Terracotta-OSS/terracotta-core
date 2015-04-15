/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
