/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

public class ActiveCoordinatorHelperTest extends TCTestCase {
  public void testGetGroupNameFrom() {
    String[] temp = new String[10];
    for (int i = 0; i < 10; i++) {
      temp[i] = "" + (9 - i);
    }

    String temp2 = ActiveCoordinatorHelper.getGroupNameFrom(temp);

    String temp3 = "";
    for (int i = 0; i < 10; i++) {
      temp3 = temp3 + i;
    }

    Assert.assertEquals(temp3, temp2);
  }
}
