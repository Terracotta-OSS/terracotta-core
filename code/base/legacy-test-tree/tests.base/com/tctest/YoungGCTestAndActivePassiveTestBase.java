/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import java.util.ArrayList;

public abstract class YoungGCTestAndActivePassiveTestBase extends GCAndActivePassiveTestBase {

  public YoungGCTestAndActivePassiveTestBase() {
    gcConfigHelper = new YoungGCConfigurationHelper();
  }

  // Run Young Gen every 10 seconds
  @Override
  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    super.setExtraJvmArgs(jvmArgs);
    ((YoungGCConfigurationHelper) gcConfigHelper).setExtraJvmArgs(jvmArgs);
  }

}
