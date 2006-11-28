/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.protocol.transport.ConnectionID;

/**
 * Thrown by Stack providers when a requested network stack cannot be located
 */
public class StackNotFoundException extends Exception {

  public StackNotFoundException(ConnectionID connectionId) {
    super(connectionId + " not found");
  }
}