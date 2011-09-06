/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tctest.TransparentTestIface;

public class ClientTerminatingSynchronousCommitTest extends ClientTerminatingTest {

  public ClientTerminatingSynchronousCommitTest() {
    super();
    super.setSynchronousWrite();
  }

  public void doSetup(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setAttribute(ClientTerminatingTestApp.FORCE_KILL, "true");
  }

}
