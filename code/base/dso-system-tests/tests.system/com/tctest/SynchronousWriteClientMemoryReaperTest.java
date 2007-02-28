/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import java.util.HashMap;
import java.util.Map;

public class SynchronousWriteClientMemoryReaperTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT    = 2;
  private static final int THREADS_COUNT = 2;

  protected Class getApplicationClass() {
    return ClientMemoryReaperTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }

  protected Map getOptionalAttributes() {
    Map attributes = new HashMap();
    attributes.put(ClientMemoryReaperTestApp.SYNCHRONOUS_WRITE, "true");
    return attributes;
  }
}
