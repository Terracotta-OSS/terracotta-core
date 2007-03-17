/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.util.Assert;

public class RouteGroupMessagesToSink implements GroupMessageListener {

  private final String name;
  private final Sink sink;

  public RouteGroupMessagesToSink(String name, Sink sink) {
    this.name = name;
    this.sink = sink;
  }

  public void messageReceived(NodeID fromNode, GroupMessage msg) {
    Assert.assertTrue(this.toString(), msg instanceof EventContext);
    sink.add((EventContext) msg);
  }
  
  public String toString() {
    return "MessageRouter [ " + name + " ] - > " + sink;
  }

}
