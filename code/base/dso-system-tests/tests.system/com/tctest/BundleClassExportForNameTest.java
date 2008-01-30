package com.tctest;

public class BundleClassExportForNameTest extends TransparentTestBase {
  public static final int NODE_COUNT = 1;

  public BundleClassExportForNameTest() {
    //
  }

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(NODE_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return BundleClassExportForNameTestApp.class;
  }
}
