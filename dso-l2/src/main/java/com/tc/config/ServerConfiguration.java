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
package com.tc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.BindPort;
import org.terracotta.config.Server;

import com.tc.net.TCSocketAddress;

import java.io.File;

public class ServerConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(ServerConfiguration.class);

  private static final String LOCALHOST = "localhost";

  private final BindPort tsaPort;
  private final BindPort tsaGroupPort;
  private final String host;
  private final String serverName;
  private final String logs;
  private volatile int clientReconnectWindow;

  ServerConfiguration(Server server, int clientReconnectWindow) {
    String bindAddress = server.getBind();
    this.host = server.getHost();
    if (this.host.equalsIgnoreCase(LOCALHOST)) {
      logger.info("The specified hostname \"" + this.host
                  + "\" may not work correctly if clients and operator console are connecting from other hosts. " + "Replace \""
                  + this.host + "\" with an appropriate hostname in configuration.");
    }

    this.serverName = server.getName();

    this.tsaPort = server.getTsaPort();
    if (TCSocketAddress.WILDCARD_IP.equals(this.tsaPort.getBind()) && !TCSocketAddress.WILDCARD_IP.equals(bindAddress)) {
      this.tsaPort.setBind(bindAddress);
    }

    this.tsaGroupPort = server.getTsaGroupPort();
    if (TCSocketAddress.WILDCARD_IP.equals(this.tsaGroupPort.getBind()) && !TCSocketAddress.WILDCARD_IP.equals(bindAddress)) {
      this.tsaGroupPort.setBind(bindAddress);
    }

    this.clientReconnectWindow = clientReconnectWindow;

    this.logs = server.getLogs();
  }

  public BindPort getTsaPort() {
    return this.tsaPort;
  }

  public BindPort getGroupPort() {
    return this.tsaGroupPort;
  }

  public String getHost() {
    return host;
  }

  public String getName() {
    return this.serverName;
  }

  public int getClientReconnectWindow() {
    return this.clientReconnectWindow;
  }

  public void setClientReconnectWindow(int value) {
    this.clientReconnectWindow = value;
  }

  public File getLogsLocation() {
    return new File(this.logs);
  }
}
