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
package com.tc.async.impl;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.net.NodeID;
import com.tc.util.Assert;

public class InBandMoveToNextSink<EC> implements SpecializedEventContext {
  private final EC event;
  private final SpecializedEventContext specialized;
  private final Sink<EC>         sink;
  private final NodeID       nodeID;
  private final boolean     flush;

  public InBandMoveToNextSink(EC event, SpecializedEventContext specialized, Sink<EC> sink, NodeID nodeID, boolean flush) {
    // We can only wrap one of the events.
    int countOfEvents = 0;
    if (null != event) {
      Assert.assertTrue(event instanceof MultiThreadedEventContext);
      countOfEvents += 1;
    }
    if (null != specialized) {
      countOfEvents += 1;
    }
    Assert.assertEquals(1, countOfEvents);
    
    this.event = event;
    this.specialized = specialized;
    this.sink = sink;
    this.nodeID = nodeID;
    this.flush = flush;
  }

  @Override
  public void execute() {
    if (null != this.specialized) {
      sink.addSpecialized(this.specialized);
    } else {
      sink.addMultiThreaded(this.event);
    }
  }

  @Override
  public Object getSchedulingKey() {
    return nodeID;
  }

  @Override
  public boolean flush() {
    return flush;
  }
}
