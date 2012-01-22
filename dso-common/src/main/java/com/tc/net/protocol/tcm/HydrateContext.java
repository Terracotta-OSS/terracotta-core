/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;

public class HydrateContext implements MultiThreadedEventContext {

  private final Sink      destSink;
  private final TCMessage message;

  public HydrateContext(TCMessage message, Sink destSink) {
    this.message = message;
    this.destSink = destSink;
  }

  public Sink getDestSink() {
    return destSink;
  }

  public TCMessage getMessage() {
    return message;
  }

  public Object getKey() {
    return message.getSourceNodeID();
  }
}
