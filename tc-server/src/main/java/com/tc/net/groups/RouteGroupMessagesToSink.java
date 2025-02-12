/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.groups;

import com.tc.async.api.Sink;
import com.tc.net.NodeID;

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
