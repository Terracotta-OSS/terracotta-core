/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

/**
 * Interface for TC message routers
 * 
 * @author steve
 */
public interface TCMessageRouter extends TCMessageSink {

  public void routeMessageType(TCMessageType protocol, TCMessageSink sink);

  public void unrouteMessageType(TCMessageType protocol);

}