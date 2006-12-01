/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

public class SettableIntValueTest extends TCTestCase {

  public final void testSettableInt() {
    SettableIntValue siv = new SettableIntValue();
    final int defaultValue = 12345;
    final int actualValue = 9999;
    assertFalse(defaultValue == actualValue);
    assertEquals(defaultValue, siv.intValue(defaultValue));
    
    siv.setInt(actualValue);
    assertEquals(actualValue, siv.intValue(defaultValue));
    
    siv.unset();
    assertEquals(defaultValue, siv.intValue(defaultValue));
    
    
  }

}
