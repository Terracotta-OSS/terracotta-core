/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;

/**
 * Probe Messages to monitor peer nodes health
 *
 * @author Manoj
 */
public interface HealthCheckerProbeMessageFactory {

  HealthCheckerProbeMessage createPing(ConnectionID connectionId, TCConnection source);

  HealthCheckerProbeMessage createPingReply(ConnectionID connectionId, TCConnection source);

  HealthCheckerProbeMessage createTimeCheck(ConnectionID connectionId, TCConnection source);
}
