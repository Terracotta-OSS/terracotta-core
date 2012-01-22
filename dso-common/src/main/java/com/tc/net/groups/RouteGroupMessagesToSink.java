/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.util.Assert;

public class RouteGroupMessagesToSink implements GroupMessageListener {

  private final String name;
  private final Sink   sink;

  public RouteGroupMessagesToSink(String name, Sink sink) {
    this.name = name;
    this.sink = sink;
  }

  public void messageReceived(NodeID fromNode, GroupMessage msg) {
    if (!(msg instanceof EventContext)) {
      Assert.failure(this.toString());
    }
    sink.add((EventContext) msg);
  }

  public String toString() {
    return "MessageRouter [ " + name + " ] - > " + sink;
  }

}
