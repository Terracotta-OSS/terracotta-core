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
package com.tc.object.config.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.BindPort;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.TcConfig;

import com.tc.config.schema.ActiveServerGroupsConfigObject;
import com.tc.config.schema.setup.ConfigurationSetupException;

import java.io.File;


/**
 * The standard implementation of {@link L2Config}.
 */
public class L2ConfigObject implements L2Config {
  private static final Logger logger = LoggerFactory.getLogger(L2ConfigObject.class);
  private static final String LOCALHOST = "localhost";
  public static final short DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT = 10;
  public static final short DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT = 20;
  public static final short DEFAULT_MANAGEMENTPORT_OFFSET_FROM_TSAPORT = 30;
  public static final int MIN_PORTNUMBER = 0x0FFF;
  public static final int MAX_PORTNUMBER = 0xFFFF;
  public static final String DEFAULT_DATA_STORAGE_SIZE = "512m";

  private final BindPort tsaPort;
  private final BindPort tsaGroupPort;
  private final String host;
  private final String serverName;
  private final String bind;
  private final int clientReconnectWindow;
  private volatile boolean jmxEnabled;

  public L2ConfigObject(Server s, int clientReconnectWindow) {
    Server server = s;
    this.clientReconnectWindow = clientReconnectWindow;

    this.bind = server.getBind();
    this.host = server.getHost();
    if (this.host.equalsIgnoreCase(LOCALHOST)) {
      logger.warn("The specified hostname \"" + this.host
                  + "\" may not work correctly if clients and operator console are connecting from other hosts. " + "Replace \""
                  + this.host + "\" with an appropriate hostname in configuration.");
    }
    this.serverName = server.getName();
    this.tsaPort = server.getTsaPort();
    this.tsaGroupPort = server.getTsaGroupPort();
  }


  @Override
  public void setJmxEnabled(boolean b) {
    this.jmxEnabled = b;
  }

  @Override
  public boolean isJmxEnabled() {
    return jmxEnabled;
  }

  @Override
  public BindPort tsaPort() {
    return this.tsaPort;
  }

  @Override
  public BindPort tsaGroupPort() {
    return this.tsaGroupPort;
  }

  @Override
  public String host() {
    return host;
  }

  @Override
  public String serverName() {
    return this.serverName;
  }

  @Override
  public int clientReconnectWindow() {
    return this.clientReconnectWindow;
  }

  @Override
  public String bind() {
    return this.bind;
  }

  public static void initializeServers(TcConfig config, File directoryLoadedFrom) throws ConfigurationSetupException {

    Servers servers = config.getServers();
    ActiveServerGroupsConfigObject.initializeMirrorGroups(servers);
  }

  public static int computeJMXPortFromTSAPort(int tsaPort) {
    int tempJmxPort = tsaPort + DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT;
    return ((tempJmxPort <= MAX_PORTNUMBER) ? tempJmxPort : (tempJmxPort % MAX_PORTNUMBER) + MIN_PORTNUMBER);
  }

  public static int computeManagementPortFromTSAPort(int tsaPort) {
    int tempPort = tsaPort + DEFAULT_MANAGEMENTPORT_OFFSET_FROM_TSAPORT;
    return ((tempPort <= MAX_PORTNUMBER) ? tempPort : (tempPort % MAX_PORTNUMBER) + MIN_PORTNUMBER);
  }


  public static Server[] getServers(Servers servers) {
    return servers.getServer().toArray(new Server[servers.getServer().size()]);
  }

  @Override
  public Server getBean() {
    return null;
  }
}
