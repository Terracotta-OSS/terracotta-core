/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.testing.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

public class TcConfigBuilder {
  private final Function<String, String> logPath;
  private final Supplier<String> serviceFragment;
  private final List<String> serverNames;
  private final List<Integer> serverPorts;
  private final List<Integer> serverGroupPorts;
  private final String namespaceFragment;
  private final int clientReconnectWindow;
  private final int voterCount;
  private final Properties tcProperties = new Properties();
  
  public TcConfigBuilder(Path stripePath, List<String> serverNames, List<Integer> serverPorts, List<Integer> serverGroupPorts,
                         Properties tcProperties, String namespaceFragment, String serviceFragment, int clientReconnectWindow, int voterCount) {
    this(name->stripePath.resolve(name).resolve("logs").toString(), ()->serviceFragment == null ? "" : serviceFragment, serverNames, serverPorts, serverGroupPorts, tcProperties, namespaceFragment, clientReconnectWindow, voterCount);
  }

  public TcConfigBuilder(Function<String, String> serverLogs, Supplier<String> serviceFragment, List<String> serverNames, List<Integer> serverPorts, List<Integer> serverGroupPorts,
                         Properties tcProperties, String namespaceFragment, int clientReconnectWindow, int voterCount) {
    this.logPath = serverLogs;
    this.serverNames = serverNames;
    this.serverPorts = serverPorts;
    this.serverGroupPorts = serverGroupPorts;
    this.tcProperties.putAll(tcProperties);
    this.namespaceFragment = namespaceFragment;
    this.serviceFragment = serviceFragment;
    this.clientReconnectWindow = clientReconnectWindow;
    this.voterCount = voterCount;
  }
  
  public String build() {
    String namespaces = ((null != namespaceFragment) ? namespaceFragment : "");

    String pre =
        "<tc-config xmlns=\"http://www.terracotta.org/config\" " + namespaces + ">\n"
            + "  <plugins>\n";
    String services = this.serviceFragment.get();
    String postservices =
        "  </plugins>\n" +
            "  <tc-properties>\n";
    StringBuilder properties = new StringBuilder();
    for (Map.Entry<Object, Object> entry : tcProperties.entrySet()) {
      properties.append("    <property name=\"").append(entry.getKey()).append("\" value=\"").append(entry.getValue()).append("\"/>\n");
    }
    String postProperties = "  </tc-properties>\n"
        + "  <servers>\n";
    StringBuilder servers = new StringBuilder();
    for (int i = 0; i < serverNames.size(); i++) {
      String serverName = serverNames.get(i);
      Integer port = serverPorts.get(i);
      Integer groupPort = serverGroupPorts.get(i);
      String oneServer =
          "    <server host=\"localhost\" name=\"" + serverName + "\">\n"
              + "      <logs>" + logPath.apply(serverName) + "</logs>\n"
              + "      <tsa-port>" + port + "</tsa-port>\n"
              + "      <tsa-group-port>" + groupPort + "</tsa-group-port>\n"
              + "    </server>\n";
      servers.append(oneServer);
    }
    String post =
        "    <client-reconnect-window>" + clientReconnectWindow + "</client-reconnect-window>\n"
            + "  </servers>\n"
            + "  <failover-priority>\n"
            + (voterCount == ConfigConstants.DEFAULT_VOTER_COUNT ?
            "    <availability/>\n" :
            "    <consistency>\n" +
                "      <voter count=\"" + voterCount + "\"/>\n" +
                "    </consistency>\n")
            + "  </failover-priority>\n"
            + "</tc-config>\n";
    return pre + services + postservices + properties + postProperties + servers + post;
  }
}
