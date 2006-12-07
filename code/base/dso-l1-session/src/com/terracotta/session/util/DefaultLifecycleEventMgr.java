/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.Session;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class DefaultLifecycleEventMgr implements LifecycleEventMgr {

  private final HttpSessionAttributeListener[] attributeListeners;
  private final HttpSessionListener[]          sessionListeners;

  public static LifecycleEventMgr makeInstance(ConfigProperties cp) {
    Assert.pre(cp != null);
    final HttpSessionAttributeListener[] attrListeners = cp.getSessionAttributeListeners();
    final HttpSessionListener[] sessListeners = cp.getSessionListeners();
    return new DefaultLifecycleEventMgr(attrListeners, sessListeners);
  }

  public DefaultLifecycleEventMgr(HttpSessionAttributeListener[] attributeListeners,
                                  HttpSessionListener[] sessionListeners) {
    this.attributeListeners = (attributeListeners == null) ? new HttpSessionAttributeListener[0] : attributeListeners;
    this.sessionListeners = (sessionListeners == null) ? new HttpSessionListener[0] : sessionListeners;
  }

  public void fireSessionCreatedEvent(Session s) {
    Assert.pre(s != null);
    HttpSessionEvent e = new HttpSessionEvent(s);
    for (int i = 0; i < sessionListeners.length; i++) {
      HttpSessionListener l = sessionListeners[i];
      if (l != null) l.sessionCreated(e);
    }
  }

  public void fireSessionDestroyedEvent(Session session) {
    Assert.pre(session != null);
    HttpSessionEvent e = new HttpSessionEvent(session);
    for (int i = 0; i < sessionListeners.length; i++) {
      HttpSessionListener l = sessionListeners[i];
      if (l != null) l.sessionDestroyed(e);
    }
  }

  public void bindAttribute(Session sess, String name, Object val) {
    if (val instanceof HttpSessionBindingListener) {
      Assert.pre(sess != null);
      HttpSessionBindingEvent e = new HttpSessionBindingEvent(sess, name, val);
      ((HttpSessionBindingListener) val).valueBound(e);
    }
  }

  public void unbindAttribute(Session sess, String name, Object val) {
    if (val instanceof HttpSessionBindingListener) {
      Assert.pre(sess != null);
      HttpSessionBindingEvent e = new HttpSessionBindingEvent(sess, name, val);
      ((HttpSessionBindingListener) val).valueUnbound(e);
    }
  }

  public void removeAttribute(Session sess, String name, Object val) {
    Assert.pre(sess != null);
    HttpSessionBindingEvent e = new HttpSessionBindingEvent(sess, name, val);
    for (int i = 0; i < attributeListeners.length; i++) {
      HttpSessionAttributeListener l = attributeListeners[i];
      if (l != null) l.attributeRemoved(e);
    }
  }

  public void replaceAttribute(Session sess, String name, Object oldVal, Object newVal) {
    Assert.pre(sess != null);
    Assert.pre(oldVal != null);
    HttpSessionBindingEvent e = new HttpSessionBindingEvent(sess, name, oldVal);
    for (int i = 0; i < attributeListeners.length; i++) {
      HttpSessionAttributeListener l = attributeListeners[i];
      if (l != null) l.attributeReplaced(e);
    }
  }

  public void setAttribute(Session sess, String name, Object val) {
    Assert.pre(sess != null);
    HttpSessionBindingEvent e = new HttpSessionBindingEvent(sess, name, val);
    for (int i = 0; i < attributeListeners.length; i++) {
      HttpSessionAttributeListener l = attributeListeners[i];
      if (l != null) l.attributeAdded(e);
    }
  }
}
