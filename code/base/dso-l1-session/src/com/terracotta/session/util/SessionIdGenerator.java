/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

public interface SessionIdGenerator {

  /**
   * Initialize whether session-locking is enabled or not.
   */
  void initialize(boolean sessionLocking);

  /**
   * @throws IllegalStateException if this method is called before {@link #initialize(boolean)}
   */
  SessionId generateNewId();

  /**
   * @throws IllegalStateException if this method is called before {@link #initialize(boolean)}
   */
  SessionId makeInstanceFromBrowserId(String requestedSessionId);

  /**
   * @throws IllegalStateException if this method is called before {@link #initialize(boolean)}
   */
  SessionId makeInstanceFromInternalKey(String key);

}
