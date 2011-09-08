/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.ReconnectionRejectedException;

/**
 * Client's behaviour when Server rejects its reconnection.
 */
public interface ReconnectionRejectedHandler {

  public static final ReconnectionRejectedHandler DEFAULT_BEHAVIOUR = new ReconnectionRejectedDefaultHandler();

  interface ReconnectionRejectedCleanupAction {
    void reconnectionRejectedCleanupAction();
  }

  public void reconnectionRejected(ReconnectionRejectedCleanupAction cleanup) throws ReconnectionRejectedException;

}
