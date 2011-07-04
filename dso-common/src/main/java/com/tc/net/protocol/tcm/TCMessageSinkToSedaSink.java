/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;

class TCMessageSinkToSedaSink implements TCMessageSink {
  private final Sink destSink;
  private final Sink hydrateSink;
  

  public TCMessageSinkToSedaSink(Sink destSink, Sink hydrateSink) {
    this.destSink = destSink;
    this.hydrateSink = hydrateSink;
  }

  public void putMessage(TCMessage message) {    
    HydrateContext context = new HydrateContext(message, destSink);
    hydrateSink.add(context);
  }
  
    
  
}
