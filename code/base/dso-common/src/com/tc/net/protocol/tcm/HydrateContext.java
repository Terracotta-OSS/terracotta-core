/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;

public class HydrateContext implements EventContext {

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
}
