/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.util.Assert;

/**
 * Contains components created during L2-connection time
 */
public class PreparedComponentsFromL2Connection {
  private final L1ConfigurationSetupManager config;

  public PreparedComponentsFromL2Connection(L1ConfigurationSetupManager config) {
    Assert.assertNotNull(config);
    this.config = config;
  }

  public ConnectionInfoConfig createConnectionInfoConfigItem() {
    L2Data[] l2s = this.config.l2Config().l2Data();
    for (L2Data l2 : l2s) {
      l2.setGroupId(0);
    }
    return new ConnectionInfoConfig(l2s, config.getSecurityInfo());
  }

  public ConnectionInfoConfig[] createConnectionInfoConfigItemByGroup() {
    /**
     * this block is synchronized because of the apache bug https://issues.apache.org/jira/browse/XMLBEANS-328. In multi
     * threaded environment we used to get ArrayIndexOutOfBoundsException See MNK-1984, 2010, 2013 for more details
     */
    synchronized (this.config) {
      this.config.l2Config().l2Data();
    }

    L2Data[][] l2DataByGroup = this.config.l2Config().getL2DataByGroup();
    // set GroupID assigned by L2
    // notes: this.config.l2config() has called ActiveCoordinatorHelper.generateGroupNames(), so that to have a right
    // group name to work with.
    for (L2Data[] group : l2DataByGroup) {
      for (L2Data l2 : group) {
        l2.setGroupId(0);
      }
    }

    ConnectionInfoConfig[] items = new ConnectionInfoConfig[l2DataByGroup.length];
    for (int i = 0; i < l2DataByGroup.length; i++) {
      items[i] = new ConnectionInfoConfig(l2DataByGroup[i], config.getSecurityInfo());
    }
    return items;
  }

}
