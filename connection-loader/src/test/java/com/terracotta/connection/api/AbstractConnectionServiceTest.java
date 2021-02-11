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
package com.terracotta.connection.api;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;

import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.entity.EndpointConnector;

public class AbstractConnectionServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final String TEST_SCHEME = "terracotta";
  private TerracottaInternalClientFactory clientFactoryMock;
  private ConnectionService connectionService;

  @Before
  public void setUp() {
    clientFactoryMock = mock(TerracottaInternalClientFactory.class);
    EndpointConnector endpointConnectorMock = mock(EndpointConnector.class);
    when(clientFactoryMock.createL1Client(anyString(), any(), any())).thenReturn(mock(TerracottaInternalClient.class));
    connectionService = new AbstractConnectionService(TEST_SCHEME, endpointConnectorMock, clientFactoryMock) {};
  }

  @Test
  public void connect() throws Exception {
    Connection connection = connectionService.connect(URI.create(TEST_SCHEME + "://localhost:4000"), new Properties());
    assertThat(connection, notNullValue());
  }

  @Test
  public void verifyProperties() throws Exception {
    ArgumentCaptor<Properties> propertiesArgumentCaptor = forClass(Properties.class);
    final String testProperty = "testProperty";
    final String testPropertyValue = "testPropertyValue";
    Properties connectionProperties = new Properties();
    connectionProperties.put(testProperty, testPropertyValue);
    connectionService.connect(URI.create(TEST_SCHEME + "://localhost:4000"), connectionProperties);
    verify(clientFactoryMock).createL1Client(anyString(), forClass(Iterable.class).capture(), propertiesArgumentCaptor.capture());
    Properties genericProperties = propertiesArgumentCaptor.getValue();

    assertThat(genericProperties.get(testProperty), is(testPropertyValue));
  }

  @Test
  public void verifyPropertiesWithIterableBasedConnect() throws Exception {
    ArgumentCaptor<Properties> propertiesArgumentCaptor = forClass(Properties.class);
    final String testProperty = "testProperty";
    final String testPropertyValue = "testPropertyValue";
    Properties connectionProperties = new Properties();
    connectionProperties.put(testProperty, testPropertyValue);
    InetSocketAddress server = InetSocketAddress.createUnresolved("localhost", 4000);
    connectionService.connect(Collections.singletonList(server), connectionProperties);
    verify(clientFactoryMock).createL1Client(anyString(), forClass(Iterable.class).capture(), propertiesArgumentCaptor.capture());

    Properties genericProperties = propertiesArgumentCaptor.getValue();
    assertThat(genericProperties.get(testProperty), is(testPropertyValue));
  }

  @Test
  public void connectWithUnknownScheme() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown URI");
    connectionService.connect(URI.create("non-terracotta://localhost:4000"), new Properties());
  }

  @Test
  public void connectWithUnknownTypeWithIterableBasedConnect() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unknown connectionType");
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TYPE, "unknown");
    InetSocketAddress server = InetSocketAddress.createUnresolved("localhost", 4000);
    connectionService.connect(Collections.singletonList(server), properties);
  }

  @Test
  public void connectWithMalformedPort() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unable to parse uri");
    connectionService.connect(URI.create(TEST_SCHEME + "://localhost:4000,localhost:dd45"), new Properties());
  }
}