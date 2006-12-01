/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

public class SettableBoolValueTest extends TCTestCase {

  public final void testClone() {
    SettableBoolValue original = new SettableBoolValue();
    original.setBool(true);
    SettableBoolValue copy = (SettableBoolValue) original.clone();
    assertEquals(original, copy);
  }

  public final void testSetBool() {
    SettableBoolValue bool = new SettableBoolValue();
    assertFalse(bool.boolValue());
    assertFalse(bool.isSet());

    assertTrue(bool.boolValue(true));

    bool.setBool(true);
    assertTrue(bool.boolValue());
    assertTrue(bool.boolValue(false));
    assertTrue(bool.boolValue(true));
  }
}
