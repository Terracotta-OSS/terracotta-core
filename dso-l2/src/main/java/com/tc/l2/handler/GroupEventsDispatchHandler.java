/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupEventsListener;

import java.util.ArrayList;
import java.util.List;

public class GroupEventsDispatchHandler extends AbstractEventHandler<GroupEvent> {

  private final List<GroupEventsListener> listeners = new ArrayList<>();

  public void addListener(GroupEventsListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public void handleEvent(GroupEvent e) {
    if (e.nodeJoined()) {
      for (GroupEventsListener listener : listeners) {
        listener.nodeJoined(e.getNodeID());
      }
    } else {
      for (GroupEventsListener listener : listeners) {
        listener.nodeLeft(e.getNodeID());
      }
    }
  }

  public static final class GroupEventsDispatcher implements GroupEventsListener {
    private final Sink<GroupEvent> sink;

    public GroupEventsDispatcher(Sink<GroupEvent> sink) {
      this.sink = sink;
    }

    @Override
    public void nodeJoined(NodeID nodeID) {
      sink.addSingleThreaded(new GroupEvent(nodeID, true));
    }

    @Override
    public void nodeLeft(NodeID nodeID) {
      sink.addSingleThreaded(new GroupEvent(nodeID, false));
    }
  }
}
