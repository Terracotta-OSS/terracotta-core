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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tc.cluster.ClusterInternalEventsGun;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.util.ProductID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class ClusterMembershipEventsHandlerTest {
  private ClusterMembershipEventsHandler handler;
  private ClusterInternalEventsGun clusterEventsGun;
  private ClientID clientID;

  @BeforeEach
  public void setUp() throws Exception {
    clientID = new ClientID(1);
    clusterEventsGun = mock(ClusterInternalEventsGun.class);
    handler = new ClusterMembershipEventsHandler(clusterEventsGun);
  }

  @Test
  public void testInternalNodeJoined() throws Exception {
    handler.handleEvent(joinedMsg(clientID, ProductID.DIAGNOSTIC));
    verify(clusterEventsGun, never()).fireNodeJoined(clientID);
  }

  @Test
  public void testInternalNodeLeft() throws Exception {
    handler.handleEvent(leftMsg(clientID, ProductID.DIAGNOSTIC));
    verify(clusterEventsGun, never()).fireNodeLeft(clientID);
  }

  @Test
  public void testUserNodeJoined() throws Exception {
    handler.handleEvent(joinedMsg(clientID, ProductID.STRIPE));
    verify(clusterEventsGun).fireNodeJoined(clientID);
  }

  @Test
  public void testUserNodeLeft() throws Exception {
    handler.handleEvent(leftMsg(clientID, ProductID.STRIPE));
    verify(clusterEventsGun).fireNodeLeft(clientID);
  }

  private ClusterMembershipMessage joinedMsg(NodeID clientId, ProductID productID) {
    return msg(ClusterMembershipMessage.EventType.NODE_CONNECTED, clientId, productID);
  }

  private ClusterMembershipMessage leftMsg(NodeID clientId, ProductID productID) {
    return msg(ClusterMembershipMessage.EventType.NODE_DISCONNECTED, clientId, productID);
  }

  private ClusterMembershipMessage msg(int type, NodeID nodeID, ProductID productID) {
    ClusterMembershipMessage membershipMessage = mock(ClusterMembershipMessage.class);
    when(membershipMessage.getProductId()).thenReturn(productID);
    when(membershipMessage.getEventType()).thenReturn(type);
    when(membershipMessage.getNodeId()).thenReturn(nodeID);
    when(membershipMessage.isNodeConnectedEvent()).thenReturn(ClusterMembershipMessage.EventType.isNodeConnected(type));
    when(membershipMessage.isNodeDisconnectedEvent()).thenReturn(ClusterMembershipMessage.EventType.isNodeDisconnected(type));
    return membershipMessage;
  }
}
