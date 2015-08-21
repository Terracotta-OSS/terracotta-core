/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerMapGetValueResponse {

  private static final long                     DEFAULT_VERSION = -1L;
  private final ServerMapRequestID  requestID;
  private final Map<Object, Object> responseMap = new HashMap<Object, Object>();
  private final Map<ObjectID, CompoundResponse> replaceMap = new HashMap<ObjectID, CompoundResponse>();
  private int replaceCount;

  public ServerMapGetValueResponse(final ServerMapRequestID requestID) {
    this.requestID = requestID;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public Map<Object, Object> getValues() {
    return this.responseMap;
  }

  public void put(Object key, ObjectID value, boolean expectReplacement, long creationTime, long lastAccessedTime,
                  long timeToIdle, long timeToLive, long version) {
    CompoundResponse setter = new CompoundResponse(value, creationTime, lastAccessedTime, timeToIdle, timeToLive,
                                                   version);
    if ( expectReplacement ) {
      replaceMap.put(value, setter);
    }
    responseMap.put(key, setter);
  }

  public void put(Object key, ObjectID value) {
    put(key, value, false, 0, 0, 0, 0, DEFAULT_VERSION);
  }
  
  public Set<ObjectID> getObjectIDs() {
    return replaceMap.keySet();
  }
  
  public boolean replace(ObjectID oid, Object value) {
    CompoundResponse placed = replaceMap.remove(oid);
    if ( placed != null ) {
      placed.setData(value);
      replaceCount += 1;
      return true;
    }
    return false;
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
    return "responses " + this.responseMap.size() + " replaces " + this.replaceMap.size() + " replace count " + replaceCount;
  }
}
