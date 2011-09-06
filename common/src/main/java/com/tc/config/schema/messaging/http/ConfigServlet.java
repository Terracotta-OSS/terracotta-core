/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.ConfigurationModel;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConfigServlet extends HttpServlet {

  public static final String                      CONFIG_ATTRIBUTE = ConfigServlet.class.getName() + ".config";

  private volatile L2ConfigurationSetupManager configSetupManager;

  public void init() {
    configSetupManager = (L2ConfigurationSetupManager) getServletContext().getAttribute(CONFIG_ATTRIBUTE);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Map params = request.getParameterMap();

    if (params.size() == 0) {
      OutputStream out = response.getOutputStream();
      IOUtils.copy(this.configSetupManager.effectiveConfigFile(), out);
    } else {
      String query = request.getParameter("query");

      if ("mode".equals(query)) {
        OutputStream out = response.getOutputStream();
        ConfigurationModel configModel = configSetupManager.systemConfig().configurationModel();
        IOUtils.copy(IOUtils.toInputStream(configModel.toString()), out);
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        writer.println("request not understood");
      }
    }

    response.flushBuffer();
  }
}
