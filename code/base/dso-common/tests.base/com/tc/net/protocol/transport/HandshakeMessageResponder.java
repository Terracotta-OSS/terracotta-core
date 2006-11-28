/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * TODO Jan 13, 2005: comment describing what this class is for.
 */
interface HandshakeMessageResponder {
  public void handleHandshakeMessage(TransportHandshakeMessage message);
}