/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.net.protocol.transport;

/**
 * The main interface for Connection HealthChecker. When CommunicationsManager are built, a HealthChecker is tied to it
 * to monitor all the connections it establishes. By default, an ECHO health checker is tied to all the communications
 * manager to respond to the peer probe signals if it ever receive any.
 * 
 * @author Manoj
 */
public interface ConnectionHealthChecker extends MessageTransportListener {

  public void start();

  public void stop();
  
}