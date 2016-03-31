/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;

import java.util.List;
import java.util.Vector;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ILogger;


public class ConfigBuilder {
  public static ConfigBuilder buildStartPort(ILogger logger, int startPort) {
    return new ConfigBuilder(logger, startPort);
  }
  
  private final ILogger logger;
  private final int startPort;
  private final List<String> serverNames;
  private String xmlNamespaceFragment;
  private String serviceXMLSnippet;
  private boolean isRestartable;
  
  private ConfigBuilder(ILogger logger, int startPort) {
    this.logger = logger;
    this.startPort = startPort;
    this.serverNames = new Vector<String>();
  }

  public ConfigBuilder addServer(String serverName) {
    Assert.assertFalse(this.serverNames.contains(serverName));
    this.serverNames.add(serverName);
    this.logger.log("Added " + serverName);
    return this;
  }

  public ConfigBuilder setNamespaceSnippet(String namespaceFragment) {
    this.xmlNamespaceFragment = namespaceFragment;
    return this;
  }

  public ConfigBuilder setServiceSnippet(String serviceFragment) {
    this.serviceXMLSnippet = serviceFragment;
    return this;
  }

  public ConfigBuilder setRestartable() {
    this.isRestartable = true;
    this.logger.log("Config set restartable");
    return this;
  }

  public String buildConfig() {
    String namespaces = (null != this.xmlNamespaceFragment) ? this.xmlNamespaceFragment : "";
    String pre = 
          "<tc-config xmlns=\"http://www.terracotta.org/config\"" + namespaces + ">\n"
        + "  <services>\n";
    String services = (null != this.serviceXMLSnippet) ? this.serviceXMLSnippet : "";
    String mid =
          "  </services>\n"
        + "  <tc-properties/>\n"
        + "  <servers secure=\"false\">\n";
    int nextPort = this.startPort;
    String servers = "";
    for (String serverName : this.serverNames) {
      int port = nextPort;
      int groupPort = nextPort + 1;
      nextPort += 2;
      String oneServer = 
            "    <server host=\"localhost\" name=\"" + serverName + "\">\n"
          + "      <data>terracotta-kit-test/" + serverName + "/data</data>\n"
          + "      <logs>terracotta-kit-test/" + serverName + "/logs</logs>\n"
          + "      <tsa-port>" + port + "</tsa-port>\n"
          + "      <tsa-group-port>" + groupPort + "</tsa-group-port>\n"
          + "    </server>\n";
      servers += oneServer;
    }
    String restartString = (this.isRestartable ? "<restartable enabled=\"true\"/>\n" : "");
    String post = 
          "    <client-reconnect-window>120</client-reconnect-window>\n"
        + restartString
        + "  </servers>\n"
        + "</tc-config>\n";
    return pre + services + mid + servers + post;
  }

  public String buildUri() {
    String connectUri = "terracotta://";
    boolean needsComma = false;
    int nextPort = this.startPort;
    for (int i = 0; i < this.serverNames.size(); ++i) {
      int port = nextPort;
      nextPort += 2;
      if (needsComma) {
        connectUri += ",";
      }
      connectUri += "localhost:" + port;
      needsComma = true;
    }
    this.logger.log("Stripe URI: " + connectUri);
    return connectUri;
  }
}
