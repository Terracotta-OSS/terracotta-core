/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.ArrayList;

public class YoungGCConfigurationHelper extends GCConfigurationHelper {

  // increasing Full DGC time to 1 minute to give change to Young Gen DGC to run
  @Override
  public int getGarbageCollectionInterval() {
    return 60;
  }

  // Run Young Gen every 10 seconds
  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY, "10000");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_ENABLED, "true");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY, "10000");

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_ENABLED + "=true");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY + "=10000");
  }
}
