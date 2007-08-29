/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;

import com.tctest.service.DirectoryMonitorMBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JBossSarServlet extends HttpServlet {
  private List       list = new ArrayList();
  private static Log log  = LogFactory.getLog(JBossSarServlet.class);

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      synchronized (list) {
        while (list.size() < 2) {
          list.wait();
        }
      }
      log.debug("shared list: " + list);
      MBeanServer server = MBeanServerLocator.locateJBoss();
      DirectoryMonitorMBean dmm = (DirectoryMonitorMBean) MBeanProxyExt
          .create(DirectoryMonitorMBean.class, "service.directory.monitor:service=Monitor", server);
      resp.getWriter().println("OK: " + list + ", " + dmm.getExtensionList());
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}
