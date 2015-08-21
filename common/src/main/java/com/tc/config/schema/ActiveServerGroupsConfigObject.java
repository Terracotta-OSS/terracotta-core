/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManagerImpl;
import com.tc.util.ActiveCoordinatorHelper;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ActiveServerGroupsConfigObject implements ActiveServerGroupsConfig {
  private final List<ActiveServerGroupConfig> groupConfigs;
  private final Servers servers;

  public ActiveServerGroupsConfigObject(Servers context, L2ConfigurationSetupManagerImpl setupManager)
      throws ConfigurationSetupException {

    servers = context;

    ActiveServerGroupConfigObject[] tempGroupConfigArray = new ActiveServerGroupConfigObject[1];
    for(Server s : servers.getServer()) {
      tempGroupConfigArray[0] = new ActiveServerGroupConfigObject(servers, setupManager);
    }
    final ActiveServerGroupConfig[] activeServerGroupConfigObjects = ActiveCoordinatorHelper.generateGroupInfo(tempGroupConfigArray);
    this.groupConfigs = Collections.unmodifiableList(Arrays.asList(activeServerGroupConfigObjects));
  }


  @Override
  public int getActiveServerGroupCount() {
    return this.groupConfigs.size();
  }

  @Override
  public List<ActiveServerGroupConfig> getActiveServerGroups() {
    return groupConfigs;
  }

  @Override
  public ActiveServerGroupConfig getActiveServerGroupForL2(String name) {
    for (ActiveServerGroupConfig activeServerGroupConfig : groupConfigs) {
      if (activeServerGroupConfig.isMember(name)) { return activeServerGroupConfig; }
    }
    return null;
  }

  public static void initializeMirrorGroups(Servers servers) {
  }

  @Override
  public Servers getBean() {
    return servers;
  }
}
