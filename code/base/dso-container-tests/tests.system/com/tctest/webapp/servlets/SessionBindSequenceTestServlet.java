/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tctest.webapp.listeners.BindingListener;
import com.tctest.webapp.listeners.BindingListenerWithException;
import com.tctest.webapp.listeners.BindingSequenceListener;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class SessionBindSequenceTestServlet extends ListenerReportingServlet {
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final String action = req.getParameter("action");
    final String key = req.getParameter("key");
    String reply = "OK";
    if ("get".equals(action)) {
      reply = key + "=" + req.getSession().getAttribute(key);
    } else if ("set".equals(action)) {
      req.getSession().setAttribute(key, new BindingListener(key));
    } else if ("setwithexception".equals(action)) {
      try {
        req.getSession().setAttribute(key, new BindingListenerWithException(key));
        reply = "Did not get expected exception!";
      } catch (Throwable e) {
        // this is expected
      }
    } else if ("setwithsequence".equals(action)) {
      req.getSession().setAttribute(key, new BindingSequenceListener(key));
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
      req.getSession();
      sleep(1000 * Integer.parseInt(key));
      req.getSession().setMaxInactiveInterval(30 * 60);
    } else {
      reply = "INVALID REQUEST";
    }
    resp.getWriter().print(reply);
    resp.flushBuffer();
  }
}