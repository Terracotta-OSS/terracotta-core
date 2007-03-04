/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.session.SessionSupport;
import com.terracotta.session.Session;
import com.terracotta.session.SessionData;
import com.terracotta.session.SessionId;
import com.terracotta.session.TerracottaSessionManager;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;

public class StandardSession implements Session, SessionSupport {

  private final SessionData       data;
  private final SessionId         id;
  private final LifecycleEventMgr eventMgr;
  private final ContextMgr        contextMgr;

  private boolean                 isValid      = true;
  private boolean                 invalidating = false;

  public StandardSession(SessionId id, SessionData data, LifecycleEventMgr mgr, ContextMgr contextMgr) {
    Assert.pre(id != null);
    Assert.pre(data != null);
    Assert.pre(mgr != null);
    Assert.pre(contextMgr != null);
    this.data = data;
    this.id = id;
    this.eventMgr = mgr;
    this.contextMgr = contextMgr;
  }

  // //////////////////////////////////////////
  // Attribute methods
  // //////////////////////////////////////////
  public Object getAttribute(String name) {
    checkIfValid();
    Object rv = data.getAttribute(name);
    return rv;
  }

  public void setAttribute(String name, Object value) {
    checkIfValid();
    if (value == null) unbindAttribute(name);
    else bindAttribute(name, value);
  }

  public void removeAttribute(String name) {
    checkIfValid();
    unbindAttribute(name);
  }

  public Enumeration getAttributeNames() {
    checkIfValid();
    String[] names = data.getAttributeNames();
    return new StringArrayEnumeration(names);
  }

  // //////////////////////////////////////////
  // Value methods
  // //////////////////////////////////////////
  public Object getValue(String name) {
    return getAttribute(name);
  }

  public void putValue(String name, Object val) {
    setAttribute(name, val);
  }

  public void removeValue(String name) {
    removeAttribute(name);
  }

  public String[] getValueNames() {
    checkIfValid();
    return data.getAttributeNames();
  }

  // //////////////////////////////////////////
  // Time and lifetime methods
  // //////////////////////////////////////////
  public void invalidate() {
    checkIfValid();
    synchronized (data) {
      checkIfInvalidating();
      invalidating = true;

      String names[] = data.getAttributeNames();
      for (int i = 0; i < names.length; i++)
        unbindAttribute(names[i]);
      eventMgr.fireSessionDestroyedEvent(this);
      this.setInvalid();
    }
  }

  private void checkIfInvalidating() {
    if (invalidating) { throw new IllegalStateException("already invalidating sesssion"); }
  }

  public boolean isNew() {
    checkIfValid();
    return id.isNew();
  }

  public long getCreationTime() {
    checkIfValid();
    return data.getCreationTime();
  }

  public long getLastAccessedTime() {
    return data.getLastAccessTime();
  }

  public int getMaxInactiveInterval() {
    return (int) data.getMaxInactiveMillis() / 1000;
  }

  public void setMaxInactiveInterval(int v) {
    data.setMaxInactiveMillis(v * 1000);
    if (isValid && v == 0) {
      invalidate();
    }

  }

  public String getId() {
    return id.getExternalId();
  }

  // //////////////////////////////////////////
  // Context methods
  // //////////////////////////////////////////
  public ServletContext getServletContext() {
    return contextMgr.getServletContext();
  }

  public HttpSessionContext getSessionContext() {
    return contextMgr.getSessionContext();
  }

  // //////////////////////////////////////////
  // Implementation-internal methods
  // //////////////////////////////////////////
  public SessionData getSessionData() {
    return data;
  }

  public SessionId getSessionId() {
    return id;
  }

  public void bindAttribute(String name, Object newVal) {
    Object oldVal = data.getAttribute(name);
    if (newVal != oldVal) eventMgr.bindAttribute(this, name, newVal);

    oldVal = data.setAttribute(name, newVal);

    if (oldVal != newVal) eventMgr.unbindAttribute(this, name, oldVal);

    // now deal with attribute listener events
    if (oldVal != null) eventMgr.replaceAttribute(this, name, oldVal, newVal);
    else eventMgr.setAttribute(this, name, newVal);
  }

  public void unbindAttribute(String name) {
    Object oldVal = data.removeAttribute(name);
    if (oldVal != null) {
      eventMgr.unbindAttribute(this, name, oldVal);
      eventMgr.removeAttribute(this, name, oldVal);
    }
  }

  public synchronized void setInvalid() {
    isValid = false;
  }

  public void checkIfValid() {
    if (!isValid) throw new IllegalStateException("This session is invalid");
  }

  public boolean isValid() {
    return isValid;
  }

  public void resumeRequest() {
    TerracottaSessionManager.resumeRequest(this);
  }

  public void pauseRequest() {
    TerracottaSessionManager.pauseRequest(this);
  }
}
