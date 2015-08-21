/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.config.NodesStore;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.text.PrettyPrintable;

import java.util.Set;

public interface GroupManager<M extends GroupMessage> extends PrettyPrintable {

  public NodeID join(Node thisNode, NodesStore nodesStore) throws GroupException;

  public NodeID getLocalNodeID();

  public void sendAll(M msg);

  public void sendAll(M msg, Set<? extends NodeID> nodeIDs);

  public GroupResponse<M> sendAllAndWaitForResponse(M msg) throws GroupException;

  public GroupResponse<M> sendAllAndWaitForResponse(M msg, Set<? extends NodeID> nodeIDs) throws GroupException;

  public void sendTo(NodeID node, M msg) throws GroupException;

  public M sendToAndWaitForResponse(NodeID nodeID, M msg) throws GroupException;

  public <N extends M> void registerForMessages(Class<N> msgClass, GroupMessageListener<N> listener);

  public <N extends M> void routeMessages(Class<N> msgClass, Sink<N> sink);

  public void registerForGroupEvents(GroupEventsListener listener);

  public void zapNode(NodeID nodeID, int type, String reason);

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor);

  public boolean isNodeConnected(NodeID sid);

  public boolean isServerConnected(String nodeName);

  public void closeMember(ServerID serverID);
}
