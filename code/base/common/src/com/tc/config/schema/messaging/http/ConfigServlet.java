/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConfigServlet extends HttpServlet {

  public static final String                      CONFIG_ATTRIBUTE = ConfigServlet.class.getName() + ".config";

  private volatile L2TVSConfigurationSetupManager configSetupManager;

  public void init() throws ServletException {
    configSetupManager = (L2TVSConfigurationSetupManager) getServletContext().getAttribute(CONFIG_ATTRIBUTE);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Map params = request.getParameterMap();

    if (params.size() == 0) {
      OutputStream out = response.getOutputStream();
      IOUtils.copy(this.configSetupManager.rawConfigFile(), out);
    } else {
      String query = request.getParameter("query");

      if ("mode".equals(query)) {
        OutputStream out = response.getOutputStream();
        ConfigItem configModel = configSetupManager.systemConfig().configurationModel();
        Object configObject = configModel.getObject();
        IOUtils.copy(IOUtils.toInputStream(configObject.toString()), out);
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        writer.println("request not understood");
      }
    }

    response.flushBuffer();
  }
}
