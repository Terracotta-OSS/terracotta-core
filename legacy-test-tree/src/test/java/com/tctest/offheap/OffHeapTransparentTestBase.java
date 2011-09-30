package com.tctest.offheap;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tctest.TransparentTestBase;

import java.util.ArrayList;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

public abstract class OffHeapTransparentTestBase extends TransparentTestBase {

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
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
    return true;
  }

  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    super.setupConfig(configFactory);
    configFactory.setOffHeapConfigObject(true, getMaxDataSizeInMB() + "m");
  }

}
