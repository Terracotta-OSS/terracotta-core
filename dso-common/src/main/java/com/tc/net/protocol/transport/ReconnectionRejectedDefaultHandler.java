/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.ReconnectionRejectedException;

/**
 * TODO: REMOVE THIS CLASS
 */
public class ReconnectionRejectedDefaultHandler implements ReconnectionRejectedHandler {

  @Override
  public void reconnectionRejected(ReconnectionRejectedCleanupAction cleanup) throws ReconnectionRejectedException {
    throw new ReconnectionRejectedException("Reconnection rejected due to stack not found. Default Behaviour.");
  }

}
