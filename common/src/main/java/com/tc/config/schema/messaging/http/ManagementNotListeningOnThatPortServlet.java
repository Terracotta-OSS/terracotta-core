/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.config.schema.messaging.http;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ManagementNotListeningOnThatPortServlet extends HttpServlet {

  public static final String CONFIG_ATTRIBUTE = ConfigServlet.class.getName() + ".config";

  private volatile L2ConfigurationSetupManager configSetupManager;

  private static final TCLogger LOGGER = TCLogging.getLogger(ManagementNotListeningOnThatPortServlet.class);

  @Override
  public void init() {
    configSetupManager = (L2ConfigurationSetupManager) getServletContext().getAttribute(CONFIG_ATTRIBUTE);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    int managementPortNumber = this.configSetupManager.commonl2Config().managementPort().getIntValue();

    PrintWriter writer = response.getWriter();
//    if(request.getPathInfo().startsWith("/v1")) {
//      writer.println("v1 " + managementPortNumber);
//    } else if(request.getPathInfo().startsWith("/v2")) {


    if (LOGGER.isDebugEnabled()) {
      LOGGER.warn("Management requests are no longer served on the TC comm port - Please only use the management port, in your case : " + managementPortNumber);
    }

    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType("application/json");

    writer.println("{\"error\":\"Impossible to connect to the agent : wrong port !\"," +
          "\"details\":\"Since 4.2, the management rest agent is only available through the management port; it appears it is "+ managementPortNumber + " in your current setup. Please reconfigure your connection to use that port.\"," +
          "\"stackTrace\":\"\"}");
  }
}
