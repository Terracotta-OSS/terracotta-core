/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.StandardL2TVSConfigurationSetupManager;
import com.terracottatech.config.ActiveServerGroup;
import com.terracottatech.config.ActiveServerGroups;
import com.terracottatech.config.Ha;

public class ActiveServerGroupsConfigObject extends BaseNewConfigObject implements ActiveServerGroupsConfig {
  private final ActiveServerGroupConfig[] groupConfigArray;
  private final int                       activeServerGroupCount;

  public ActiveServerGroupsConfigObject(ConfigContext context, StandardL2TVSConfigurationSetupManager setupManager) {
    super(context);
    context.ensureRepositoryProvides(ActiveServerGroups.class);
    final ActiveServerGroups groups = (ActiveServerGroups) context.bean();

    if (groups == null) { throw new AssertionError(
                                                   "ActiveServerGroups is null!  This should never happen since we make sure default is used."); }

    final ActiveServerGroup[] groupArray = groups.getActiveServerGroupArray();

    if (groupArray == null || groupArray.length == 0) { throw new AssertionError(
                                                                                 "ActiveServerGroup array is null!  This should never happen since we make sure default is used."); }

    this.activeServerGroupCount = groupArray.length;

    this.groupConfigArray = new ActiveServerGroupConfig[groupArray.length];

    for (int i = 0; i < groupArray.length; i++) {
      // if no Ha element defined for this group then set it to common ha
      if (!groupArray[i].isSetHa()) {
        groupArray[i].setHa(setupManager.getCommonHa());
      }
      this.groupConfigArray[i] = new ActiveServerGroupConfigObject(createContext(setupManager, groupArray[i]),
                                                                   setupManager, i);
    }
  }

  public int getActiveServerGroupCount() {
    return this.activeServerGroupCount;
  }

  public ActiveServerGroupConfig[] getActiveServerGroupArray() {
    return groupConfigArray;
  }

  private final ConfigContext createContext(StandardL2TVSConfigurationSetupManager setupManager,
                                            final ActiveServerGroup group) {
    ChildBeanRepository beanRepository = new ChildBeanRepository(setupManager.serversBeanRepository(),
                                                                 ActiveServerGroup.class, new ChildBeanFetcher() {
                                                                   public XmlObject getChild(XmlObject parent) {
                                                                     return group;
                                                                   }
                                                                 });
    return setupManager.createContext(beanRepository, setupManager.getConfigFilePath());
  }

  public static ActiveServerGroups getDefaultActiveServerGroups(DefaultValueProvider defaultValueProvider,
                                                                MutableBeanRepository serversBeanRepository, Ha commonHa)
      throws ConfigurationSetupException {
    ActiveServerGroups asgs = ActiveServerGroups.Factory.newInstance();
    ActiveServerGroup[] groupArray = new ActiveServerGroup[1];
    groupArray[0] = ActiveServerGroupConfigObject.getDefaultActiveServerGroup(defaultValueProvider,
                                                                              serversBeanRepository, commonHa);
    asgs.setActiveServerGroupArray(groupArray);
    return asgs;
  }

  public ActiveServerGroupConfig getActiveServerGroupForL2(String name) {
    for (int groupCount = 0; groupCount < activeServerGroupCount; groupCount++) {
      if (groupConfigArray[groupCount].isMember(name)) { return groupConfigArray[groupCount]; }
    }
    return null;
  }
}
