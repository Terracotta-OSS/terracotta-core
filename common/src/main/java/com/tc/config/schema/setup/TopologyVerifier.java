/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.server.ServerConnectionValidator;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TopologyVerifier {
  private final Servers                   oldServersBean;
  private final Servers                   newServersBean;
  private final ActiveServerGroupsConfig  oldGroupsInfo;
  private final ServerConnectionValidator serverConnectionValidator;

  private static final TCLogger           logger = TCLogging.getLogger(TopologyVerifier.class);

  TopologyVerifier(MutableBeanRepository oldServers, MutableBeanRepository newServers,
                   ActiveServerGroupsConfig oldGroupsInfo, ServerConnectionValidator serverConnectionValidator) {
    this.oldServersBean = (Servers) oldServers.bean();
    this.newServersBean = (Servers) newServers.bean();
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
    return oldGroupsInfo.getActiveServerGroupCount() == 1 && newServersBean.getMirrorGroupArray().length == 1;
  }

  private boolean isGroupNameSpecified() {
    MirrorGroup[] newGroupsInfo = newServersBean.getMirrorGroupArray();

    // check to see the group names for all new servers are set
    for (MirrorGroup newGroup : newGroupsInfo) {
      if (!newGroup.isSetGroupName()) { return false; }
    }
    return true;
  }

  private boolean isMemberMovedToDifferentGroup() {
    MirrorGroup[] newGroupsInfo = newServersBean.getMirrorGroupArray();
    for (MirrorGroup newGroupInfo : newGroupsInfo) {
      String groupName = newGroupInfo.getGroupName();
      for (String member : L2DSOConfigObject.getServerNames(newGroupInfo)) {
        String previousGrpName = getPreviousGroupName(member);
        if (previousGrpName != null && !groupName.equals(previousGrpName)) {
          logger.warn(member + " group was changed. This is not supported currently.");
          return true;
        }
      }
    }

    return false;
  }

  private String getPreviousGroupName(String member) {
    for (ActiveServerGroupConfig groupInfo : oldGroupsInfo.getActiveServerGroups()) {
      if (groupInfo.isMember(member)) { return groupInfo.getGroupName(); }
    }
    return null;
  }

  private boolean isGroupNameSame() {
    MirrorGroup[] newGroupsInfo = newServersBean.getMirrorGroupArray();

    Set<String> newGroupNames = new HashSet<String>();
    for (MirrorGroup newGroup : newGroupsInfo) {
      newGroupNames.add(newGroup.getGroupName());
    }

    Set<String> oldGroupNames = new HashSet<String>();
    for (ActiveServerGroupConfig oldGroupInfo : this.oldGroupsInfo.getActiveServerGroups()) {
      oldGroupNames.add(oldGroupInfo.getGroupName());
    }

    boolean areGroupNamesSame = oldGroupNames.equals(newGroupNames);
    if (!areGroupNamesSame) {
      logger.warn("The group names have changed. Groups before=" + oldGroupNames + " Groups after=" + newGroupNames);
    }

    return areGroupNamesSame;
  }

  private TopologyReloadStatus checkExistingServerConfigIsSame() {
    Server[] oldServerArray = L2DSOConfigObject.getServers(oldServersBean);
    Map<String, Server> oldServersInfo = new HashMap<String, Server>();
    for (Server server : oldServerArray) {
      oldServersInfo.put(server.getName(), server);
    }

    if (!checkGarbageCollection()) { return TopologyReloadStatus.TOPOLOGY_CHANGE_UNACCEPTABLE; }

    Server[] newServerArray = L2DSOConfigObject.getServers(newServersBean);
    boolean isTopologyChanged = !(newServerArray.length == oldServerArray.length);
    for (Server newServer : newServerArray) {
      Server oldServer = oldServersInfo.get(newServer.getName());
      if (oldServer != null && !checkServer(oldServer, newServer)) { return TopologyReloadStatus.TOPOLOGY_CHANGE_UNACCEPTABLE; }

      if (oldServer == null) {
        isTopologyChanged = true;
      }
    }

    if (!isTopologyChanged) { return TopologyReloadStatus.TOPOLOGY_UNCHANGED; }

    return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE;
  }

  private boolean checkGarbageCollection() {
    if (oldServersBean.isSetGarbageCollection()) {
      if (!newServersBean.isSetGarbageCollection()) { return false; }

      if ((oldServersBean.getGarbageCollection().getEnabled() != newServersBean.getGarbageCollection().getEnabled())
          || oldServersBean.getGarbageCollection().getInterval() != newServersBean.getGarbageCollection().getInterval()) {
        logger.warn("Server Garbage Collection Info changed");
        return false;
      }
    }

    return true;
  }

  private Set<String> getRemovedMembers() {
    Server[] oldServerArray = L2DSOConfigObject.getServers(oldServersBean);
    HashSet<String> oldServerNames = new HashSet<String>();
    for (Server server : oldServerArray) {
      oldServerNames.add(server.getName());
    }

    Server[] newServerArray = L2DSOConfigObject.getServers(newServersBean);
    for (Server newServer : newServerArray) {
      oldServerNames.remove(newServer.getName());
    }

    return oldServerNames;
  }

  /**
   * check ports, persistence and mode
   */
  private boolean checkServer(Server oldServer, Server newServer) {
    if (!validatePorts(oldServer.getTsaPort(), newServer.getTsaPort())
        || !validatePorts(oldServer.getJmxPort(), newServer.getJmxPort())
        || !validatePorts(oldServer.getTsaGroupPort(), newServer.getTsaGroupPort())) {
      logger.warn("Server port configuration was changed for server " + oldServer.getName()
                  + ". [tsa-port, tsa-group-port, jmx-port] [ {" + oldServer.getTsaPort() + "}, {"
                  + oldServer.getTsaGroupPort() + "}, {" + oldServer.getJmxPort() + "}] :"
                  + ". [tsa-port, tsa-group-port, jmx-port] [ {" + oldServer.getTsaPort() + "}, {"
                  + oldServer.getTsaGroupPort() + "}, {" + oldServer.getJmxPort() + "}] to [ {"
                  + newServer.getTsaPort() + "}, {" + newServer.getTsaGroupPort() + "}, {" + newServer.getJmxPort()
                  + "}]");
      return false;
    }

    return true;
  }

  private boolean validatePorts(BindPort oldValue, BindPort newValue) {
    Integer oldPort = oldValue != null ? oldValue.getIntValue() : null;
    Integer newPort = newValue != null ? newValue.getIntValue() : null;

    if ((oldPort == null && newPort == null)) {
      return true;
    } else if (oldPort != null && oldPort.equals(newPort)) {
      // check the bind address
      return validatePortAddress(oldValue.getBind(), newValue.getBind());
    }
    return false;
  }

  private boolean validatePortAddress(String oldValue, String newValue) {
    if (oldValue != null && newValue != null) {
      return oldValue.equals(newValue);
    } else {
      return (oldValue == newValue);
    }
  }
}
