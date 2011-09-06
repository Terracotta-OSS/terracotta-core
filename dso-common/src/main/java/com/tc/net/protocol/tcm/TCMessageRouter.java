/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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