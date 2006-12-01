/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

public class DefaultSessionId implements SessionId {

  private final String key;
  private final String requestedId;
  private final String externalId;
  private final Lock lock;
  protected DefaultSessionId(final String internalKey, final String requestedId, final String externalId) {
    Assert.pre(internalKey != null);
    Assert.pre(externalId != null);
    this.key = internalKey;
    this.requestedId = requestedId;
    this.externalId = externalId;
    this.lock = new Lock(this.key);
  }

  public String getRequestedId() {
    return requestedId;
  }

  public String getKey() {
    return key;
  }

  public boolean isNew() {
    return requestedId == null;
  }

  public boolean isServerHop() {
    return !isNew() && !externalId.equals(requestedId);
  }

  public String toString() {
    return getClass().getName() + "{ " + "key=" + getKey() + ", id=" + getRequestedId() + "}";
  }

  public String getExternalId() {
    return externalId;
  }

  public void commitLock() {
    lock.commitLock();
  }

  public void getWriteLock() {
    lock.getWriteLock();
  }

  public boolean tryWriteLock() {
    return lock.tryWriteLock();
  }

  public Lock getLock() {
    return lock;
  }
}
