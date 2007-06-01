/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.tc.session.SessionSupport;
import com.terracotta.session.util.ContextMgr;
import com.terracotta.session.util.LifecycleEventMgr;
import com.terracotta.session.util.StringArrayEnumeration;
import com.terracotta.session.util.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;

public class SessionData implements Session, SessionSupport {
  private final Map                   attributes         = new HashMap();
  private final Map                   internalAttributes = new HashMap();
  private transient Map               transientAttributes;
  private final long                  createTime;
  private final Timestamp             timestamp;

  private long                        lastAccessedTime;
  private long                        maxIdleMillis;
  private transient long              requestStartMillis;

  private transient SessionId         sessionId;
  private transient LifecycleEventMgr eventMgr;
  private transient ContextMgr        contextMgr;
  private transient boolean           invalidated        = false;

  protected SessionData(int maxIdleSeconds) {
    this.createTime = System.currentTimeMillis();
    this.lastAccessedTime = 0;
    setMaxInactiveMillis(maxIdleSeconds * 1000);
    this.timestamp = new Timestamp(this.createTime + maxIdleMillis);
  }

  void associate(SessionId sid, LifecycleEventMgr lifecycleEventMgr, ContextMgr ctxMgr) {
    this.sessionId = sid;
    this.eventMgr = lifecycleEventMgr;
    this.contextMgr = ctxMgr;
  }

  public SessionId getSessionId() {
    return this.sessionId;
  }

  public SessionData getSessionData() {
    return this;
  }

  public ServletContext getServletContext() {
    return contextMgr.getServletContext();
  }

  public HttpSessionContext getSessionContext() {
    return contextMgr.getSessionContext();
  }

  public synchronized boolean isValid() {
    if (invalidated) { return false; }
    final boolean isValid = getIdleMillis() < getMaxInactiveMillis();
    return isValid;
  }

  public boolean isNew() {
    checkIfValid();
    return sessionId.isNew();
  }

  public synchronized void invalidate() {
    if (invalidated) { throw new IllegalStateException("session already invalidated"); }

    try {
      eventMgr.fireSessionDestroyedEvent(this);

      String[] attrs = (String[]) attributes.keySet().toArray(new String[attributes.size()]);

      for (int i = 0; i < attrs.length; i++) {
        unbindAttribute(attrs[i]);
      }
    } finally {
      invalidated = true;
    }
  }

  private void checkIfValid() {
    if (!isValid()) throw new IllegalStateException("This session is invalid");
  }

  synchronized void startRequest() {
    requestStartMillis = System.currentTimeMillis();
  }

  public void setMaxInactiveInterval(int v) {
    setMaxInactiveMillis(v * 1000);
    if (isValid() && v == 0) {
      invalidate();
    }
  }

  /**
   * returns idle millis.
   */
  synchronized long getIdleMillis() {
    if (lastAccessedTime == 0) return 0;
    if (requestStartMillis > lastAccessedTime) return requestStartMillis - lastAccessedTime;
    return Math.max(System.currentTimeMillis() - lastAccessedTime, 0);
  }

  synchronized void finishRequest() {
    requestStartMillis = 0;
    lastAccessedTime = System.currentTimeMillis();
  }

  public synchronized long getCreationTime() {
    checkIfValid();
    return createTime;
  }

  public synchronized long getLastAccessedTime() {
    checkIfValid();
    return lastAccessedTime;
  }

  public void setAttribute(String name, Object value) {
    setAttributeReturnOld(name, value);
  }

  public synchronized Object setAttributeReturnOld(String name, Object value) {
    checkIfValid();
    if (value == null) {
      return unbindAttribute(name);
    } else {
      return bindAttribute(name, value);
    }
  }

  public void putValue(String name, Object val) {
    setAttribute(name, val);
  }

  public synchronized Object getAttribute(String name) {
    checkIfValid();
    return attributes.get(name);
  }

  public Object getValue(String name) {
    return getAttribute(name);
  }

  public synchronized String[] getValueNames() {
    checkIfValid();
    Set keys = attributes.keySet();
    return (String[]) keys.toArray(new String[keys.size()]);
  }

  public Enumeration getAttributeNames() {
    return new StringArrayEnumeration(getValueNames());
  }

  public void removeAttribute(String name) {
    removeAttributeReturnOld(name);
  }

  public synchronized Object removeAttributeReturnOld(String name) {
    checkIfValid();
    return unbindAttribute(name);
  }

  public void removeValue(String name) {
    removeAttribute(name);
  }

  synchronized long getMaxInactiveMillis() {
    return maxIdleMillis;
  }

  public int getMaxInactiveInterval() {
    return (int) getMaxInactiveMillis() / 1000;
  }

  private void setMaxInactiveMillis(long v) {
    maxIdleMillis = v;
  }

  public synchronized Object getInternalAttribute(String name) {
    checkIfValid();
    return internalAttributes.get(name);
  }

  public synchronized Object setInternalAttribute(String name, Object value) {
    checkIfValid();
    return internalAttributes.put(name, value);
  }

  public synchronized Object removeInternalAttribute(String name) {
    checkIfValid();
    return internalAttributes.remove(name);
  }

  public synchronized Object getTransientAttribute(String name) {
    checkIfValid();
    return getTransientAttributes().get(name);
  }

  public synchronized Object setTransientAttribute(String name, Object value) {
    checkIfValid();
    return getTransientAttributes().put(name, value);
  }

  public synchronized Object removeTransientAttribute(String name) {
    checkIfValid();
    return getTransientAttributes().remove(name);
  }

  public synchronized Collection getTransientAttributeKeys() {
    checkIfValid();
    return new ArrayList(getTransientAttributes().keySet());
  }

  private Object bindAttribute(String name, Object newVal) {
    Object oldVal = getAttribute(name);
    if (newVal != oldVal) eventMgr.bindAttribute(this, name, newVal);

    oldVal = attributes.put(name, newVal);

    if (oldVal != newVal) eventMgr.unbindAttribute(this, name, oldVal);

    // now deal with attribute listener events
    if (oldVal != null) eventMgr.replaceAttribute(this, name, oldVal, newVal);
    else eventMgr.setAttribute(this, name, newVal);

    return oldVal;
  }

  private Object unbindAttribute(String name) {
    Object oldVal = attributes.remove(name);
    if (oldVal != null) {
      eventMgr.unbindAttribute(this, name, oldVal);
      eventMgr.removeAttribute(this, name, oldVal);
    }
    return oldVal;
  }

  public void resumeRequest() {
    TerracottaSessionManager.resumeRequest(this);
  }

  public void pauseRequest() {
    TerracottaSessionManager.pauseRequest(this);
  }

  private Map getTransientAttributes() {
    if (transientAttributes == null) {
      transientAttributes = new HashMap();
    }
    return transientAttributes;
  }

  public String getId() {
    return sessionId.getExternalId();
  }

  Timestamp getTimestamp() {
    return timestamp;
  }

  public synchronized boolean isInvalidated() {
    return invalidated;
  }

}
