/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupMessage;

public class RouteGroupMessagesToSink<M extends GroupMessage> implements GroupMessageListener<M> {

  private final String name;
  private final Sink<M> sink;

  public RouteGroupMessagesToSink(String name, Sink<M> sink) {
    this.name = name;
    this.sink = sink;
  }

  @Override
  public void messageReceived(NodeID fromNode, M msg) {
    sink.addSingleThreaded(msg);
  }

  @Override
  public String toString() {
    return "MessageRouter [ " + name + " ] - > " + sink;
  }

}
