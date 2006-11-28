/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class SerialVersionUIDTest extends TransparentTestBase {

  private static final int   NODE_COUNT    = 5;

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
    getTransparentAppConfig().setAttribute(SerialVersionUIDTestApp.TEMP_FILE_KEY, getTempFile("TempSerializedData").getAbsolutePath());
  }

  protected Class getApplicationClass() {
    return SerialVersionUIDTestApp.class;
  }

}
