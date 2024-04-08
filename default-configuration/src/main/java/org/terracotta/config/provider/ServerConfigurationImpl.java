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
package org.terracotta.config.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.BindPort;
import org.terracotta.config.Server;

import org.terracotta.configuration.ServerConfiguration;

import java.io.File;
import java.net.InetSocketAddress;

public class ServerConfigurationImpl implements ServerConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(ServerConfigurationImpl.class);

  private static volatile boolean WARNED = false;

  private static final String LOCALHOST = "localhost";
  private static final String WILDCARD_IP    = "0.0.0.0";

  private final BindPort tsaPort;
  private final BindPort tsaGroupPort;
  private final String host;
  private final String serverName;
  private final File logs;
  private final int clientReconnectWindow;

  ServerConfigurationImpl(Server server, boolean consoleLogging, int clientReconnectWindow) {
    String bindAddress = server.getBind();
    this.host = server.getHost();
    if (this.host.equalsIgnoreCase(LOCALHOST) && !WARNED) {
      WARNED = true;
      logger.info("The specified hostname \"" + this.host
                  + "\" may not work correctly if clients and operator console are connecting from other hosts. " + "Replace \""
                  + this.host + "\" with an appropriate hostname in configuration.");
    }

    this.serverName = server.getName();

    this.tsaPort = server.getTsaPort();
    if (WILDCARD_IP.equals(this.tsaPort.getBind()) && !WILDCARD_IP.equals(bindAddress)) {
      this.tsaPort.setBind(bindAddress);
    }

    this.tsaGroupPort = server.getTsaGroupPort();
    if (WILDCARD_IP.equals(this.tsaGroupPort.getBind()) && !WILDCARD_IP.equals(bindAddress)) {
      this.tsaGroupPort.setBind(bindAddress);
    }

    this.clientReconnectWindow = clientReconnectWindow;

    this.logs = (consoleLogging) ? null : new File(server.getLogs());
  }

  @Override
  public InetSocketAddress getTsaPort() {
    return InetSocketAddress.createUnresolved(this.tsaPort.getBind(), this.tsaPort.getValue());
  }

  @Override
  public InetSocketAddress getGroupPort() {
    return InetSocketAddress.createUnresolved(this.tsaGroupPort.getBind(), this.tsaGroupPort.getValue());
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public String getName() {
    return this.serverName;
  }

  @Override
  public int getClientReconnectWindow() {
    return this.clientReconnectWindow;
  }

  @Override
  public File getLogsLocation() {
    return this.logs;
  }
}
