/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 *
 */
public interface TransportHandshakeErrorHandler {

  void handleHandshakeError(TransportHandshakeErrorContext e);

  void handleHandshakeError(TransportHandshakeErrorContext e, TransportHandshakeMessage m);

}
