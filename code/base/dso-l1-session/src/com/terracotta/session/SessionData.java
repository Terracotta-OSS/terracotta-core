/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import com.terracotta.session.util.Assert;
import com.terracotta.session.util.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SessionData {
  private final String    id;
  private final Map       attributes = new HashMap();
  private final long      createTime;
  private final Timestamp timestamp;

  private long            lastAccessTime;
  private long            maxIdleMillis;
  private transient long  requestStartMillis;

  protected SessionData(String id, int maxIdleSeconds) {
    Assert.pre(id != null && id.length() != 0);
    this.id = id;
    this.createTime = System.currentTimeMillis();
    this.lastAccessTime = 0;
    setMaxInactiveMillis(maxIdleSeconds * 1000);
    this.timestamp = new Timestamp(this.createTime + maxIdleMillis);
  }

  synchronized boolean isValid() {
    final boolean isValid = getIdleMillis() < getMaxInactiveMillis();
    return isValid;
  }

  synchronized void startRequest() {
    requestStartMillis = System.currentTimeMillis();
  }

  /**
   * returns idle millis.
   */
  synchronized long getIdleMillis() {
    if (lastAccessTime == 0) return 0;
    if (requestStartMillis > lastAccessTime) return requestStartMillis - lastAccessTime;
    return Math.max(System.currentTimeMillis() - lastAccessTime, 0);
  }

  synchronized void finishRequest() {
    requestStartMillis = 0;
    lastAccessTime = System.currentTimeMillis();
  }

  public synchronized long getCreationTime() {
    return createTime;
  }

  public synchronized long getLastAccessTime() {
    return lastAccessTime;
  }

  public synchronized Object setAttribute(String name, Object value) {
    return attributes.put(name, value);
  }

  public synchronized Object getAttribute(String name) {
    return attributes.get(name);
  }

  public synchronized String[] getAttributeNames() {
    Set keys = attributes.keySet();
    String[] rv = (String[]) keys.toArray(new String[keys.size()]);
    Assert.post(rv != null);
    return rv;
  }

  public synchronized Object removeAttribute(String name) {
    return attributes.remove(name);
  }

  public synchronized long getMaxInactiveMillis() {
    return maxIdleMillis;
  }

  public synchronized void setMaxInactiveMillis(long v) {
    maxIdleMillis = v;
  }

  String getId() {
    return id;
  }

  Timestamp getTimestamp() {
    return timestamp;
  }
}
