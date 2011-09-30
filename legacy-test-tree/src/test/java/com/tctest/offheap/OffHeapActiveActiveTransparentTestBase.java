/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.offheap;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tctest.ActiveActiveTransparentTestBase;

import java.util.ArrayList;

public abstract class OffHeapActiveActiveTransparentTestBase extends ActiveActiveTransparentTestBase {

  @Override
  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    jvmArgs.add("-XX:MaxDirectMemorySize=" + getJVMArgsMaxDirectMemorySize());

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_SKIP_JVMARG_CHECK + "=true");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_INITIAL_DATASIZE + "=1m");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_TABLESIZE + "=1m");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_OBJECT_CACHE_CONCURRENCY + "=16");

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_MAX_PAGE_SIZE + "=10k");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_MIN_PAGE_SIZE + "=10k");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OFFHEAP_MAP_CACHE_TABLESIZE + "=1k");
  }

  protected String getJVMArgsMaxDirectMemorySize() {
    return (getMaxDataSizeInMB() + 56) + "m";
  }

  protected int getMaxDataSizeInMB() {
    return 200;
  }

  @Override
  protected boolean useExternalProcess() {
    // we are not running AP servers as external process as AP servers get intialized
    // at setUpActivePassiveServers() will not be called if external servers are enabled.
    return false;
  }

  @Override
  protected void setupConfig(final TestConfigurationSetupManagerFactory configFactory) {
    super.setupConfig(configFactory);
    configFactory.setOffHeapConfigObject(true, getMaxDataSizeInMB() + "m");
  }

}
