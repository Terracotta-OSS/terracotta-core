/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;

/**
 * Interface for TC message routers
 * 
 * @author steve
 */
public interface TCMessageRouter extends TCMessageSink {

  public void routeMessageType(TCMessageType protocol, TCMessageSink sink);

  public void routeMessageType(TCMessageType protocol, Sink destSink, Sink hydrateSink);

  public void unrouteMessageType(TCMessageType protocol);

}
