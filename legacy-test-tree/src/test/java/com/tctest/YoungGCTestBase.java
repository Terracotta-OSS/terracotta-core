/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import java.util.ArrayList;

public abstract class YoungGCTestBase extends GCTestBase {

  public YoungGCTestBase() {
    gcConfigHelper = new YoungGCConfigurationHelper();
  }

  // Run Young Gen every 10 seconds
  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    ((YoungGCConfigurationHelper) gcConfigHelper).setExtraJvmArgs(jvmArgs);
  }

}
