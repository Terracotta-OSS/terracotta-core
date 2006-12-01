/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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