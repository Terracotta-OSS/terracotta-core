/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
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

  public ConfigItem[] createConnectionInfoConfigItemByGroup() {
    // this initializes the data structures in L2ConfigForL1Object
    this.config.l2Config().l2Data().getObject();

    ObjectArrayConfigItem[] l2DataByGroup = this.config.l2Config().getL2DataByGroup();
    ConfigItem[] items = new ConfigItem[l2DataByGroup.length];
    for (int i = 0; i < l2DataByGroup.length; i++) {
      items[i] = new ConnectionInfoConfigItem(l2DataByGroup[i]);
    }
    return items;
  }
  
  public boolean isActiveActive() {
    ConfigItem[] groups = createConnectionInfoConfigItemByGroup();
    return (groups.length > 1);
  }

}
