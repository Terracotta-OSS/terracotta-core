/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.terracotta.config.Servers;

import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.repository.BeanRepository;
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
