/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LongRunningTestServlet extends HttpServlet {

  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
    try {
      doGet0(req, res);
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t.getMessage());
    }
  }

  private void doGet0(HttpServletRequest req, HttpServletResponse res) throws IOException {
    Random random = new Random();

    String action = req.getParameter("action");

    if ("create".equals(action)) {
      doCreate(req, res, random);
    } else if ("mutate".equals(action)) {
      doMutate(req, res, random);
    } else if ("add".equals(action)) {
      doAdd(req, res, random);
    } else if ("remove".equals(action)) {
      doRemove(req, res, random);
    } else if ("invalidate".equals(action)) {
      doInvalidate(req, res, random);
    } else {
      throw new AssertionError("unknown action: " + action);
    }

    createGarbageAndConsumeCPU(random);

    res.setStatus(HttpServletResponse.SC_OK);
    PrintWriter writer = res.getWriter();
    writer.println("OK");
  }

  private void createGarbageAndConsumeCPU(Random random) {
    List list = new LinkedList();

    for (int i = 0; i < 512; i++) {
      list.add(String.valueOf(System.currentTimeMillis()));
    }

    int hc = Math.abs(list.hashCode());
    hc = hc == Integer.MIN_VALUE ? 0 : hc;

    double d = 0;
    while ((hc % 10000) != 0) {
      d += hc;
      d = Math.sqrt(d);
      hc--;
    }

    if (d == random.nextDouble()) {
      System.err.println("Really?");
    }

  }

  private void doRemove(HttpServletRequest req, HttpServletResponse res, Random random) {
    HttpSession session = getSession(req, false);

    String[] attrs = session.getValueNames();
    if (attrs.length == 0) { return; }

    String attr = attrs[random.nextInt(attrs.length)];
    session.removeAttribute(attr);
  }

  private void doInvalidate(HttpServletRequest req, HttpServletResponse res, Random random) {
    HttpSession session = getSession(req, false);
    session.invalidate();
  }

  private void doAdd(HttpServletRequest req, HttpServletResponse res, Random random) {
    HttpSession session = getSession(req, false);
    session.setAttribute("add" + System.currentTimeMillis(), makeGraph(random));
  }

  private void doMutate(HttpServletRequest req, HttpServletResponse res, Random random) {
    HttpSession session = getSession(req, false);

    String[] attrs = session.getValueNames();
    if (attrs.length == 0) { return; }

    String attr = attrs[random.nextInt(attrs.length)];
    DataObject obj = (DataObject) session.getAttribute(attr);
    obj.mutate(random);

    // Do this for containers that require it (could be skipped for DSO, but it shouldn't matter)
    session.setAttribute(attr, obj);
  }

  private void doCreate(HttpServletRequest req, HttpServletResponse res, Random random) {
    HttpSession session = getSession(req, true);

    Integer idle = Integer.valueOf(req.getParameter("idle"));
    session.setMaxInactiveInterval(idle.intValue());

    int numAttrs = random.nextInt(5);
    for (int i = 0; i < numAttrs; i++) {
      session.setAttribute("create" + i, makeGraph(random));
    }
  }

  static DataObject makeGraph(Random random) {
    return makeGraph(random, random.nextInt(100) + 1);
  }

  static HttpSession getSession(HttpServletRequest req, boolean expectedNewValue) {
    HttpSession session = req.getSession(true);
    boolean isNew = session.isNew();
    if (expectedNewValue != isNew) { throw new AssertionError("session.isNew() has incorrect value: " + isNew
                                                              + " for requested ID " + req.getRequestedSessionId()); }
    return session;
  }

  static DataObject makeGraph(Random random, int maxNodes) {
    int num = random.nextInt(maxNodes) + 1;
    DataObject root = new DataObject();
    for (int i = 0; i < num; i++) {
      root.expand(random);
    }
    return root;
  }

  private static class DataObject {
    private int        f1;
    private int        f2;
    private int        f3;
    private int        f4;

    private DataObject ref1;
    private DataObject ref2;

    private void mutateLocal() {
      f1++;
      f2++;
      f3++;
      f4++;
    }

    void mutate(Random random) {
      mutateLocal();

      if (random.nextBoolean()) {
        if (ref1 != null) {
          ref1.mutate(random);
        }
      } else {
        if (ref2 != null) {
          ref2.mutate(random);
        }
      }
    }

    void expand(Random random) {
      if (ref1 == null && ref2 == null) {
        ref1 = new DataObject();
        return;
      }

      if (ref1 == null) {
        if (random.nextBoolean()) {
          ref2.expand(random);
        } else {
          ref1 = new DataObject();
        }
        return;
      }

      if (ref2 == null) {
        if (random.nextBoolean()) {
          ref1.expand(random);
        } else {
          ref2 = new DataObject();
        }
        return;
      }

      if (random.nextBoolean()) {
        ref1.expand(random);
      } else {
        ref2.expand(random);
      }
    }
  }

}
