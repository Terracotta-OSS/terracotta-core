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
package com.tc.util.concurrent;

import junit.framework.TestCase;

/**
 * test cases for SetOnceRef
 * 
 * @author teck
 */
public class SetOnceRefTest extends TestCase {

  public void testAllowsNull() {
    SetOnceRef ref = new SetOnceRef(false);

    assertFalse(ref.allowsNull());

    try {
      ref.set(null);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    Object val = new Object();
    ref.set(val);

    assertTrue(ref.get() == val);
  }

  public void testSetNull() {
    final SetOnceRef ref1 = new SetOnceRef(true);
    ref1.set(null);
    final SetOnceRef ref2 = new SetOnceRef(null, true);

    assertTrue(ref1.get() == null);
    assertTrue(ref2.get() == null);
  }

  public void testSetTwice() {
    final Object val1 = new Object();
    final Object val2 = new Object();

    final SetOnceRef ref1 = new SetOnceRef();
    ref1.set(val1);
    final SetOnceRef ref2 = new SetOnceRef(val2);

    assertTrue(ref1.get() == val1);
    assertTrue(ref2.get() == val2);

    try {
      ref1.set(val1);
      fail();
    } catch (IllegalStateException e) {
      // expected
    }

    try {
      ref2.set(val2);
      fail();
    } catch (IllegalStateException e) {
      // expected
    }
  }

  public void testGetMany() {
    final Object JUnitGetsMeHard = new Object();
    final SetOnceRef ref = new SetOnceRef(JUnitGetsMeHard);

    for (int i = 0; i < 1000; i++) {
      assertTrue(ref.get() == JUnitGetsMeHard);
    }
  }

  public void testThreadAccess() throws InterruptedException {
    final Object val = new Object();

    final SetOnceRef ref = new SetOnceRef();

    Thread other = new Thread(new Runnable() {
      @Override
      public void run() {
        ref.set(val);
      }
    });

    other.start();
    other.join();

    assertTrue(ref.get() == val);
  }

}
