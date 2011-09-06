/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.handler;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.ResourceBundle;

public class CallbackHandlerResourcesTest extends TCTestCase {

  private ResourceBundle resources;

  @Override
  protected void setUp() throws Exception {
    this.resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".messages");
  }

  public void testResources() {
    Assert.assertTrue(CallbackHandlerResources.getDirtyDBAutodeleteAutoRestartZapMessage()
        .equals(this.resources.getObject("dirtydb.zap.autodelete.autorestart")));
    Assert.assertTrue(CallbackHandlerResources.getDirtyDBAutodeleteZapMessage()
        .equals(this.resources.getObject("dirtydb.zap.autodelete")));
    Assert.assertTrue(CallbackHandlerResources.getDirtyDBAutoRestartZapMessage()
        .equals(this.resources.getObject("dirtydb.zap.autorestart")));
    Assert.assertTrue(CallbackHandlerResources.getDirtyDBZapMessage().equals(this.resources.getObject("dirtydb.zap")));
  }
}
