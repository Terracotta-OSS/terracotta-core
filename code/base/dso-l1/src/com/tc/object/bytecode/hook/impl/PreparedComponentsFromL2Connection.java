/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.config.ConnectionInfoConfigItem;
import com.tc.util.Assert;

/**
 * Contains components created during L2-connection time, in {@link DSOContextImpl}, that are needed by the
 * {@link DistributedObjectClient} eventually.
 */
public class PreparedComponentsFromL2Connection {
  private final L1TVSConfigurationSetupManager config;

  public PreparedComponentsFromL2Connection(L1TVSConfigurationSetupManager config) {
    Assert.assertNotNull(config);
    this.config = config;
  }

  public ConfigItem createConnectionInfoConfigItem() {
    return new ConnectionInfoConfigItem(this.config.l2Config().l2Data());
  }
}