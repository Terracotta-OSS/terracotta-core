/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public class ServerMapGetValueResponse {

  private final ServerMapRequestID requestID;
  private final Object             value;

  public ServerMapGetValueResponse(final ServerMapRequestID requestID, final Object value) {
    this.requestID = requestID;
    this.value = value;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public Object getValue() {
    return this.value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServerMapGetValueResponse) {
      ServerMapGetValueResponse response = (ServerMapGetValueResponse) obj;
      return getRequestID().equals(response.getRequestID()) && getValue().equals(response.getValue());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.requestID.hashCode();
  }

  
}
