/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;

public class InBandMoveToNextSink implements SpecializedEventContext {

  private final EventContext event;
  private final Sink         sink;

  public InBandMoveToNextSink(EventContext event, Sink sink) {
    this.event = event;
    this.sink = sink;
  }

  public void execute() {
    sink.add(event);
  }

}
