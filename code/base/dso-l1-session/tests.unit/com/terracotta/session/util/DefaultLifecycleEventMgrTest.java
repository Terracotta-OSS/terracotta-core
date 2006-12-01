/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.Session;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.MockSessionAttributeListener;
import javax.servlet.http.MockSessionBindingListener;
import javax.servlet.http.MockSessionListener;

import junit.framework.TestCase;

public class DefaultLifecycleEventMgrTest extends TestCase {

  private DefaultLifecycleEventMgr       eventMgr;
  private MockSessionAttributeListener   attributeListener;
  private HttpSessionAttributeListener[] attributeListeners;
  private MockSessionListener            sessionListener;
  private HttpSessionListener[]          sessionListeners;
  private Session                        sess;

  protected void setUp() throws Exception {
    sess = new MockSession();
    attributeListener = new MockSessionAttributeListener();
    attributeListeners = new HttpSessionAttributeListener[] { attributeListener };
    sessionListener = new MockSessionListener();
    sessionListeners = new HttpSessionListener[] { sessionListener };
    eventMgr = new DefaultLifecycleEventMgr(attributeListeners, sessionListeners);
  }

  public void testFireSessionCreatedEvent() {
    eventMgr.fireSessionCreatedEvent(sess);
    assertEquals("sessionCreated", sessionListener.getLastMethod());
    assertSame(sess, sessionListener.getLastEvent().getSession());
  }

  public void testInvalidate() {
    eventMgr.fireSessionDestroyedEvent(sess);
    assertEquals("sessionDestroyed", sessionListener.getLastMethod());
    assertSame(sess, sessionListener.getLastEvent().getSession());
  }

  public void testBindingEvents() {
    final String name = "SomeAttributeName";
    final MockSessionBindingListener val = new MockSessionBindingListener();

    eventMgr.bindAttribute(sess, name, val);
    checkBindingEvent("valueBound", name, val, val);
    val.clear();

    eventMgr.unbindAttribute(sess, name, val);
    checkBindingEvent("valueUnbound", name, val, val);
    val.clear();
    attributeListener.clear();
  }

  public void testAttributeEvents() {
    final String name = "SomeAttrName";
    final Object val = name;
    
    eventMgr.setAttribute(sess, name, val);
    checkAttributeEvent("attributeAdded", name, val);

    eventMgr.removeAttribute(sess, name, val);
    checkAttributeEvent("attributeRemoved", name, val);

    eventMgr.replaceAttribute(sess, name, val, val);
    checkAttributeEvent("attributeReplaced", name, val);

  }

  private void checkAttributeEvent(final String eventName, final String attrName, final Object attrVal) {
    assertEquals(eventName, attributeListener.getLastMethod());
    HttpSessionBindingEvent  e = attributeListener.getLastEvent();
    assertNotNull(e);
    assertEquals(attrName, e.getName());
    assertSame(attrVal, e.getValue());
  }
  private void checkBindingEvent(final String bindEventName, final String attrName,
                                 final MockSessionBindingListener listener, final Object val) {
    assertEquals(bindEventName, listener.getLastEventName());
    HttpSessionBindingEvent e = listener.getLastEvent();
    assertNotNull(e);
    assertSame(sess, e.getSession());
    assertSame(attrName, e.getName());
    assertSame(val, e.getValue());
  }
}
