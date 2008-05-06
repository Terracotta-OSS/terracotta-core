/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import java.util.HashMap;
import java.util.Map;

public class LinkedHashMapSynchronousWriteTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Map getOptionalAttributes() {
    Map attributes = new HashMap();
    attributes.put(LinkedHashMapTestApp.SYNCHRONOUS_WRITE, "true");
    return attributes;
  }

  protected Class getApplicationClass() {
    return LinkedHashMapTestApp.class;
  }

}
