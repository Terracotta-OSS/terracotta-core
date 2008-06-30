package com.tctest;

public class BundleClassExportLoadClassTest extends TransparentTestBase {
  public static final int NODE_COUNT = 1;

  public BundleClassExportLoadClassTest() {
    // CDV-598
    disableAllUntil("2009-06-30");
  }

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(NODE_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return BundleClassExportLoadClassTestApp.class;
  }
}
