/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

public interface GroupEventsListener {

  public void nodeJoined(NodeID nodeID);

  public void nodeLeft(NodeID nodeID);
  
}
