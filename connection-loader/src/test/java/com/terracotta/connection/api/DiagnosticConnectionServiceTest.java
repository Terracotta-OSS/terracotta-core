package com.terracotta.connection.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.connection.Connection;

import com.terracotta.connection.EndpointConnector;
import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientFactory;

import java.net.URI;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DiagnosticConnectionServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void connect() throws Exception {
    TerracottaInternalClientFactory clientFactoryMock = mock(TerracottaInternalClientFactory.class);
    when(clientFactoryMock.createL1Client(any())).thenReturn(mock(TerracottaInternalClient.class));
    DiagnosticConnectionService diagnosticConnectionService =
        new DiagnosticConnectionService(mock(EndpointConnector.class), clientFactoryMock);
    Connection connection =
        diagnosticConnectionService.connect(URI.create("diagnostic://localhost:9410"), new Properties());
    assertThat(connection, notNullValue());
  }

  @Test
  public void connectWithNonDiagnosticScheme() throws Exception {
    TerracottaInternalClientFactory clientFactoryMock = mock(TerracottaInternalClientFactory.class);
    when(clientFactoryMock.createL1Client(any())).thenReturn(mock(TerracottaInternalClient.class));
    DiagnosticConnectionService diagnosticConnectionService =
        new DiagnosticConnectionService(mock(EndpointConnector.class), clientFactoryMock);
    expectedException.expect(IllegalArgumentException.class);
    diagnosticConnectionService.connect(URI.create("non-diagnostic://localhost:9410"), new Properties());
  }
}