/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.sessions;

import java.util.Iterator;
import java.util.LinkedList;

import javax.management.NotCompliantMBeanException;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.stats.AggregateInteger;

public final class SessionMonitor extends AbstractTerracottaMBean implements SessionMonitorMBean {

  final AggregateInteger   requestsProcessed;
  final AggregateInteger   sessionsCreated;
  final AggregateInteger   sessionsDestroyed;
  private final LinkedList sessionsControllers;

  public SessionMonitor() throws NotCompliantMBeanException {
    super(SessionMonitorMBean.class, false);
    requestsProcessed = new AggregateInteger("RequestsProcessed", 100);
    sessionsCreated = new AggregateInteger("CreatedSessionCount", 100);
    sessionsDestroyed = new AggregateInteger("DestroyedSessionCount", 100);
    sessionsControllers = new LinkedList();
  }

  public int getRequestCount() {
    return requestsProcessed.getN();
  }

  public int getRequestRatePerSecond() {
    return requestsProcessed.getSampleRate(1000);
  }

  public int getCreatedSessionCount() {
    return sessionsCreated.getN();
  }

  public int getSessionCreationRatePerMinute() {
    return sessionsCreated.getSampleRate(1000 * 60);
  }

  public int getDestroyedSessionCount() {
    return sessionsDestroyed.getN();
  }

  public int getSessionDestructionRatePerMinute() {
    return sessionsDestroyed.getSampleRate(1000 * 60);
  }

  public synchronized void reset() {
    requestsProcessed.reset();
    sessionsCreated.reset();
    sessionsDestroyed.reset();
    synchronized (sessionsControllers) {
      sessionsControllers.clear();
    }
  }

  public boolean expireSession(String sessionId) {
    boolean sessionExpired = false;
    synchronized (sessionsControllers) {
      for (Iterator iter = sessionsControllers.iterator(); iter.hasNext();) {
        SessionsComptroller controller = (SessionsComptroller) iter.next();
        sessionExpired = sessionExpired || controller.killSession(sessionId);
      }
    }
    return sessionExpired;
  }

  public synchronized void sessionCreated() {
    if (isEnabled()) sessionsCreated.addSample(1);
  }

  public synchronized void sessionDestroyed() {
    if (isEnabled()) sessionsDestroyed.addSample(1);
  }

  public synchronized void requestProcessed() {
    if (isEnabled()) requestsProcessed.addSample(1);
  }

  public synchronized void registerSessionsController(SessionsComptroller controller) {
    if (isEnabled()) {
      synchronized (sessionsControllers) {
        sessionsControllers.add(controller);
      }
    }
  }

}
