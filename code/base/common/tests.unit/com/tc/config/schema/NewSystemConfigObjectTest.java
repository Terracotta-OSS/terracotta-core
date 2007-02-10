/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.terracottatech.configV2.System;
import com.terracottatech.configV2.TcConfigDocument.TcConfig;

/**
 * Unit/subsystem test for {@link NewSystemConfigObject}.
 */
public class NewSystemConfigObjectTest extends ConfigObjectTestBase {

  public void setUp() throws Exception {
    super.setUp(System.class);
  }

  protected XmlObject getBeanFromTcConfig(TcConfig config) throws Exception {
    return config.getSystem();
  }

  public void testConstruction() throws Exception {
    try {
      new NewSystemConfigObject(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }
}
