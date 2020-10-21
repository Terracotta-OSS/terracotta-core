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

import com.tc.classloader.ServiceLocator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.properties.TCPropertiesImpl;
import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.ConfigurationProvider;
import org.terracotta.configuration.ServerConfiguration;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.configuration.ConfigurationException;

public class ServerConfigurationManagerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String[] TEST_SERVER_NAMES = {"test-server-1", "test-server-2"};
  private static final int[] TEST_SERVER_PORTS = {9410, 9510};
  private static final int[] TEST_GROUP_PORTS = {9430, 9530};

  private volatile String currentServer;
  
  private ConfigurationProvider mockServers(int length) throws ConfigurationException {
    ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
    Configuration configuration = mock(Configuration.class);
    when(configurationProvider.getConfiguration()).thenReturn(configuration);
    List<ServerConfiguration> servers = new ArrayList<>();
    
    for (int x=0;x<length;x++) {
      servers.add(createServer(x, 100));
    }
    
    when(configuration.getServerConfigurations()).thenReturn(servers);

    when(configuration.getServerConfiguration()).then(a->{
      Object value = currentServer;
      if (value == null) {
        return servers.size() == 1 ? servers.get(0) : null;
      }
      for (int x=0;x<length;x++) {
        if (TEST_SERVER_NAMES[x] == value) {
          return servers.get(x);
        }
      }
      return null;
    });
    return configurationProvider;
  }

  @Test
  public void testSingleServerValidConfiguration() throws Exception {
    ConfigurationProvider configurationProvider = mockServers(1);
    String[] processArgs = new String[] {"arg1", "arg2"};
    currentServer = TEST_SERVER_NAMES[0];
    ServerConfigurationManager manager = new ServerConfigurationManager(configurationProvider,
                                                                        new ServiceLocator(Thread.currentThread().getContextClassLoader()),
                                                                        processArgs);

    manager.initialize();
    GroupConfiguration groupConfiguration = manager.getGroupConfiguration();
    assertThat(groupConfiguration.getCurrentNode(), is(createNode(0)));
    System.out.println(groupConfiguration.getNodes());
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
  }

  @Test
  public void testSingleServerWithServerNameNull() throws Exception {
    ConfigurationProvider configurationProvider = mockServers(1);
    
    boolean consistentStartup = true;
    String[] processArgs = new String[] {"arg1", "arg2"};
    currentServer = null;
    ServerConfigurationManager manager = new ServerConfigurationManager(configurationProvider,
                                                                        new ServiceLocator(Thread.currentThread().getContextClassLoader()),
                                                                        processArgs);

    manager.initialize();
    ServerConfiguration serverConfiguration = manager.getServerConfiguration();
    assertThat(serverConfiguration.getName(), is(TEST_SERVER_NAMES[0]));
  }

  @Test
  public void testSingleServerWithInvalidServerName() throws Exception {
    ConfigurationProvider configurationProvider = mockServers(1);

    String[] processArgs = new String[] {"arg1", "arg2"};
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("unable to determine server configuration");
    currentServer = "not-a-server-name";
    new ServerConfigurationManager(configurationProvider,
                                                                        new ServiceLocator(Thread.currentThread().getContextClassLoader()),
                                                                        processArgs).initialize();
  }

  @Test
  public void testMultipleServersValidConfiguration() throws Exception {
    ConfigurationProvider configurationProvider = mockServers(2);

    int currentServerIndex = 1;

    String[] processArgs = new String[] {"arg1", "arg2"};
    currentServer = TEST_SERVER_NAMES[currentServerIndex];
    ServerConfigurationManager manager = new ServerConfigurationManager(configurationProvider,
                                                                        new ServiceLocator(Thread.currentThread().getContextClassLoader()),
                                                                        processArgs);

    manager.initialize();
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
  }

  @Test
  public void testMultipleServersWithServerNameNull() throws Exception {
    ConfigurationProvider configurationProvider = mockServers(2);

    String[] processArgs = new String[] {"arg1", "arg2"};
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("unable to determine server configuration");
    currentServer = null;
    new ServerConfigurationManager(configurationProvider,
                                                                        new ServiceLocator(Thread.currentThread().getContextClassLoader()),
                                                                        processArgs).initialize();
  }

  @Test
  public void testMultipleServersWithInvalidServerName() throws Exception {
    ConfigurationProvider configurationProvider = mockServers(2);

    String[] processArgs = new String[] {"arg1", "arg2"};
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("unable to determine server configuration");

    currentServer = "not-a-server-name";
    new ServerConfigurationManager(configurationProvider,
                                   new ServiceLocator(Thread.currentThread().getContextClassLoader()),
                                   processArgs).initialize();
  }

  @Test
  public void testTcProperties() throws Exception {
    ConfigurationProvider configurationProvider = mockServers(2);
    Configuration configuration = configurationProvider.getConfiguration();

    int currentServerIndex = 1;

    Properties tcProperties = new Properties();
    String testKey = "some-tc-property-key";
    String testValue = "value";
    tcProperties.setProperty(testKey, testValue);
    when(configuration.getTcProperties()).thenReturn(tcProperties);

    List<ServerConfiguration> servers = new ArrayList<>();
    servers.add(createServer(0, 100));
    servers.add(createServer(1, 100));
    when(configuration.getServerConfigurations()).thenReturn(servers);

    String[] processArgs = new String[] {"arg1", "arg2"};
    currentServer = TEST_SERVER_NAMES[currentServerIndex];
    new ServerConfigurationManager(configurationProvider,
                                                                        new ServiceLocator(Thread.currentThread().getContextClassLoader()),
                                                                        processArgs).initialize();

    assertThat(TCPropertiesImpl.getProperties().getProperty(testKey), is(testValue));
  }


  private static ServerConfiguration createServer(int serverIndex, int reconnectWindow) {
    ServerConfiguration config = mock(ServerConfiguration.class);
    when(config.getName()).thenReturn(TEST_SERVER_NAMES[serverIndex]);
    when(config.getHost()).thenReturn("localhost");
    InetSocketAddress tsaPort = InetSocketAddress.createUnresolved(TCSocketAddress.WILDCARD_IP, TEST_SERVER_PORTS[serverIndex]);
    when(config.getTsaPort()).thenReturn(tsaPort);
    InetSocketAddress groupPort = InetSocketAddress.createUnresolved(TCSocketAddress.WILDCARD_IP, TEST_GROUP_PORTS[serverIndex]);
    when(config.getGroupPort()).thenReturn(groupPort);
    when(config.getClientReconnectWindow()).thenReturn(reconnectWindow);
    return config;
  }

  private static Node createNode(int serverIndex) {
    return new Node("localhost", TEST_SERVER_PORTS[serverIndex], TEST_GROUP_PORTS[serverIndex]);
  }
}