/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.terracotta.session.util.Assert;
import com.terracotta.session.util.Lock;
import com.terracotta.session.util.Timestamp;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class SessionDataStore {

  private final Map store;                // <SessionData>
  private final Map dtmStore;             // <Timestamp>
  private final int maxIdleTimeoutSeconds;

  public SessionDataStore(String appName, int maxIdleTimeoutSeconds) {
    Assert.pre(appName != null && appName.length() > 0);

    this.maxIdleTimeoutSeconds = maxIdleTimeoutSeconds;

    final String sessionRootName = "tc:session_" + appName;
    final String dtmRootName = "@tc:session_timestamp_" + appName;
    final Lock lock = new Lock(sessionRootName);
    lock.getWriteLock();
    try {
      this.store = (Hashtable) ManagerUtil.lookupOrCreateRootNoDepth(sessionRootName, new Hashtable());
      ((Manageable) store).__tc_managed().disableAutoLocking();
      this.dtmStore = (Hashtable) ManagerUtil.lookupOrCreateRootNoDepth(dtmRootName, new Hashtable());
      ((Manageable) dtmStore).__tc_managed().disableAutoLocking();
    } finally {
      lock.commitLock();
    }
    Assert.post(store != null);
  }

  /**
   * <ol>
   * <li>get WRITE_LOCK for sessId
   * <li>creates session data
   * <li>put newly-created SessionData into the global Map
   * <li>returns newly-created SessionData
   * </ol>
   */
  public SessionData createSessionData(final SessionId sessId) {
    Assert.pre(sessId != null);
    SessionData rv = null;
    sessId.getWriteLock();
    rv = new SessionData(sessId.getKey(), maxIdleTimeoutSeconds);
    store.put(sessId.getKey(), rv);
    dtmStore.put(sessId.getKey(), rv.getTimestamp());
    rv.startRequest();
    return rv;
  }

  /**
   * <ol>
   * <li>get WRITE_LOCK for sessId
   * <li>look up SessionData for sessId.getKey() in the global Map
   * <li>if SessionData is invalid, unlock sessId and return null (invalidator will take care of killing this session)
   * <li>return valid SessionData
   */
  public SessionData find(final SessionId sessId) {
    Assert.pre(sessId != null);

    SessionData rv = null;
    sessId.getWriteLock();
    try {
      rv = (SessionData) store.get(sessId.getKey());
      if (rv != null) {
        rv.startRequest();
        if (!rv.isValid()) rv = null;
        else {
          updateTimestampIfNeeded(rv);
        }
      }
    } finally {
      if (rv == null) sessId.commitLock();
    }
    return rv;
  }

  void updateTimestampIfNeeded(SessionData sd) {
    Assert.pre(sd != null);
    final long now = System.currentTimeMillis();
    final Timestamp t = sd.getTimestamp();
    final long diff = t.getMillis() - now;
    if (diff < (sd.getMaxInactiveMillis() / 2) || diff > (sd.getMaxInactiveMillis())) {
      t.setMillis(now + sd.getMaxInactiveMillis());
    }
  }

  public void remove(final SessionId id) {
    Assert.pre(id != null);
    id.getWriteLock();
    try {
      store.remove(id.getKey());
      dtmStore.remove(id.getKey());
    } finally {
      id.commitLock();
    }
  }

  public String[] getAllKeys() {
    String[] rv;
    synchronized (store) {
      Set keys = store.keySet();
      rv = (String[]) keys.toArray(new String[keys.size()]);
    }
    Assert.post(rv != null);
    return rv;
  }

  Timestamp findTimestampUnlocked(final SessionId sessId) {
    return (Timestamp) dtmStore.get(sessId.getKey());
  }

  SessionData findSessionDataUnlocked(final SessionId sessId) {
    final SessionData rv = (SessionData) store.get(sessId.getKey());
    return rv;
  }

}
