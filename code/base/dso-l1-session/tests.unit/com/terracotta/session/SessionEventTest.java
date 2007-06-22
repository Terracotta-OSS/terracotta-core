/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.terracotta.session.util.ContextMgr;
import com.terracotta.session.util.DefaultIdGenerator;
import com.terracotta.session.util.MockContextMgr;
import com.terracotta.session.util.MockLifecycleEventMgr;
import com.terracotta.session.util.SessionIdGenerator;

import junit.framework.TestCase;

public class SessionEventTest extends TestCase {

  private String                serverId;
  private SessionId             sessionId;
  private MockLifecycleEventMgr eventMgr;
  private ContextMgr            contextMgr;
  private SessionData           session;
  private int                   maxIdleSeconds;
  private SessionIdGenerator    idGenerator;
  private SessionManager        sessionManager;

  protected void setUp() throws Exception {
    maxIdleSeconds = 123;
    serverId = "someServerId";
    idGenerator = new DefaultIdGenerator(20, serverId);
    sessionId = idGenerator.generateNewId();
    eventMgr = new MockLifecycleEventMgr();
    contextMgr = new MockContextMgr();
    session = new SessionData(maxIdleSeconds);
    sessionManager = new MockSessionManager();
    session.associate(sessionId, eventMgr, contextMgr, sessionManager);
  }

  public final void testEvents() {
    final String name = "someName";
    session.setAttribute(name, name);
    assertEquals("setAttribute", eventMgr.getLastMethod());
    assertSame(session, eventMgr.getSession());
    assertSame(name, eventMgr.getName());
    assertSame(name, eventMgr.getValue());
    eventMgr.clear();

    session.setAttribute(name, name);
    assertEquals("replaceAttribute", eventMgr.getLastMethod());
    assertSame(session, eventMgr.getSession());
    assertSame(name, eventMgr.getName());
    assertSame(name, eventMgr.getValue());
    eventMgr.clear();

    session.removeAttribute(name);
    assertEquals("removeAttribute", eventMgr.getLastMethod());
    assertSame(session, eventMgr.getSession());
    assertSame(name, eventMgr.getName());
    assertSame(name, eventMgr.getValue());
    eventMgr.clear();

    session.removeAttribute(name);
    assertNull(eventMgr.getLastMethod());
    assertNull(eventMgr.getSession());
    assertNull(eventMgr.getName());
    assertNull(eventMgr.getValue());
    eventMgr.clear();

    session.invalidate();
    assertEquals("fireSessionDestroyedEvent", eventMgr.getLastMethod());
    assertSame(session, eventMgr.getSession());
    eventMgr.clear();
  }
}
