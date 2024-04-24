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

import org.junit.Test;
import org.terracotta.config.BindPort;
import org.terracotta.config.Server;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ServerConfigurationTest {

  private static final String SERVER_NAME = "server-1";
  private static final String LOCALHOST = "localhost";
  private static final String WILDCARD_IP = "0.0.0.0";
  private static final String LOGS = "logs";
  private static final int    TSA_PORT = 1000;
  private static final int    GROUP_PORT = 1100;

  @Test
  public void testConfiguration() {
    int reconnectWindow = 100;
    ServerConfigurationImpl serverConfiguration =
        new ServerConfigurationImpl(createServer(false), false, reconnectWindow);

    assertThat(serverConfiguration.getTsaPort().getHostName(), is(LOCALHOST));
    assertThat(serverConfiguration.getGroupPort().getHostName(), is(LOCALHOST));
    assertThat(serverConfiguration.getTsaPort().getPort(), is(TSA_PORT));
    assertThat(serverConfiguration.getGroupPort().getPort(), is(GROUP_PORT));
    assertThat(serverConfiguration.getName(), is(SERVER_NAME));
    assertThat(serverConfiguration.getHost(), is(LOCALHOST));
    assertThat(serverConfiguration.getLogsLocation(), is(new File(LOGS)));
    assertThat(serverConfiguration.getClientReconnectWindow(), is(reconnectWindow));
  }

  @Test
  public void testConfigurationWithWildcards() {
    ServerConfigurationImpl serverConfiguration =
        new ServerConfigurationImpl(createServer(true), false, 100);

    assertThat(serverConfiguration.getTsaPort().getHostName(), is(LOCALHOST));
    assertThat(serverConfiguration.getGroupPort().getHostName(), is(LOCALHOST));
  }

  private static Server createServer(boolean wildcards) {
    String bindAddress = wildcards ? WILDCARD_IP : LOCALHOST;

    Server server = new Server();
    server.setName(SERVER_NAME);
    server.setHost(LOCALHOST);
    server.setLogs(LOGS);
    server.setBind(LOCALHOST);

    BindPort tsaBind = new BindPort();
    tsaBind.setBind(bindAddress);
    tsaBind.setValue(TSA_PORT);

    BindPort groupBind = new BindPort();
    groupBind.setBind(bindAddress);
    groupBind.setValue(GROUP_PORT);

    server.setTsaPort(tsaBind);
    server.setTsaGroupPort(groupBind);

    return server;
  }
}