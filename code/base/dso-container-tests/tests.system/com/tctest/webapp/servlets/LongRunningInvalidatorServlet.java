/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tctest.webapp.listeners.InvalidatorBindingListener;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class LongRunningInvalidatorServlet extends ListenerReportingServlet {
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final String action = req.getParameter("action");
    final String key = req.getParameter("key");
    String reply = "OK";
    if ("get".equals(action)) {
      reply = key + "=" + req.getSession().getAttribute(key);
    } else if ("set".equals(action)) {
      req.getSession().setAttribute(key, new InvalidatorBindingListener(key));
    } else if ("remove".equals(action)) {
      req.getSession().removeAttribute(key);
    } else if ("call_count".equals(action)) {
      reply = key + "=" + getCallCount(key);
    } else if ("setmax".equals(action)) {
      req.getSession().setMaxInactiveInterval(Integer.parseInt(key));
    } else if ("isNew".equals(action)) {
      if (!req.getSession().isNew()) reply = "OLD SESSION!";
    } else if ("isOld".equals(action)) {
      if (req.getSession().isNew()) reply = "NEW SESSION!";
    } else if ("sleep".equals(action)) {
      // lock session and go to sleep
      // even in case of session-locking=false, session-invalidator should not invalidate
      req.getSession();
      long now = System.currentTimeMillis();
      long stop = now + (Integer.parseInt(key) * 1000);
      while (stop > System.currentTimeMillis()) {
        try {
          System.out.println("Sleeping for 2 secs... remaining: " + (stop - System.currentTimeMillis()) + " msecs");
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          // ignored
        }
      }
      // set the maxInactive to high number unless TerracottaSessionManager.postProcessSession will invalidate the
      // session before next request
      req.getSession().setMaxInactiveInterval(30 * 60);
    } else if ("invalidate".equals(action)) {
      // invalidate the session explicitly
      req.getSession().invalidate();
    } else if ("invalidateAndAccess".equals(action)) {
      req.getSession().invalidate();
      // create a new session, access and again invalidate
      if (!req.getSession().isNew()) reply = "OLD SESSION!";
      else {
        req.getSession().setAttribute("key", "value");
        req.getSession().invalidate();
      }
    } else {
      reply = "INVALID REQUEST";
    }
    resp.getWriter().print(reply);
    resp.flushBuffer();
  }
}