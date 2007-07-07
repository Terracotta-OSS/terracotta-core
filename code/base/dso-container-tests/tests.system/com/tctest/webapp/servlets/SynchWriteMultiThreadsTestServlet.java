/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class SynchWriteMultiThreadsTestServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    String server = request.getParameter("server");
    String command = request.getParameter("command");
    String data = request.getParameter("data");

    switch (Integer.parseInt(server)) {
      case 0:
        handleServer0(session, out, command, data);
        break;
      case 1:
        handleServer1(session, out, command, data);
        break;
      default:
        out.print("unknown value for server param: " + server);
    }
    out.flush();
  }

  private void handleServer0(HttpSession session, PrintWriter out, String command, String data) {
    if (command.equals("ping")) {
      session.setAttribute("ping", "pong");
      out.println("OK");
    } else if (command.equals("insert")) {
      session.setAttribute("data" + data, data + "");
      out.println("OK");
    } else if (command.equals("kill")) {
      out.println("OK");
      System.err.println("Execute order 66... halt.");
      System.err.flush();
      Runtime.getRuntime().halt(1);
    }
  }

  private void handleServer1(HttpSession session, PrintWriter out, String command, String data) {
    if (command.equals("ping")) {
      String ping = (String) session.getAttribute("ping");
      if (ping == null) {
        out.println("ping is null");
      } else out.println("OK");
    } else if (command.equals("query")) {

      String log = "** " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + " | sessionId="
                   + session.getId().substring(0, 5) + " | command=" + command + " | data=" + data;

      String queried_data = (String) session.getAttribute("data" + data);
      if (queried_data == null) {
        out.println("data" + data + " is null");
      } else out.println(queried_data);

      System.err.println(log + "## found=" + queried_data);
      System.err.flush();
    }
  }
}