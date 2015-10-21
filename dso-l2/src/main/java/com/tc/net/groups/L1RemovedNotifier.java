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
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.HaConfig;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;

/*
 * Detect client disconnection from one mirror-group then notify other groups. Others will disconnect that client.
 */
public class L1RemovedNotifier implements ConnectionIDFactoryListener {
  private static final TCLogger logger = TCLogging.getLogger(L1RemovedNotifier.class);
  private final boolean         isActiveCoordinator;
  private final GroupID         activeCoordinatorGroupID;
  private final GroupID         thisGroupID;
  private final GroupManager    groupManager;
  private final ChannelManager  channelManager;
  private final Sink<L1RemovedGroupMessage> sink;

  public L1RemovedNotifier(HaConfig haConfig, GroupManager groupManager, ChannelManager channelManager, Sink<L1RemovedGroupMessage> sink) {
    this.isActiveCoordinator = haConfig.isActiveCoordinatorGroup();
    this.activeCoordinatorGroupID = haConfig.getActiveCoordinatorGroupID();
    this.thisGroupID = haConfig.getThisGroupID();
    this.groupManager = groupManager;
    this.channelManager = channelManager;
    this.sink = sink;

    groupManager.registerForMessages(L1RemovedGroupMessage.class, new L1RemovedNotificationRouter());
  }

  /*
   * broadcast to all active if this is active-coordinator, otherwise send to active-coordinator only.
   */
  @Override
  public void connectionIDDestroyed(ConnectionID connectionID) {
    L1RemovedGroupMessage msg = new L1RemovedGroupMessage(new ClientID(connectionID.getChannelID()), thisGroupID);
    if (isActiveCoordinator) {
      // send to all cluster
      logger.warn("Notify cluster ConnectionID destroyed " + connectionID);
      groupManager.sendAll(msg);
    } else {
      // only send to active-coordinator
      logger.warn("Notify ConnectionID destroyed " + connectionID + " to " + activeCoordinatorGroupID);
      try {
        groupManager.sendTo(activeCoordinatorGroupID, msg);
      } catch (GroupException e) {
        logger.error("Failed to send " + msg + " to " + activeCoordinatorGroupID + " " + e);
      }
    }
  }

  @Override
  public void connectionIDCreated(ConnectionID connectionID) {
    // ignore
  }

  /*
   * L1-removed-message receiver
   */
  private final class L1RemovedNotificationRouter implements GroupMessageListener<L1RemovedGroupMessage> {

    @Override
    public void messageReceived(NodeID fromNode, L1RemovedGroupMessage l1RemovedGroupMessage) {
      ClientID cid = l1RemovedGroupMessage.getClientID();
      MessageChannel channel = channelManager.getChannel(cid.getChannelID());
      // This receives event from receive_group_message_stage. The "channel.close" does sendAllAndWaitForResponse to
      // remove ConnectionID from stripe but it also expects response from same thread that causes deadlock. Use a stage
      // to do closing instead.
      if (channel != null) {
        sink.addSingleThreaded(l1RemovedGroupMessage);
      }
    }
  }

}
