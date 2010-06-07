/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
