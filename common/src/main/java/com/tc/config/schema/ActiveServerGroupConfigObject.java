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
  public static final int     defaultGroupId         = 0;

  private static final int    DEFAULT_ELECETION_TIME = 5;

  private GroupID             groupId;
  private String              grpName;
  private final MembersConfig membersConfig;
  private final MirrorGroup   group;

  public ActiveServerGroupConfigObject(ConfigContext context, L2ConfigurationSetupManagerImpl setupManager) {
    super(context);
    context.ensureRepositoryProvides(MirrorGroup.class);
    group = (MirrorGroup) context.bean();

    String groupName = group.getGroupName();
    this.grpName = groupName;

    membersConfig = new MembersConfigObject(createContext(setupManager));
  }

  public void setGroupId(GroupID groupId) {
    this.groupId = groupId;
  }

  @Override
  public int getElectionTimeInSecs() {
    if (group.isSetElectionTime()) { return group.getElectionTime(); }
    return DEFAULT_ELECETION_TIME;
  }

  public void setGroupName(String groupName) {
    this.grpName = groupName;
  }

  @Override
  public String getGroupName() {
    return grpName;
  }

  @Override
  public MembersConfig getMembers() {
    return this.membersConfig;
  }

  @Override
  public GroupID getGroupId() {
    return this.groupId;
  }

  private final ConfigContext createContext(L2ConfigurationSetupManagerImpl setupManager) {
    ChildBeanRepository beanRepository = new ChildBeanRepository(setupManager.serversBeanRepository(), Members.class,
                                                                 new ChildBeanFetcher() {
                                                                   @Override
                                                                   public XmlObject getChild(XmlObject parent) {
                                                                     return group.getMembers();
                                                                   }
                                                                 });
    return setupManager.createContext(beanRepository, setupManager.getConfigFilePath());
  }

  @Override
  public boolean isMember(String l2Name) {
    String[] members = getMembers().getMemberArray();
    for (String member : members) {
      if (member.equals(l2Name)) { return true; }
    }
    return false;
  }

  public static void createDefaultMirrorGroup(Servers servers) throws ConfigurationSetupException {
    Assert.assertTrue(servers.isSetMirrorGroups());
    Assert.assertEquals(0, servers.getMirrorGroups().getMirrorGroupArray().length);

    MirrorGroup mirrorGroup = servers.getMirrorGroups().addNewMirrorGroup();
    mirrorGroup.setElectionTime(DEFAULT_ELECETION_TIME);
    Members members = mirrorGroup.addNewMembers();

    Server[] serverArray = servers.getServerArray();

    for (int i = 0; i < serverArray.length; i++) {
      // name for each server should exist
      String name = serverArray[i].getName();
      if (name == null || name.equals("")) { throw new ConfigurationSetupException(
                                                                                   "server's name not defined... name=["
                                                                                       + name + "] serverTsaPort=["
                                                                                       + serverArray[i].getTsaPort()
                                                                                       + "]"); }
      members.insertMember(i, serverArray[i].getName());
    }
  }

}
