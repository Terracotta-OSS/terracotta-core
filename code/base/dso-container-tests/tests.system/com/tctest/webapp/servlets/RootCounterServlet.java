/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RootCounterServlet extends HttpServlet {
  private final RootCounterServlet.Counter counterObject = new Counter();

  private static class Counter {
    private int counter;

    public Counter() {
      counter = 0;
    }

    public synchronized void increment() {
      counter++;
    }

    public synchronized void setValue(int newValue) {
      counter = newValue;
    }

    public synchronized int getValue() {
      return counter;
    }
  }

  private int getCurrentCountValue() {
    counterObject.increment();
    return counterObject.getValue();
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/plain");
    PrintWriter out = response.getWriter();
    out.println(getCurrentCountValue());
  }
}