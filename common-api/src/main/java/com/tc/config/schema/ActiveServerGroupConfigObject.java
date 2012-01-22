/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManagerImpl;
import com.tc.net.GroupID;
import com.tc.util.Assert;
import com.terracottatech.config.Ha;
import com.terracottatech.config.Members;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

public class ActiveServerGroupConfigObject extends BaseConfigObject implements ActiveServerGroupConfig {

  // TODO: the defaultValueProvider is not implemented to fetch default values
  // for attributes... possibly fix this and
  // use the commented code to set defaultId:
  // int defaultId = ((XmlInteger)
  // defaultValueProvider.defaultFor(serversBeanRepository.rootBeanSchemaType(),
  // "active-server-groups/active-server-group[@id]")).getBigIntegerValue().intValue();
  public static final int     defaultGroupId = 0;

  private GroupID             groupId;
  private String              grpName;
  private final HaConfigSchema   haConfig;
  private final MembersConfig membersConfig;

  public ActiveServerGroupConfigObject(ConfigContext context, L2ConfigurationSetupManagerImpl setupManager) {
    super(context);
    context.ensureRepositoryProvides(MirrorGroup.class);
    MirrorGroup group = (MirrorGroup) context.bean();

    String groupName = group.getGroupName();
    this.grpName = groupName;

    membersConfig = new MembersConfigObject(createContext(setupManager, true, group));
    haConfig = new HaConfigObject(createContext(setupManager, false, group));
  }

  public void setGroupId(GroupID groupId) {
    this.groupId = groupId;
  }

  public HaConfigSchema getHaHolder() {
    return this.haConfig;
  }
  
  public void setGroupName(String groupName) {
    this.grpName = groupName;
  }

  public String getGroupName() {
    return grpName;
  }

  public MembersConfig getMembers() {
    return this.membersConfig;
  }

  public GroupID getGroupId() {
    return this.groupId;
  }

  private final ConfigContext createContext(L2ConfigurationSetupManagerImpl setupManager, boolean isMembers,
                                            final MirrorGroup group) {
    if (isMembers) {
      ChildBeanRepository beanRepository = new ChildBeanRepository(setupManager.serversBeanRepository(), Members.class,
                                                                   new ChildBeanFetcher() {
                                                                     public XmlObject getChild(XmlObject parent) {
                                                                       return group.getMembers();
                                                                     }
                                                                   });
      return setupManager.createContext(beanRepository, setupManager.getConfigFilePath());
    } else {
      ChildBeanRepository beanRepository = new ChildBeanRepository(setupManager.serversBeanRepository(), Ha.class,
                                                                   new ChildBeanFetcher() {
                                                                     public XmlObject getChild(XmlObject parent) {
                                                                       return group.getHa();
                                                                     }
                                                                   });
      return setupManager.createContext(beanRepository, setupManager.getConfigFilePath());
    }
  }

  public boolean isMember(String l2Name) {
    String[] members = getMembers().getMemberArray();
    for (int i = 0; i < members.length; i++) {
      if (members[i].equals(l2Name)) { return true; }
    }
    return false;
  }

  public static void createDefaultMirrorGroup(Servers servers, Ha ha) throws ConfigurationSetupException {
    Assert.assertTrue(servers.isSetMirrorGroups());
    Assert.assertEquals(0, servers.getMirrorGroups().getMirrorGroupArray().length);
    
    MirrorGroup mirrorGroup = servers.getMirrorGroups().addNewMirrorGroup();
    mirrorGroup.setHa(ha);
    Members members = mirrorGroup.addNewMembers();
    
    Server[] serverArray = servers.getServerArray();

    for (int i = 0; i < serverArray.length; i++) {
      // name for each server should exist
      String name = serverArray[i].getName();
      if (name == null || name.equals("")) { throw new ConfigurationSetupException(
                                                                                   "server's name not defined... name=["
                                                                                       + name + "] serverDsoPort=["
                                                                                       + serverArray[i].getDsoPort()
                                                                                       + "]"); }
      members.insertMember(i, serverArray[i].getName());
    }
  }
  
}
