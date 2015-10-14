/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.ClientID;

public class InvalidateObjectsForClientContext implements MultiThreadedEventContext {

  private final ClientID clientID;

  public InvalidateObjectsForClientContext(ClientID clientID) {
    this.clientID = clientID;
  }

  public ClientID getClientID() {
    return clientID;
  }

  @Override
  public Object getSchedulingKey() {
    return clientID;
  }

  @Override
  public boolean flush() {
//  this is independent for each client and does not require a flush
    return false;
  }
}
