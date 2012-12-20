/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManagerImpl;
import com.tc.net.GroupID;
import com.tc.util.Assert;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

import java.util.LinkedHashSet;
import java.util.Set;

public class ActiveServerGroupConfigObject extends BaseConfigObject implements ActiveServerGroupConfig {

  // TODO: the defaultValueProvider is not implemented to fetch default values
  // for attributes... possibly fix this and
  // use the commented code to set defaultId:
  // int defaultId = ((XmlInteger)
  // defaultValueProvider.defaultFor(serversBeanRepository.rootBeanSchemaType(),
  // "active-server-groups/active-server-group[@id]")).getBigIntegerValue().intValue();
  public static final int     defaultGroupId         = 0;

  private static final int    DEFAULT_ELECETION_TIME = 5;

  private static final String DEFAULT_GROUP_NAME     = "default-group";

  private GroupID             groupId;
  private String              grpName;
  private final Set<String>   members;
  private final MirrorGroup   group;

  public ActiveServerGroupConfigObject(ConfigContext context, L2ConfigurationSetupManagerImpl setupManager)
      throws ConfigurationSetupException {
    super(context);
    context.ensureRepositoryProvides(MirrorGroup.class);
    group = (MirrorGroup) context.bean();

    String groupName = group.getGroupName();
    this.grpName = groupName;

    members = new LinkedHashSet<String>(group.sizeOfServerArray());
    for (Server server : group.getServerArray()) {
      boolean added = members.add(server.getName());
      if (!added) { throw new ConfigurationSetupException("Duplicate server name [" + server.getName() + "] in group "
                                                          + groupName); }
    }
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
  public String[] getMembers() {
    return this.members.toArray(new String[members.size()]);
  }

  @Override
  public GroupID getGroupId() {
    return this.groupId;
  }

  @Override
  public boolean isMember(String l2Name) {
    return members.contains(l2Name);
  }

  public static void createDefaultMirrorGroup(Servers servers) {
    Assert.assertEquals(0, servers.getMirrorGroupArray().length);

    MirrorGroup mirrorGroup = servers.addNewMirrorGroup();
    mirrorGroup.setElectionTime(DEFAULT_ELECETION_TIME);
    mirrorGroup.setGroupName(DEFAULT_GROUP_NAME);

    Server[] serverArray = servers.getServerArray();
    for (int i = 0; i < serverArray.length; i++) {
      Server server = (Server) serverArray[i].copy();
      serverArray[i] = server;
    }

    servers.setServerArray(null);
    mirrorGroup.setServerArray(serverArray);
  }

}
