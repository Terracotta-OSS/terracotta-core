/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

public interface SessionId {

  /**
   * @return requested session id unchanged
   */
  String getRequestedId();

  /**
   * @return session id that will be returned to the client browser. This might be diff from requestedSessionId and will
   *         contain key plus, potentially, server id
   */
  String getExternalId();

  /**
   * @return part of session id that serves as a constant key into collection of session objects; the OTHER parts of
   *         session id can change throughout session lifetime, this part must stay constant.
   */
  String getKey();

  boolean isServerHop();

  boolean isNew();

  void getWriteLock();

  boolean tryWriteLock();

  void commitLock();

  /**
   * Acquires a READ lock on the session-invalidator lock No-Op in case isSessionLockingEnabled() is true
   */
  void getSessionInvalidatorReadLock();

  /**
   * Attempts to acquire a WRITE lock on the session-invalidator lock No-Op in case isSessionLockingEnabled() is true
   * Returns true if WRITE lock was acquired else returns false
   */
  boolean trySessionInvalidatorWriteLock();

  /**
   * Commits and release the session-invalidator lock No-Op in case isSessionLockingEnabled() is true
   */
  void commitSessionInvalidatorLock();
}
