/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Configuration.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.config.BindPort;
import org.terracotta.config.Property;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcProperties;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.properties.TCPropertiesImpl;
import com.terracotta.config.Configuration;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerConfigurationManagerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String[] TEST_SERVER_NAMES = {"test-server-1", "test-server-2"};
  private static final int[] TEST_SERVER_PORTS = {9410, 9510};
  private static final int[] TEST_GROUP_PORTS = {9430, 9530};

  @Test
  public void testSingleServerValidConfiguration() throws Exception {
    Configuration configuration = mock(Configuration.class);

    TcConfig tcConfig = new TcConfig();
    when(configuration.getPlatformConfiguration()).thenReturn(tcConfig);

    Servers servers = new Servers();
    servers.getServer().add(createServer(0));
    servers.setClientReconnectWindow(100);
    tcConfig.setServers(servers);

    boolean consistentStartup = true;
    String[] processArgs = new String[] {"arg1", "arg2"};
    ServerConfigurationManager manager = new ServerConfigurationManager(TEST_SERVER_NAMES[0],
                                                                        configuration,
                                                                        consistentStartup,
                                                                        false,
                                                                        Thread.currentThread().getContextClassLoader(),
                                                                        processArgs);

    GroupConfiguration groupConfiguration = manager.getGroupConfiguration();
    assertThat(groupConfiguration.getCurrentNode(), is(createNode(0)));
    assertThat(groupConfiguration.getNodes(),
               containsInAnyOrder(
                   createNode(0)
               )
    );
    assertThat(groupConfiguration.getMembers(), arrayContainingInAnyOrder(TEST_SERVER_NAMES[0]));

    ServerConfiguration serverConfiguration = manager.getServerConfiguration();
    assertThat(serverConfiguration.getName(), is(TEST_SERVER_NAMES[0]));

    assertThat(manager.getServiceLocator(), notNullValue());
    assertThat(manager.getProcessArguments(), arrayContainingInAnyOrder(processArgs));
    assertThat(manager.consistentStartup(), is(consistentStartup));
  }

  @Test
  public void testSingleServerWithServerNameNull() throws Exception {
    Configuration configuration = mock(Configuration.class);

    TcConfig tcConfig = new TcConfig();
    when(configuration.getPlatformConfiguration()).thenReturn(tcConfig);

    Servers servers = new Servers();
    servers.getServer().add(createServer(0));
    servers.setClientReconnectWindow(100);
    tcConfig.setServers(servers);

    boolean consistentStartup = true;
    String[] processArgs = new String[] {"arg1", "arg2"};
    ServerConfigurationManager manager = new ServerConfigurationManager(null,
                                                                        configuration,
                                                                        consistentStartup,
                                                                        false,
                                                                        Thread.currentThread().getContextClassLoader(),
                                                                        processArgs);

    ServerConfiguration serverConfiguration = manager.getServerConfiguration();
    assertThat(serverConfiguration.getName(), is(TEST_SERVER_NAMES[0]));
  }

  @Test
  public void testSingleServerWithInvalidServerName() throws Exception {
    Configuration configuration = mock(Configuration.class);

    TcConfig tcConfig = new TcConfig();
    when(configuration.getPlatformConfiguration()).thenReturn(tcConfig);

    Servers servers = new Servers();
    servers.getServer().add(createServer(0));
    servers.setClientReconnectWindow(100);
    tcConfig.setServers(servers);

    boolean consistentStartup = true;
    String[] processArgs = new String[] {"arg1", "arg2"};
    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("does not exist in the specified configuration");

    new ServerConfigurationManager("not-a-server-name",
                                                                        configuration,
                                                                        consistentStartup,
                                                                        false,
                                                                        Thread.currentThread().getContextClassLoader(),
                                                                        processArgs);
  }

  @Test
  public void testMultipleServersValidConfiguration() throws Exception {
    Configuration configuration = mock(Configuration.class);
    int currentServerIndex = 1;

    TcConfig tcConfig = new TcConfig();
    when(configuration.getPlatformConfiguration()).thenReturn(tcConfig);

    Servers servers = new Servers();
    servers.getServer().add(createServer(0));
    servers.getServer().add(createServer(1));
    servers.setClientReconnectWindow(100);
    tcConfig.setServers(servers);

    boolean consistentStartup = false;
    String[] processArgs = new String[] {"arg1", "arg2"};
    ServerConfigurationManager manager = new ServerConfigurationManager(TEST_SERVER_NAMES[currentServerIndex],
                                                                        configuration,
                                                                        consistentStartup,
                                                                        false,
                                                                        Thread.currentThread().getContextClassLoader(),
                                                                        processArgs);

    GroupConfiguration groupConfiguration = manager.getGroupConfiguration();
    assertThat(groupConfiguration.getCurrentNode(), is(createNode(currentServerIndex)));
    assertThat(groupConfiguration.getNodes(),
               containsInAnyOrder(
                 createNode(0), createNode(1)
               )
    );
    assertThat(groupConfiguration.getMembers(), arrayContainingInAnyOrder(TEST_SERVER_NAMES));

    ServerConfiguration serverConfiguration = manager.getServerConfiguration();
    assertThat(serverConfiguration.getName(), is(TEST_SERVER_NAMES[currentServerIndex]));

    assertThat(manager.getServiceLocator(), notNullValue());
    assertThat(manager.getProcessArguments(), arrayContainingInAnyOrder(processArgs));
    assertThat(manager.consistentStartup(), is(consistentStartup));
  }

  @Test
  public void testMultipleServersWithServerNameNull() throws Exception {
    Configuration configuration = mock(Configuration.class);

    TcConfig tcConfig = new TcConfig();
    when(configuration.getPlatformConfiguration()).thenReturn(tcConfig);

    Servers servers = new Servers();
    servers.getServer().add(createServer(0));
    servers.getServer().add(createServer(1));
    servers.setClientReconnectWindow(100);
    tcConfig.setServers(servers);

    String[] processArgs = new String[] {"arg1", "arg2"};
    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("The script can not automatically choose between the following server names");
    ServerConfigurationManager manager = new ServerConfigurationManager(null,
                                                                        configuration,
                                                                        true,
                                                                        false,
                                                                        Thread.currentThread().getContextClassLoader(),
                                                                        processArgs);
  }

  @Test
  public void testMultipleServersWithInvalidServerName() throws Exception {
    Configuration configuration = mock(Configuration.class);

    TcConfig tcConfig = new TcConfig();
    when(configuration.getPlatformConfiguration()).thenReturn(tcConfig);

    Servers servers = new Servers();
    servers.getServer().add(createServer(0));
    servers.getServer().add(createServer(1));
    servers.setClientReconnectWindow(100);
    tcConfig.setServers(servers);

    String[] processArgs = new String[] {"arg1", "arg2"};
    expectedException.expect(ConfigurationSetupException.class);
    expectedException.expectMessage("does not exist in the specified configuration");

    new ServerConfigurationManager("not-a-server-name",
                                   configuration,
                                   true,
                                   false,
                                   Thread.currentThread().getContextClassLoader(),
                                   processArgs);
  }

  @Test
  public void testTcProperties() throws Exception {
    Configuration configuration = mock(Configuration.class);
    int currentServerIndex = 1;

    TcConfig tcConfig = new TcConfig();
    when(configuration.getPlatformConfiguration()).thenReturn(tcConfig);
    TcProperties tcProperties = new TcProperties();
    Property tcProperty = new Property();
    String testKey = "some-tc-property-key";
    String testValue = "value";
    tcProperty.setName(testKey);
    tcProperty.setValue(testValue);
    tcProperties.getProperty().add(tcProperty);
    tcConfig.setTcProperties(tcProperties);

    Servers servers = new Servers();
    servers.getServer().add(createServer(0));
    servers.getServer().add(createServer(1));
    servers.setClientReconnectWindow(100);
    tcConfig.setServers(servers);

    boolean consistentStartup = false;
    String[] processArgs = new String[] {"arg1", "arg2"};
    ServerConfigurationManager manager = new ServerConfigurationManager(TEST_SERVER_NAMES[currentServerIndex],
                                                                        configuration,
                                                                        consistentStartup,
                                                                        false,
                                                                        Thread.currentThread().getContextClassLoader(),
                                                                        processArgs);

    assertThat(TCPropertiesImpl.getProperties().getProperty(testKey), is(testValue));
  }


  private static Server createServer(int serverIndex) {
    Server server = new Server();
    server.setName(TEST_SERVER_NAMES[serverIndex]);
    server.setBind("localhost");
    server.setHost("localhost");
    BindPort tsaPortBind = new BindPort();
    tsaPortBind.setBind(TCSocketAddress.WILDCARD_IP);
    tsaPortBind.setValue(TEST_SERVER_PORTS[serverIndex]);
    server.setTsaPort(tsaPortBind);
    BindPort groupPortBind = new BindPort();
    groupPortBind.setBind(TCSocketAddress.WILDCARD_IP);
    groupPortBind.setValue(TEST_GROUP_PORTS[serverIndex]);
    server.setTsaGroupPort(groupPortBind);

    return server;
  }

  private static Node createNode(int serverIndex) {
    return new Node("localhost", TEST_SERVER_PORTS[serverIndex], TEST_GROUP_PORTS[serverIndex]);
  }
}