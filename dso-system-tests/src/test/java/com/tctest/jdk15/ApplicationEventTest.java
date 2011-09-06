/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class ApplicationEventTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  static {
    System.setProperty("project.name", "test"); // this causes application events to be sent
  }
  
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    t.getTransparentAppConfig().setAttribute("jmx-port", getAdminPort());
  }

  protected Class getApplicationClass() {
    return ApplicationEventTestApp.class;
  }

}
