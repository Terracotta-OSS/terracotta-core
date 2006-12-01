/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.servlets;

import com.tctest.performance.sampledata.OrganicObjectGraph;
import com.tctest.performance.sampledata.OrganicObjectGraphManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class OrganicObjectGraphServlet extends HttpServlet {

  private static final String CHANGES            = "changes";          // # of changes to randomly selected graph
  private static final String CREATE_GRAPH       = "create";           // 0 = no, > 0 = # of nodes
  private static final String FINISHED           = "finished";         // test complete, serialize data
  private static final String ERROR              = "error";
  private static final String HOST_SYSPROP       = "__TEST_HOSTNAME__"; // system property uniquely identifies a host
  private static final int    DEFAULT_GRAPH_SIZE = 10;
  private final Random        random             = new Random(0);

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    String envKey = System.getProperty(HOST_SYSPROP);
    if (envKey == null) throw new IOException("System Property \"" + HOST_SYSPROP + "\" Not Set");

    if (request.getParameter(FINISHED) != null) {
      try {
        writeData(session);
      } catch (Exception e) {
        throw new IOException("Unable to write graph data to filesystem.");
      }
      out.println("Wrote Data");
      return;
    }

    int graphCount = graphCount(session);
    int changes = 0;
    if (request.getParameter(CHANGES) == null) {
      out.println(ERROR);
      return;
    } else {
      changes = Integer.parseInt(request.getParameter(CHANGES));
    }
    int createNewGraph = 0;
    if (request.getParameter(CREATE_GRAPH) == null) {
      out.println(ERROR);
      return;
    } else {
      createNewGraph = Integer.parseInt(request.getParameter(CREATE_GRAPH));
    }

    if (createNewGraph > 0 || session.isNew()) {
      if (session.isNew()) createNewGraph = DEFAULT_GRAPH_SIZE;
      try {
        OrganicObjectGraph graph = OrganicObjectGraphManager.createOrganicGraph(createNewGraph, envKey);
        session.setAttribute("graph_" + graphCount, graph);
      } catch (Exception e) {
        e.printStackTrace();
        throw new IOException("Unable to create data graph");
      }
    }

    if (changes > 0) {
      int rand = getRandom(graphCount);
      OrganicObjectGraph graph = (OrganicObjectGraph) session.getAttribute("graph_" + rand);
      graph.mutateRandom(changes);
      session.setAttribute("graph_" + rand, graph); // put the session back (not needed by DSO)
    }

    out.println("Session Updated");
    out.flush();
    return;
  }

  private void writeData(HttpSession session) throws Exception {
    Enumeration names = session.getAttributeNames();
    String name;
    while (names.hasMoreElements()) {
      name = (String) names.nextElement();
      File file = new File("results" + File.separator + session.getId() + "_" + name + ".obj");
      System.out.println("Writing File: " + file.getAbsolutePath());
      OrganicObjectGraphManager.serializeGraph(((OrganicObjectGraph) session.getAttribute(name)), file);
    }
  }

  private int graphCount(HttpSession session) {
    Enumeration names = session.getAttributeNames();
    int count = 0;
    while (names.hasMoreElements()) {
      if (((String) names.nextElement()).startsWith("graph_")) count++;
    }
    return count;
  }

  private int getRandom(int bound) {
    return new Long(Math.round(Math.floor(bound * random.nextDouble()))).intValue();
  }
}
