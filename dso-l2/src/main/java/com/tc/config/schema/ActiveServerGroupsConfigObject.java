/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
