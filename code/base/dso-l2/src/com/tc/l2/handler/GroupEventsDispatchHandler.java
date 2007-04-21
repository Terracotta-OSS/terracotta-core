/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.NodeID;

public class GroupEventsDispatchHandler extends AbstractEventHandler {

  private final GroupEventsListener target;

  public GroupEventsDispatchHandler(GroupEventsListener target) {
    this.target = target;
  }

  public void handleEvent(EventContext context) {
    GroupEvent e = (GroupEvent) context;
    if (e.nodeJoined()) {
      target.nodeJoined(e.getNodeID());
    } else {
      target.nodeLeft(e.getNodeID());
    }
  }

  public static final class GroupEventsDispatcher implements GroupEventsListener {
    private final Sink sink;

    public GroupEventsDispatcher(Sink sink) {
      this.sink = sink;
    }

    public void nodeJoined(NodeID nodeID) {
      sink.add(new GroupEvent(nodeID, true));
    }

    public void nodeLeft(NodeID nodeID) {
      sink.add(new GroupEvent(nodeID, false));
    }
  }

  private static final class GroupEvent implements EventContext {

    private final NodeID  nodeID;
    private final boolean joined;

    public GroupEvent(NodeID nodeID, boolean joined) {
      this.nodeID = nodeID;
      this.joined = joined;
    }

    public boolean nodeJoined() {
      return joined;
    }

    public NodeID getNodeID() {
      return nodeID;
    }

  }
}
