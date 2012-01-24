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
import com.tc.util.Assert;
import com.terracottatech.config.Ha;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.Servers;

public class ActiveServerGroupsConfigObject extends BaseConfigObject implements ActiveServerGroupsConfig {
  private final ActiveServerGroupConfig[] groupConfigArray;
  private final int                       activeServerGroupCount;

  public ActiveServerGroupsConfigObject(ConfigContext context, L2ConfigurationSetupManagerImpl setupManager)
      throws ConfigurationSetupException {
    super(context);
    context.ensureRepositoryProvides(MirrorGroups.class);

    MirrorGroups groups = (MirrorGroups) context.bean();
    if (groups == null) { throw new AssertionError(
                                                   "ActiveServerGroups is null!  This should never happen since we make sure default is used."); }

    final MirrorGroup[] groupArray = groups.getMirrorGroupArray();

    if (groupArray == null || groupArray.length == 0) { throw new AssertionError(
                                                                                 "ActiveServerGroup array is null!  This should never happen since we make sure default is used."); }

    this.activeServerGroupCount = groupArray.length;

    ActiveServerGroupConfigObject[] tempGroupConfigArray = new ActiveServerGroupConfigObject[groupArray.length];

    for (int i = 0; i < tempGroupConfigArray.length; i++) {
      tempGroupConfigArray[i] = new ActiveServerGroupConfigObject(createContext(setupManager, groupArray[i]),
                                                                  setupManager);
    }
    this.groupConfigArray = ActiveCoordinatorHelper.generateGroupInfo(tempGroupConfigArray);
  }

  public int getActiveServerGroupCount() {
    return this.activeServerGroupCount;
  }

  public ActiveServerGroupConfig[] getActiveServerGroupArray() {
    return groupConfigArray;
  }

  public ActiveServerGroupConfig getActiveServerGroupForL2(String name) {
    for (int groupCount = 0; groupCount < activeServerGroupCount; groupCount++) {
      if (groupConfigArray[groupCount].isMember(name)) { return groupConfigArray[groupCount]; }
    }
    return null;
  }

  private final ConfigContext createContext(L2ConfigurationSetupManagerImpl setupManager, final MirrorGroup group) {
    ChildBeanRepository beanRepository = new ChildBeanRepository(setupManager.serversBeanRepository(),
                                                                 MirrorGroup.class, new ChildBeanFetcher() {
                                                                   public XmlObject getChild(XmlObject parent) {
                                                                     return group;
                                                                   }
                                                                 });
    return setupManager.createContext(beanRepository, setupManager.getConfigFilePath());
  }

  public static void createDefaultServerMirrorGroups(Servers servers, DefaultValueProvider defaultValueProvider)
      throws ConfigurationSetupException {
    Ha ha = servers.getHa();
    Assert.assertNotNull(ha);
    servers.addNewMirrorGroups();
    ActiveServerGroupConfigObject.createDefaultMirrorGroup(servers, ha);
  }

  public static void initializeMirrorGroups(Servers servers, DefaultValueProvider defaultValueProvider)
      throws ConfigurationSetupException {
    Assert.assertTrue(servers.isSetHa());
    if (!servers.isSetMirrorGroups()) {
      createDefaultServerMirrorGroups(servers, defaultValueProvider);
    } else {
      MirrorGroup[] mirrorGroups = servers.getMirrorGroups().getMirrorGroupArray();
      if (mirrorGroups.length == 0) {
        ActiveServerGroupConfigObject.createDefaultMirrorGroup(servers, servers.getHa());
      }

      for (MirrorGroup mirrorGroup : mirrorGroups) {
        if (!mirrorGroup.isSetHa()) {
          mirrorGroup.setHa(servers.getHa());
        } else {
          HaConfigObject.checkAndInitializeHa(mirrorGroup.getHa(), servers.getHa());
        }
      }
    }
  }
}
