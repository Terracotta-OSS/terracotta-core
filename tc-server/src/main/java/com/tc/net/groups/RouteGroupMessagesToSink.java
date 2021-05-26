/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
    sink.addToSink(msg);
  }

  @Override
  public String toString() {
    return "MessageRouter [ " + name + " ] - > " + sink;
  }

}
