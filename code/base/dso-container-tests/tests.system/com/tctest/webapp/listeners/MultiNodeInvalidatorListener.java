/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.listeners;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class MultiNodeInvalidatorListener implements HttpSessionListener {

  private final static Set sessionIDs = new HashSet();

  public void sessionCreated(HttpSessionEvent event) {
    String id = getPlainId(event);
    synchronized (sessionIDs) {
      boolean added = sessionIDs.add(id);
      if (!added) { throw new AssertionError("Failed to add " + id + " to " + sessionIDs); }
    }
  }

  public void sessionDestroyed(HttpSessionEvent event) {
    String id = getPlainId(event);
    synchronized (sessionIDs) {
      boolean removed = sessionIDs.remove(id);
      if (!removed) { throw new AssertionError("Failed to remove " + id + " from " + sessionIDs); }
    }
  }

  public static int getNumberOfSessios() {
    synchronized (sessionIDs) {
      return sessionIDs.size();
    }
  }

  // note: if we ever export TC session classes/interfaces into the visibility of web apps in all containers we can
  // remove this reflection mess
  private static final Object[]  EMPTY_ARGS = new Object[] {};
  private static volatile Method getSessionIdMethod;
  private static volatile Method getKeyMethod;

  private static String getPlainId(HttpSessionEvent event) {
    try {
      Object session = event.getSession();
      if (getSessionIdMethod == null) {
        getSessionIdMethod = session.getClass().getDeclaredMethod("getSessionId", new Class[] {});
      }

      Object sessionId = getSessionIdMethod.invoke(session, EMPTY_ARGS);
      if (getKeyMethod == null) {
        getKeyMethod = sessionId.getClass().getDeclaredMethod("getKey", new Class[] {});
      }
      return (String) getKeyMethod.invoke(sessionId, EMPTY_ARGS);
    } catch (Throwable t) {
      throw new AssertionError(t);
    }

  }

}
