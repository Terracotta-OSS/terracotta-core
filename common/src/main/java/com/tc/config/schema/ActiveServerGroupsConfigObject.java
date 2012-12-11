/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManagerImpl;
import com.tc.util.ActiveCoordinatorHelper;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Servers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ActiveServerGroupsConfigObject extends BaseConfigObject implements ActiveServerGroupsConfig {
  private final List<ActiveServerGroupConfig> groupConfigs;

  public ActiveServerGroupsConfigObject(ConfigContext context, L2ConfigurationSetupManagerImpl setupManager)
      throws ConfigurationSetupException {
    super(context);
    context.ensureRepositoryProvides(Servers.class);

    Servers servers = (Servers) context.bean();
    final MirrorGroup[] groupArray = servers.getMirrorGroupArray();

    if (groupArray == null || groupArray.length == 0) { throw new AssertionError(
                                                                                 "ActiveServerGroup array is null!  This should never happen since we make sure default is used."); }

    ActiveServerGroupConfigObject[] tempGroupConfigArray = new ActiveServerGroupConfigObject[groupArray.length];

    for (int i = 0; i < tempGroupConfigArray.length; i++) {
      tempGroupConfigArray[i] = new ActiveServerGroupConfigObject(createContext(setupManager, groupArray[i]),
                                                                  setupManager);
    }
    final ActiveServerGroupConfig[] activeServerGroupConfigObjects = ActiveCoordinatorHelper
        .generateGroupInfo(tempGroupConfigArray);
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

  private final ConfigContext createContext(L2ConfigurationSetupManagerImpl setupManager, final MirrorGroup group) {
    ChildBeanRepository beanRepository = new ChildBeanRepository(setupManager.serversBeanRepository(),
                                                                 MirrorGroup.class, new ChildBeanFetcher() {
                                                                   @Override
                                                                   public XmlObject getChild(XmlObject parent) {
                                                                     return group;
                                                                   }
                                                                 });
    return setupManager.createContext(beanRepository, setupManager.getConfigFilePath());
  }

  public static void initializeMirrorGroups(Servers servers, DefaultValueProvider defaultValueProvider) {
    if (servers.getMirrorGroupArray() == null || servers.getMirrorGroupArray().length == 0) {
      ActiveServerGroupConfigObject.createDefaultMirrorGroup(servers);
    }
  }
}
