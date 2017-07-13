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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;


public class ConfigBuilder {

  public static final int DEFAULT_CLIENT_RECONNECT_WINDOW_TIME = 120;

  public static ConfigBuilder buildStartPort(ContextualLogger logger, int startPort) {
    return new ConfigBuilder(logger, startPort);
  }
  
  private final ContextualLogger logger;
  private final int startPort;
  private final List<String> serverNames;
  private final List<Integer> tsaServerPorts;
  private final List<Integer> tsaGroupPorts;
  private String xmlNamespaceFragment;
  private String serviceXMLSnippet;
  private String entityXMLSnippet;
  private int clientReconnectWindowTime; // in secs
  
  private ConfigBuilder(ContextualLogger logger, int startPort) {
    this.logger = logger;
    this.startPort = startPort;
    this.serverNames = new Vector<String>();
    this.tsaServerPorts = new Vector<Integer>();
    this.tsaGroupPorts = new Vector<Integer>();
  }

  public ConfigBuilder addServer(String serverName) {
    Assert.assertFalse(this.serverNames.contains(serverName));
    this.serverNames.add(serverName);
    this.logger.output("Added " + serverName);
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

  public ConfigBuilder setEntitySnippet(String entityFragment) {
    this.entityXMLSnippet = entityFragment;
    return this;
  }

  public ConfigBuilder setClientReconnectWindowTime(final int clientReconnectWindowTime) {
    this.clientReconnectWindowTime = clientReconnectWindowTime;
    return this;
  }

  public String buildConfig() {
    // Now, on to the common case of the config builder.
    String namespaces = ((null != this.xmlNamespaceFragment) ? this.xmlNamespaceFragment : "");
    
    String pre = 
          "<tc-config xmlns=\"http://www.terracotta.org/config\" " + namespaces + ">\n"
        + "  <plugins>\n";
    String services = ((null != this.serviceXMLSnippet) ? this.serviceXMLSnippet : "");
    String postservices =
          "  </plugins>\n"
        + "  <entities>\n";
    
    String entities = (null != this.entityXMLSnippet) ? this.entityXMLSnippet : "";
    
    String postentities = "  </entities>\n" + 
                          "  <tc-properties/>\n"
        + "  <servers secure=\"false\">\n";
    int nextPort = this.startPort;
    String servers = "";
    for (String serverName : this.serverNames) {
      int port = nextPort;
      int groupPort = nextPort + 1;
      this.tsaServerPorts.add(port);
      this.tsaGroupPorts.add(groupPort);
      nextPort += 2;
      String oneServer = 
            "    <server host=\"localhost\" name=\"" + serverName + "\">\n"
          + "      <logs>terracotta-kit-test/" + serverName + "/logs</logs>\n"
          + "      <tsa-port>" + port + "</tsa-port>\n"
          + "      <tsa-group-port>" + groupPort + "</tsa-group-port>\n"
          + "    </server>\n";
      servers += oneServer;
    }
    String post =
        "    <client-reconnect-window>" + this.clientReconnectWindowTime + "</client-reconnect-window>\n"
        + "  </servers>\n"
        + "</tc-config>\n";
    return pre + services + postservices + entities + postentities + servers + post;
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
    this.logger.output("Stripe URI: " + connectUri);
    return connectUri;
  }

  public ClusterInfo getClusterInfo() {
    Map<String, ServerInfo> servers = new HashMap<>();

    for(int i = 0; i < tsaServerPorts.size(); i++) {
      String serverName = serverNames.get(i);
      servers.put(serverName, new ServerInfo(serverName, tsaServerPorts.get(i), tsaGroupPorts.get(i)));
    }

    return new ClusterInfo(servers);
  }

}
