/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.tc.exception.ImplementMe;

public class MockSessionId implements SessionId {

  public void commitLock() {
    throw new ImplementMe();
  }

  public String getExternalId() {
    throw new ImplementMe();
  }

  public String getKey() {
    throw new ImplementMe();
  }

  public String getRequestedId() {
    throw new ImplementMe();
  }

  public void getWriteLock() {
    throw new ImplementMe();
  }

  public boolean isNew() {
    throw new ImplementMe();
  }

  public boolean isServerHop() {
    throw new ImplementMe();
  }

  public boolean tryWriteLock() {
    throw new ImplementMe();
  }

}
