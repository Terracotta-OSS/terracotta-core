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
package com.tc.l2.ha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.state.ServerMode;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.GroupResponse;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.utils.L2Utils;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.concurrent.SetOnceFlag;
import org.terracotta.configuration.ConfigurationProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ReplicatedClusterStateManagerImpl implements ReplicatedClusterStateManager, GroupMessageListener<ClusterStateMessage> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReplicatedClusterStateManagerImpl.class);

  private final GroupManager<AbstractGroupMessage>    groupManager;
  private final ClusterState    state;
  private final ConfigurationProvider configurationProvider;
  private final Supplier<ServerMode>    currentMode;

  private boolean               isActive = false;

  private final Collection<NodeID>    others = new HashSet<>();

  public ReplicatedClusterStateManagerImpl(GroupManager<AbstractGroupMessage> groupManager, Supplier<ServerMode> currentMode,
                                           ClusterState clusterState, ConfigurationProvider configurationProvider) {
    this.groupManager = groupManager;
    this.currentMode = currentMode;
    this.state = clusterState;
    this.configurationProvider = configurationProvider;
    groupManager.registerForMessages(ClusterStateMessage.class, this);
  }

  @Override
  public synchronized void goActiveAndSyncState() {
    switch (currentMode.get()) {
      case ACTIVE:
        state.setCurrentState(currentMode.get().getState());
        state.generateStripeIDIfNeeded();
        state.syncActiveState();

        others.clear();
        // Sync state to external passive servers
        state.setConfigSyncData(configurationProvider.getSyncData());
        others.addAll(publishToAll(ClusterStateMessage.createClusterStateMessage(state)));
        isActive = true;
        break;
      case STOP:
        TCLogging.getConsoleLogger().warn("Failed to activate.  Server is stopping");
        break;
      default:
        throw new AssertionError("cannot activate. State:" + currentMode.get());
    }
    notifyAll();
  }

  @Override
  public synchronized void publishClusterState(NodeID nodeID) throws GroupException {
    waitUntilActive();
    state.setConfigSyncData(configurationProvider.getSyncData());
    ClusterStateMessage msg = (ClusterStateMessage)groupManager
        .sendToAndWaitForResponse(nodeID, ClusterStateMessage.createClusterStateMessage(state));
    validateResponse(nodeID, msg);
  }

  private void waitUntilActive() {
    while (!isActive) {
      LOGGER.info("Waiting since ReplicatedClusterStateManager hasn't gone ACTIVE yet ...");
      try {
        wait(3000);
      } catch (InterruptedException e) {
        L2Utils.handleInterrupted(LOGGER, e);
      }
    }
  }

  private boolean validateResponse(NodeID nodeID, ClusterStateMessage msg) {
    if (msg == null || msg.getType() != ClusterStateMessage.OPERATION_SUCCESS) {
      LOGGER.error("Recd wrong response from : " + nodeID + " : msg = " + msg
                   + " while publishing Cluster State");
      return false;
    } else {
      return true;
    }
  }

  @Override
  public synchronized void connectionIDCreated(ConnectionID connectionID) {
    Assert.assertTrue(isActive);
    state.addNewConnection(connectionID);
    Collection<NodeID> sentTo = publishToAll(ClusterStateMessage.createNewConnectionCreatedMessage(connectionID));
    LOGGER.debug("applied to " + sentTo);
  }

  @Override
  public synchronized void connectionIDDestroyed(ConnectionID connectionID) {
    Assert.assertTrue(isActive);
    state.removeConnection(connectionID);
    Collection<NodeID> sentTo = publishToAll(ClusterStateMessage.createConnectionDestroyedMessage(connectionID));
    LOGGER.debug("applied to " + sentTo);
  }

  private Collection<NodeID> publishToAll(AbstractGroupMessage message) {
    try {
      GroupResponse<AbstractGroupMessage> gr = groupManager.sendAllAndWaitForResponse(message);
      HashSet<NodeID> success = new HashSet<>();
      for (AbstractGroupMessage resp : gr.getResponses()) {
        ClusterStateMessage msg = (ClusterStateMessage) resp;
        if (validateResponse(msg.messageFrom(), msg)) {
          success.add(msg.messageFrom());
        } else {
          LOGGER.info("message not validated {} by {} result {}", message, msg.messageFrom(), msg.getType());
        }
      }
      return success;
    } catch (GroupException e) {
      // TODO:: Is this extreme ?
      throw new AssertionError(e);
    }
  }

  @Override
  public void messageReceived(NodeID fromNode, ClusterStateMessage msg) {
    handleClusterStateMessage(fromNode, msg);
  }

  private void handleClusterStateMessage(NodeID fromNode, ClusterStateMessage msg) {
    if (isActive) {
      LOGGER.warn("Recd ClusterStateMessage from " + fromNode
                  + " while I am the cluster co-ordinator. This is bad. Sending NG response. ");
      sendNGSplitBrainResponse(fromNode, msg);
    } else {
      // XXX:: Is it a good idea to check if the message we are receiving is from the active server that we think is
      // active ? There is a race between publishing active and pushing cluster state and hence we don't do the check.
      // May be someday these two messages will merge into one.
      if (msg.isSplitBrainMessage()) {
        return; // About to get zapped no need to actually do anything with the split brain message.
      }
      if (ServerMode.PASSIVE_STATES.contains(this.currentMode.get())) {
        msg.initState(state);
        if (msg.getType() == ClusterStateMessage.COMPLETE_STATE) {
          configurationProvider.sync(state.getConfigSyncData());
        }
        state.syncSequenceState();
        sendOKResponse(fromNode, msg);
      } else {
        sendNGSplitBrainResponse(fromNode, msg);
      }
    }
  }

  private void sendOKResponse(NodeID fromNode, ClusterStateMessage msg) {
    try {
      groupManager.sendTo(fromNode, ClusterStateMessage.createOKResponse(msg));
    } catch (GroupException e) {
      LOGGER.error("Error handling message : " + msg, e);
    }
  }

  private void sendNGSplitBrainResponse(NodeID fromNode, ClusterStateMessage msg) {
    try {
      groupManager.sendTo(fromNode, ClusterStateMessage.createNGSplitBrainResponse(msg));
    } catch (GroupException e) {
      LOGGER.error("Error handling message : " + msg, e);
    }
  }

  @Override
  public synchronized void setCurrentState(State currentState) {
    this.state.setCurrentState(currentState);
  }

  @Override
  public void reportStateToMap(Map<String, Object> state) {
    state.put("className", this.getClass().getName());
    Map<String, Object> cstate = new LinkedHashMap<>();
    state.put("state", cstate);
    this.state.reportStateToMap(cstate);
    state.put("currentMode", this.currentMode.get().toString());
  }
}
