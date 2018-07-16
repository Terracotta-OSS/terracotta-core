package com.terracotta.connection.api;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;

import com.tc.object.ClientBuilderFactory;
import com.terracotta.connection.EndpointConnector;
import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientFactory;
import com.terracotta.connection.client.TerracottaClientConfigParams;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    when(clientFactoryMock.createL1Client(any())).thenReturn(mock(TerracottaInternalClient.class));
    connectionService = new AbstractConnectionService(TEST_SCHEME, endpointConnectorMock, clientFactoryMock) {};
  }

  @Test
  public void connect() throws Exception {
    Connection connection = connectionService.connect(URI.create(TEST_SCHEME + "://localhost:4000"), new Properties());
    assertThat(connection, notNullValue());
  }

  @Test
  public void verifyProperties() throws Exception {
    ArgumentCaptor<TerracottaClientConfigParams> argumentCaptor =
        ArgumentCaptor.forClass(TerracottaClientConfigParams.class);
    final String testProperty = "testProperty";
    final String testPropertyValue = "testPropertyValue";
    Properties connectionProperties = new Properties();
    connectionProperties.put(testProperty, testPropertyValue);
    connectionService.connect(URI.create(TEST_SCHEME + "://localhost:4000"), connectionProperties);
    verify(clientFactoryMock).createL1Client(argumentCaptor.capture());
    TerracottaClientConfigParams terracottaClientConfigParams = argumentCaptor.getValue();
    Properties genericProperties = terracottaClientConfigParams.getGenericProperties();

    assertThat(genericProperties.get(ClientBuilderFactory.CLIENT_BUILDER_TYPE), notNullValue());
    assertThat(genericProperties.get(testProperty), is(testPropertyValue));
  }

  @Test
  public void verifyPropertiesWithIterableBasedConnect() throws Exception {
    ArgumentCaptor<TerracottaClientConfigParams> argumentCaptor = ArgumentCaptor.forClass(TerracottaClientConfigParams.class);
    final String testProperty = "testProperty";
    final String testPropertyValue = "testPropertyValue";
    Properties connectionProperties = new Properties();
    connectionProperties.put(testProperty, testPropertyValue);
    InetSocketAddress server = InetSocketAddress.createUnresolved("localhost", 4000);
    connectionService.connect(Collections.singletonList(server), connectionProperties);
    verify(clientFactoryMock).createL1Client(argumentCaptor.capture());

    TerracottaClientConfigParams terracottaClientConfigParams = argumentCaptor.getValue();
    Properties genericProperties = terracottaClientConfigParams.getGenericProperties();
    assertThat(genericProperties.get(ClientBuilderFactory.CLIENT_BUILDER_TYPE), notNullValue());
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