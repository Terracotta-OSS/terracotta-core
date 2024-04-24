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
import com.tc.config.GroupConfiguration;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.core.TCConnectionManager;

import java.util.Set;
import com.tc.text.PrettyPrintable;

public interface GroupManager<M extends GroupMessage> extends PrettyPrintable {

  public NodeID join(GroupConfiguration groupConfiguration) throws GroupException;
  
  public void disconnect();
  
  public ServerID getLocalNodeID();

  public void sendAll(M msg);

  public void sendAll(M msg, Set<? extends NodeID> nodeIDs);

  public GroupResponse<M> sendAllAndWaitForResponse(M msg) throws GroupException;

  public GroupResponse<M> sendAllAndWaitForResponse(M msg, Set<? extends NodeID> nodeIDs) throws GroupException;

  public void sendTo(NodeID node, M msg) throws GroupException;

  public void sendTo(Set<String> nodes, M msg);

  public void sendToWithSentCallback(NodeID node, M msg, Runnable sentCallback) throws GroupException;

  public M sendToAndWaitForResponse(NodeID nodeID, M msg) throws GroupException;

  public GroupResponse<M> sendToAndWaitForResponse(Set<String> nodes, M msg) throws GroupException;

  public <N extends M> void registerForMessages(Class<? extends N> msgClass, GroupMessageListener<N> listener);

  public <N extends M> void routeMessages(Class<? extends N> msgClass, Sink<N> sink);

  public void registerForGroupEvents(GroupEventsListener listener);

  public void unregisterForGroupEvents(GroupEventsListener listener);

  public void zapNode(NodeID nodeID, int type, String reason);

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor);

  public boolean isNodeConnected(NodeID sid);

  public boolean isServerConnected(String nodeName);

  public void closeMember(ServerID serverID);
  
  public void closeMember(String name);
  
  public TCConnectionManager getConnectionManager();

  public void shutdown();

  public boolean isStopped();
  
  int getBufferCount();
}
