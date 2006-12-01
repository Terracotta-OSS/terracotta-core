/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

public class MockIdGenerator implements SessionIdGenerator {

  private SessionId id;

  public MockIdGenerator(SessionId id) {
    this.id = id;
  }

  public SessionId generateNewId() {
    return id;
  }

  public SessionId makeInstanceFromBrowserId(String requestedSessionId) {
    return id;
  }

  public SessionId makeInstanceFromInternalKey(String key) {
    return id;
  }

}
