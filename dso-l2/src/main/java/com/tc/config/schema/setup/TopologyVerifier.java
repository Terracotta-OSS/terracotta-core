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
package com.tc.config.schema.setup;

import org.terracotta.config.Servers;

import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.server.ServerConnectionValidator;

import java.util.Set;

//TODO fix this when a proper topology is present
public class TopologyVerifier {
  private final Servers                   oldServersBean;
  private final Servers                   newServersBean;
  private final ActiveServerGroupsConfig  oldGroupsInfo;
  private final ServerConnectionValidator serverConnectionValidator;

  private static final TCLogger           logger = TCLogging.getLogger(TopologyVerifier.class);

  TopologyVerifier(Servers oldServers, Servers newServers,
                   ActiveServerGroupsConfig oldGroupsInfo, ServerConnectionValidator serverConnectionValidator) {
    this.oldServersBean = oldServers;
    this.newServersBean = newServers;
    this.oldGroupsInfo = oldGroupsInfo;
    this.serverConnectionValidator = serverConnectionValidator;
  }

  /**
   *
   */
  public TopologyReloadStatus checkAndValidateConfig() {
    // first check if all existing servers config is not changed
    // check ports, dgc info, persistent info
    TopologyReloadStatus topologyStatus = checkExistingServerConfigIsSame();
    if (topologyStatus != TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE) { return topologyStatus; }

    // check if group names consist of the same members as the older ones
    topologyStatus = checkGroupInfo();
    if (topologyStatus != TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE) { return topologyStatus; }

    // Check if removed members are still alive
    return checkIfServersAlive();
  }

  private TopologyReloadStatus checkIfServersAlive() {
    Set<String> membersRemoved = getRemovedMembers();
    for (String member : membersRemoved) {
      if (serverConnectionValidator.isAlive(member)) {
        logger.warn("Reloading servers config failed as " + member + " is still alive.");
        return TopologyReloadStatus.SERVER_STILL_ALIVE;
      }
    }

    return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE;
  }

  private TopologyReloadStatus checkGroupInfo() {
    if (isGroupsSizeEqualsOne()) { return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE; }

    if (!isGroupNameSpecified()) { return TopologyReloadStatus.SPECIFY_MIRROR_GROUPS; }

    if (!isGroupNameSame()) { return TopologyReloadStatus.TOPOLOGY_CHANGE_UNACCEPTABLE; }

    if (isMemberMovedToDifferentGroup()) { return TopologyReloadStatus.TOPOLOGY_CHANGE_UNACCEPTABLE; }

    return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE;
  }

  private boolean isGroupsSizeEqualsOne() {
    return true;
  }

  private boolean isGroupNameSpecified() {
    return false;
  }

  private boolean isMemberMovedToDifferentGroup() {
    return false;
  }

  private boolean isGroupNameSame() {

    return false;
  }

  private TopologyReloadStatus checkExistingServerConfigIsSame() {
    return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE;
  }

  private Set<String> getRemovedMembers() {
    return null;
  }

}
