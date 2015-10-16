/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;

public class HydrateContext implements MultiThreadedEventContext {

  private final Sink<TCMessage>      destSink;
  private final TCMessage message;

  public HydrateContext(TCMessage message, Sink<TCMessage> destSink) {
    this.message = message;
    this.destSink = destSink;
  }

  public Sink<TCMessage> getDestSink() {
    return destSink;
  }

  public TCMessage getMessage() {
    return message;
  }

  @Override
  public Object getSchedulingKey() {
    return message.getSourceNodeID();
  }
  
  @Override
  public boolean flush() {
//  hydrate operations are independent and don't need a flush
    return false;
  }
}
