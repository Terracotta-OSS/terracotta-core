/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;

public interface GroupManager {

  public NodeID join(final Node thisNode, final Node[] allNodes) throws GroupException;
  
  public NodeID getLocalNodeID() throws GroupException;

  public void sendAll(GroupMessage msg) throws GroupException;

  public GroupResponse sendAllAndWaitForResponse(GroupMessage msg) throws GroupException;

  public void sendTo(NodeID node, GroupMessage msg) throws GroupException;
  
  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException;
  
  public void registerForMessages(Class msgClass, GroupMessageListener listener);

  public void routeMessages(Class msgClass, Sink sink);

  public void registerForGroupEvents(GroupEventsListener listener);

  public void zapNode(NodeID nodeID);

}
