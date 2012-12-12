/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * Client's behaviour when Server rejects its reconnection.
 */
public interface ReconnectionRejectedHandler {

  boolean isRetryOnReconnectionRejected();
}
