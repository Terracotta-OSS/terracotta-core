/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class EnumSetTest extends TransparentTestBase {

  private static final int NODE_COUNT = EnumSetTestApp.Utensil.values().length;

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(NODE_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return EnumSetTestApp.class;
  }

}
