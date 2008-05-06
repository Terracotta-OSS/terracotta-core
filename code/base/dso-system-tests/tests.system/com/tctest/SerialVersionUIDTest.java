/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
