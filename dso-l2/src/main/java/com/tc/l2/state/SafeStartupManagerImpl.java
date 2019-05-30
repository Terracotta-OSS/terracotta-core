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
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.logging.TCLogging;
import com.tc.management.TerracottaManagement;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupEventsListener;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import static com.tc.l2.state.ConsistencyMBean.CONSISTENCY_BEAN_NAME;
import com.tc.util.concurrent.SetOnceFlag;

public class SafeStartupManagerImpl implements ConsistencyManager, GroupEventsListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(SafeStartupManagerImpl.class);
  private static final Logger CONSOLE = TCLogging.getConsoleLogger();

  private final boolean consistentStartup;
  private boolean allowTransition = false;
  private boolean suspended = false;

  private final int peerServers;
  private final ConsistencyManager consistencyManager;
  private final Set<NodeID> activePeers = new HashSet<>();
  private final SetOnceFlag disable = new SetOnceFlag();

  public SafeStartupManagerImpl(boolean consistentStartup, int peerServers, ConsistencyManager consistencyManager) {
    this.consistentStartup = consistentStartup;
    this.peerServers = peerServers;
    this.consistencyManager = consistencyManager;
    initMBean();
  }

  private void initMBean() {
    try {
      ObjectName mbeanName = TerracottaManagement.createObjectName(null, CONSISTENCY_BEAN_NAME, TerracottaManagement.MBeanDomain.PUBLIC);
      ManagementFactory.getPlatformMBeanServer().registerMBean(new ConsistencyMBeanImpl(this), mbeanName);
    } catch (Exception e) {
      LOGGER.warn("SafeMode MBean not initialized", e);
    }
  }

  @Override
  public synchronized boolean requestTransition(ServerMode mode, NodeID sourceNode, Transition newMode) throws IllegalStateException {
    if (newMode == Transition.CONNECT_TO_ACTIVE) {
      // disable this mode since we have already tried to connect to an existing active.
      // safe startup mode no longer applies
      disable.attemptSet();
    } else if (!disable.isSet() && consistentStartup && mode == ServerMode.START && newMode == Transition.MOVE_TO_ACTIVE) {
      if (activePeers.size() == peerServers) {
        CONSOLE.info("Action:{} allowed because all servers are connected", newMode);
        suspended = false;
      } else if (allowTransition) {
        CONSOLE.info("Action:{} allowed with external intervention", newMode);
        suspended = false;
      } else {
        CONSOLE.info("Action:{} not allowed because not enough servers are connected", newMode);
        suspended = true;
      }
      return !suspended;
    }
    return consistencyManager.requestTransition(mode, sourceNode, newMode);
  }

  @Override
  public synchronized boolean lastTransitionSuspended() {
    if (suspended) {
      return true;
    }
    return consistencyManager.lastTransitionSuspended();
  }

  @Override
  public synchronized void allowLastTransition() {
    if (suspended) {
      LOGGER.info("External intervention to allow the last requested transition");
      this.allowTransition = true;
    } else {
      this.consistencyManager.allowLastTransition();
    }
  }

  @Override
  public Collection<Transition> requestedActions() {
    return consistencyManager.requestedActions();
  }

  @Override
  public void nodeJoined(NodeID nodeID) {
    activePeers.add(nodeID);
    if (consistencyManager instanceof GroupEventsListener) {
      ((GroupEventsListener) consistencyManager).nodeJoined(nodeID);
    }
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    activePeers.remove(nodeID);
    if (consistencyManager instanceof GroupEventsListener) {
      ((GroupEventsListener) consistencyManager).nodeLeft(nodeID);
    }
  }

  @Override
  public long getCurrentTerm() {
    return consistencyManager.getCurrentTerm();
  }

  @Override
  public Enrollment createVerificationEnrollment(NodeID lastActive, WeightGeneratorFactory weightFactory) {
    return consistencyManager.createVerificationEnrollment(lastActive, weightFactory);
  }
}
