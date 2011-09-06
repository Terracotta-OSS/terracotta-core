/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
