/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class UnlockedSessionObjectServlet extends HttpServlet {

  public static final String CREATE_SESSION      = "createSession";
  public static final String MUTATE_WITHOUT_LOCK = "mutateWithoutLock";
  public static final String MUTATE_WITH_LOCK    = "mutateWithLock";
  public static final String INSERT              = "insert";
  public static final String KEY                 = "key";

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String cmd = req.getParameter("cmd");

    HttpSession session = req.getSession(true);
    String sessionId = session.getId();
    debug("Current sessionId: " + sessionId + " cmd=" + cmd);

    if (CREATE_SESSION.equals(cmd)) {
      session.setAttribute("dumbValue", "dumbValue");
      resp.getWriter().print("OK");
      return;
    } else if (INSERT.equals(cmd)) {
      debug("Serving request for cmd=" + INSERT);
      DumbData dumbData = new DumbData();
      debug("Adding value for key=" + KEY + " in session:" + session);
      session.setAttribute(KEY, dumbData);
      resp.getWriter().print("OK");
      return;
    } else if (MUTATE_WITHOUT_LOCK.equals(cmd)) {
      debug("Serving request for cmd=" + MUTATE_WITHOUT_LOCK);
      DumbData dumbData = (DumbData) session.getAttribute(KEY);
      try {
        debug("Trying to mutate without lock...");
        dumbData.setDumbProperty("newProperty");
        debug("Mutated without lock");
      } catch (Throwable e) {
        resp.getWriter().print(e.getClass().getName());
        debug("Caught throwable : " + e);
        return;
      }
      resp.getWriter().print("OK");
      return;
    } else if (MUTATE_WITH_LOCK.equals(cmd)) {
      debug("Serving request for cmd=" + MUTATE_WITH_LOCK);
      DumbData dumbData = (DumbData) session.getAttribute(KEY);
      mutateWithLock(dumbData);
      resp.getWriter().print("OK");
      return;
    }
    resp.getWriter().print("Unknown request: cmd=" + cmd);
  }

  private void mutateWithLock(DumbData dumbData) {
    synchronized (dumbData) {
      dumbData.setDumbProperty("newProperty");
    }
  }

  private static void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

  private static class DumbData {
    private String dumbProperty;

    public DumbData() {
      this.dumbProperty = "defaultValue";
    }

    public String getDumbProperty() {
      return dumbProperty;
    }

    public void setDumbProperty(String dumbProperty) {
      this.dumbProperty = dumbProperty;
    }

  }

}
