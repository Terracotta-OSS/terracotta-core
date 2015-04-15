/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.NodesStore;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.text.PrettyPrintable;

import java.util.Set;

public interface GroupManager extends PrettyPrintable {

  public NodeID join(final Node thisNode, final NodesStore nodesStore) throws GroupException;

  public NodeID getLocalNodeID();

  public void sendAll(GroupMessage msg);

  public void sendAll(GroupMessage msg, Set nodeIDs);

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException;

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg, Set nodeIDs) throws GroupException;

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException;

  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException;

  public void registerForMessages(Class msgClass, GroupMessageListener listener);

  public void routeMessages(Class msgClass, Sink sink);

  public void registerForGroupEvents(GroupEventsListener listener);

  public void zapNode(NodeID nodeID, int type, String reason);

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor);

  public boolean isNodeConnected(NodeID sid);

  public boolean isServerConnected(String nodeName);

  public void closeMember(ServerID serverID);
}
