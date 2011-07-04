/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public class ServerMapGetValueRequest {

  private final ServerMapRequestID requestID;
  private final Object             key;

  public ServerMapGetValueRequest(final ServerMapRequestID serverMapRequestID, final Object key) {
    this.requestID = serverMapRequestID;
    this.key = key;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public Object getKey() {
    return this.key;
  }

}
