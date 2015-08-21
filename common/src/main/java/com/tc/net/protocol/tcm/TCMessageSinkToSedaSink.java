/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;

class TCMessageSinkToSedaSink implements TCMessageSink {
  private final Sink<TCMessage> destSink;
  private final Sink<HydrateContext> hydrateSink;
  

  public TCMessageSinkToSedaSink(Sink<TCMessage> destSink, Sink<HydrateContext> hydrateSink) {
    this.destSink = destSink;
    this.hydrateSink = hydrateSink;
  }

  @Override
  public void putMessage(TCMessage message) {    
    HydrateContext context = new HydrateContext(message, destSink);
    hydrateSink.addMultiThreaded(context);
  }
  
    
  
}
