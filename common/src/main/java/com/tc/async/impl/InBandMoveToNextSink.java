/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  public InBandMoveToNextSink(EC event, SpecializedEventContext specialized, Sink<EC> sink, NodeID nodeID) {
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
//  in-band always needs to flush all threads
    return true;
  }
}
