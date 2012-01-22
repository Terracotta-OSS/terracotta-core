/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;


public interface HealthCheckerProbeMessage extends WireProtocolMessage {
  
  public boolean isPing();

  public boolean isPingReply();

}
