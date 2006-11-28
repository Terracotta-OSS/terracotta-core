/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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