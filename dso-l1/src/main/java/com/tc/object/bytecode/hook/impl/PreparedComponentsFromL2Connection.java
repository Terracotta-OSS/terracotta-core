/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.net.GroupID;
import com.tc.object.DistributedObjectClient;
import com.tc.object.config.ConfigInfoFromL2Impl;
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.util.Assert;

import java.util.Map;
import java.util.TreeSet;

/**
 * Contains components created during L2-connection time, in {@link DSOContextImpl}, that are needed by the
 * {@link DistributedObjectClient} eventually.
 */
public class PreparedComponentsFromL2Connection {
  private final L1ConfigurationSetupManager config;
  private final Map<String, GroupID>        groupnameIDMap;

  public PreparedComponentsFromL2Connection(L1ConfigurationSetupManager config) {
    Assert.assertNotNull(config);
    this.config = config;
    this.groupnameIDMap = readGroupnameIDMapFromL2();
  }

  private Map<String, GroupID> readGroupnameIDMapFromL2() {
    Map<String, GroupID> map = null;
    try {
      map = new ConfigInfoFromL2Impl(this.config).getGroupNameIDMapFromL2();
    } catch (ConfigurationSetupException e) {
      throw new TCRuntimeException(e);
    }
    return map;
  }

  private int getGroupID(String gname) {
    GroupID id = this.groupnameIDMap.get(gname);
    if (id != null) {
      return id.toInt();
    } else {
      throw new TCRuntimeException("No L2 GroupID mapping for " + gname);
    }
  }

  public ConnectionInfoConfig createConnectionInfoConfigItem() {
    L2Data[] l2s = this.config.l2Config().l2Data();
    for (L2Data l2 : l2s) {
      l2.setGroupId(getGroupID(l2.getGroupName()));
    }
    return new ConnectionInfoConfig(l2s);
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
    // Groupname to work with.
    for (L2Data[] group : l2DataByGroup) {
      for (L2Data l2 : group) {
        l2.setGroupId(getGroupID(l2.getGroupName()));
      }
    }

    ConnectionInfoConfig[] items = new ConnectionInfoConfig[l2DataByGroup.length];
    for (int i = 0; i < l2DataByGroup.length; i++) {
      items[i] = new ConnectionInfoConfig(l2DataByGroup[i]);
    }
    return items;
  }

  public boolean isActiveActive() {
    ConnectionInfoConfig[] groups = createConnectionInfoConfigItemByGroup();
    return (groups.length > 1);
  }

  public GroupID[] getGroupIDs() {
    TreeSet<GroupID> sortedGids = new TreeSet<GroupID>(this.groupnameIDMap.values());
    GroupID[] gidArray = new GroupID[sortedGids.size()];
    return sortedGids.toArray(gidArray);
  }

}
