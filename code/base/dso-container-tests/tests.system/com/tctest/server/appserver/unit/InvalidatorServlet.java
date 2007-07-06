/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class InvalidatorServlet extends HttpServlet {
  private static Map callCounts = new HashMap();

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final String action = req.getParameter("action");
    final String key = req.getParameter("key");
    String reply = "OK";
    if ("get".equals(action)) {
      reply = key + "=" + req.getSession().getAttribute(key);
    } else if ("set".equals(action)) {
      req.getSession().setAttribute(key, new InvalidatorBindingListener(key));
    } else if ("setwithexception".equals(action)) {
      try {
        req.getSession().setAttribute(key, new BindingListenerWithException(key));
        reply = "Did not get expected exception!";
      } catch (Throwable e) {
        // this is expected
      }
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

  private void sleep(int i) {
    try {
      Date now = new Date();
      System.err.println("SERVLET: " + now + ": going to sleep for " + i + " millis");
      Thread.sleep(i);
      now = new Date();
      System.err.println("SERVLET: " + now + ": woke up from sleeping for " + i + " millis");
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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