/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.SystemConfigObject;
import com.terracottatech.config.System;
import com.terracottatech.config.TcConfigDocument.TcConfig;

/**
 * Unit/subsystem test for {@link SystemConfigObject}.
 */
public class SystemConfigObjectTest extends ConfigObjectTestBase {

  public void setUp() throws Exception {
    super.setUp(System.class);
  }

  protected XmlObject getBeanFromTcConfig(TcConfig config) throws Exception {
    return config.getSystem();
  }

  public void testConstruction() throws Exception {
    try {
      new SystemConfigObject(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }
}
