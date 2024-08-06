/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
    SetOnceRef<Object> ref = new SetOnceRef<Object>(false);

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
    final SetOnceRef<Object> ref1 = new SetOnceRef<Object>(true);
    ref1.set(null);
    final SetOnceRef<Object> ref2 = new SetOnceRef<Object>(null, true);

    assertTrue(ref1.get() == null);
    assertTrue(ref2.get() == null);
  }

  public void testSetTwice() {
    final Object val1 = new Object();
    final Object val2 = new Object();

    final SetOnceRef<Object> ref1 = new SetOnceRef<Object>();
    ref1.set(val1);
    final SetOnceRef<Object> ref2 = new SetOnceRef<Object>(val2);

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
    final SetOnceRef<Object> ref = new SetOnceRef<Object>(JUnitGetsMeHard);

    for (int i = 0; i < 1000; i++) {
      assertTrue(ref.get() == JUnitGetsMeHard);
    }
  }

  public void testThreadAccess() throws InterruptedException {
    final Object val = new Object();

    final SetOnceRef<Object> ref = new SetOnceRef<Object>();

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
