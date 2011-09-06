/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;

public interface TCGroupMemberDiscovery extends GroupEventsListener {

  public void start() throws GroupException;
  
  public void addNode(Node node);

  public void stop(long timeout);

  public void setupNodes(Node local, Node[] nodes);
  
  public Node getLocalNode();
  
  public void discoveryHandler(EventContext context);
  
  public boolean isValidClusterNode(NodeID nodeID);

  public void removeNode(Node node);

  public boolean isServerConnected(String nodeName);
  
}
