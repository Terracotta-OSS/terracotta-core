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
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.net.ClientID;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.cluster.ClusterInternalEventsGun;

public class ClusterMembershipEventsHandler extends AbstractEventHandler<ClusterMembershipMessage> {

  private final ClusterInternalEventsGun clusterEventsGun;

  public ClusterMembershipEventsHandler(ClusterInternalEventsGun ClusterEventsGun) {
    this.clusterEventsGun = ClusterEventsGun;
  }

  @Override
  public void handleEvent(ClusterMembershipMessage context) throws EventHandlerException {
    handleClusterMembershipMessage(context);
  }

  private void handleClusterMembershipMessage(ClusterMembershipMessage cmm) throws EventHandlerException {
    if (cmm.getProductId().isInternal()) {
      // don't fire events for internal products.
      return;
    }
    if (cmm.isNodeConnectedEvent()) {
      clusterEventsGun.fireNodeJoined((ClientID)cmm.getNodeId());
    } else if (cmm.isNodeDisconnectedEvent()) {
      clusterEventsGun.fireNodeLeft((ClientID)cmm.getNodeId());
    } else {
      throw new EventHandlerException("Unknown event type: " + cmm);
    }
  }

}
