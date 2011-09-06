/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import java.util.Map;

public class ServerMapGetValueResponse {

  private final ServerMapRequestID  requestID;
  private final Map<Object, Object> responseMap;

  public ServerMapGetValueResponse(final ServerMapRequestID requestID, final Map<Object, Object> values) {
    this.requestID = requestID;
    this.responseMap = values;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public Map<Object, Object> getValues() {
    return this.responseMap;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServerMapGetValueResponse) {
      ServerMapGetValueResponse response = (ServerMapGetValueResponse) obj;
      return getRequestID().equals(response.getRequestID()) && getValues().equals(response.getValues());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.requestID.hashCode();
  }

  @Override
  public String toString() {
    return "responses" + this.responseMap;
  }

}
