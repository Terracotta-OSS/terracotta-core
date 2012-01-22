/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * TODO Jan 13, 2005: comment describing what this class is for.
 */
interface HandshakeMessageResponder {
  public void handleHandshakeMessage(TransportHandshakeMessage message);
}
