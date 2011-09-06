/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * TODO Jan 13, 2005: comment describing what this class is for.
 */
interface HandshakeMessageResponder {
  public void handleHandshakeMessage(TransportHandshakeMessage message);
}