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
import org.terracotta.config.Server;
import org.terracotta.config.Servers;

import java.util.HashSet;
import java.util.Set;

public class ActiveServerGroupConfigObject implements ActiveServerGroupConfig {

  private final Servers s;

  private String              grpName;
  private final Set<String>   members;

  public ActiveServerGroupConfigObject(Servers s, L2ConfigurationSetupManagerImpl setupManager)
      throws ConfigurationSetupException {
    this.s = s;
    members = new HashSet<>();
    for(Server server : s.getServer()) {
      members.add(server.getName());
    }
  }

  @Override
  public int getElectionTimeInSecs() {
    //TODO fix the election time
    return 5;
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
  public boolean isMember(String l2Name) {
    return members.contains(l2Name);
  }

  public static void createDefaultMirrorGroup(Servers servers) {
    //TODO fix this,
    //DO nothing
  }

  @Override
  public Servers getBean() {
    return s;
  }
}
