/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tctest.webapp.listeners.BindingListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ListenerReportingServlet extends HttpServlet {
  private static Map callCounts = new HashMap();

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // requests should have 2 standard params: action=get|set|remove|call_count and key
    final String action = req.getParameter("action");
    final String key = req.getParameter("key");
    String reply = "OK";
    if ("get".equals(action)) {
      reply = key + "=" + req.getSession().getAttribute(key);
    } else if ("set".equals(action)) {
      req.getSession().setAttribute(key, new BindingListener(key));
    } else if ("remove".equals(action)) {
      req.getSession().removeAttribute(key);
    } else if ("call_count".equals(action)) {
      reply = key + "=" + getCallCount(key);
    } else if ("invalidate".equals(action)) {
      req.getSession().invalidate();
    } else if ("isNew".equals(action)) {
      if (!req.getSession().isNew()) reply = "ERROR: OLD SESSION!";
    } else {
      reply = "INVALID REQUEST";
    }
    resp.getWriter().print(reply);
    resp.flushBuffer();
  }

  private synchronized static int getCallCount(String key) {
    Integer i = (Integer) callCounts.get(key);
    return i == null ? 0 : i.intValue();
  }

  public synchronized static void incrementCallCount(String key) {
    Integer i = (Integer) callCounts.get(key);
    if (i == null) {
      i = new Integer(1);
    } else {
      i = new Integer(i.intValue() + 1);
    }
    callCounts.put(key, i);
  }
}
