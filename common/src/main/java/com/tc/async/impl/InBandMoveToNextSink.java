/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.async.impl;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.net.NodeID;

public class InBandMoveToNextSink implements SpecializedEventContext {

  private final EventContext event;
  private final Sink         sink;
  private final NodeID       nodeID;

  public InBandMoveToNextSink(EventContext event, Sink sink, NodeID nodeID) {
    this.event = event;
    this.sink = sink;
    this.nodeID = nodeID;
  }

  @Override
  public void execute() {
    sink.add(event);
  }

  @Override
  public Object getKey() {
    return nodeID;
  }

}
