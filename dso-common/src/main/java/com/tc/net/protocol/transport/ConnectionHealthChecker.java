/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
