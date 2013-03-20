/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import java.util.HashMap;
import java.util.Map;

public class ServerMapGetValueResponse {

  private final ServerMapRequestID  requestID;
  private final Map<Object, Object> responseMap = new HashMap<Object, Object>();

  public ServerMapGetValueResponse(final ServerMapRequestID requestID) {
    this.requestID = requestID;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public Map<Object, Object> getValues() {
    return this.responseMap;
  }

  public void put(Object key, Object value, long creationTime, long lastAccessedTime, long timeToIdle, long timeToLive) {
    responseMap.put(key, new ResponseValue(value, creationTime, lastAccessedTime, timeToIdle, timeToLive));
  }

  public void put(Object key, Object value) {
    put(key, value, 0, 0, 0, 0);
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

  public static class ResponseValue {
    private final long creationTime;
    private final long lastAccessedTime;
    private final long timeToIdle;
    private final long timeToLive;
    private final Object data;

    public ResponseValue(final Object data, final long creationTime, final long lastAccessedTime, final long timeToIdle, final long timeToLive) {
      this.creationTime = creationTime;
      this.lastAccessedTime = lastAccessedTime;
      this.timeToIdle = timeToIdle;
      this.timeToLive = timeToLive;
      this.data = data;
    }

    public long getCreationTime() {
      return creationTime;
    }

    public long getLastAccessedTime() {
      return lastAccessedTime;
    }

    public long getTimeToIdle() {
      return timeToIdle;
    }

    public long getTimeToLive() {
      return timeToLive;
    }

    public Object getData() {
      return data;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final ResponseValue that = (ResponseValue)o;

      if (creationTime != that.creationTime) return false;
      if (lastAccessedTime != that.lastAccessedTime) return false;
      if (timeToIdle != that.timeToIdle) return false;
      if (timeToLive != that.timeToLive) return false;
      if (!data.equals(that.data)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = (int)(creationTime ^ (creationTime >>> 32));
      result = 31 * result + (int)(lastAccessedTime ^ (lastAccessedTime >>> 32));
      result = 31 * result + (int)(timeToIdle ^ (timeToIdle >>> 32));
      result = 31 * result + (int)(timeToLive ^ (timeToLive >>> 32));
      result = 31 * result + data.hashCode();
      return result;
    }
  }
}
