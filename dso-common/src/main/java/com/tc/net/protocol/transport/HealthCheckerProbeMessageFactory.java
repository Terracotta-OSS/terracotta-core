/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;

/**
 * Probe Messages to monitor peer nodes health
 * 
 * @author Manoj
 */
public interface HealthCheckerProbeMessageFactory {
  public HealthCheckerProbeMessage createPing(ConnectionID connectionId, TCConnection source);

  public HealthCheckerProbeMessage createPingReply(ConnectionID connectionId, TCConnection source);

}
