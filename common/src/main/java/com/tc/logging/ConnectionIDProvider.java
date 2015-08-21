/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.net.protocol.transport.ConnectionID;


public interface ConnectionIDProvider {
  ConnectionID getConnectionId();
}
