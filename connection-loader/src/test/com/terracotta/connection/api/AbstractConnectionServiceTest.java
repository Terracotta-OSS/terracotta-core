package com.terracotta.connection.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionService;

import com.terracotta.connection.client.TerracottaClientConfigParams;

import java.net.URI;
import java.util.Properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AbstractConnectionServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final Connection expectedConnection = mock(Connection.class);

  private final ConnectionService connectionService = new AbstractConnectionService() {
    @Override
    Connection internalConnect(TerracottaClientConfigParams configParams) throws ConnectionException {
      return expectedConnection;
    }

    @Override
    public boolean handlesURI(URI uri) {
      return true;
    }
  };

  @Test
  public void connect() throws Exception {
    assertThat(connectionService.connect(URI.create("terracotta://localhost:4000"), new Properties()),
               is(expectedConnection));
  }

  @Test
  public void connectWithMalformedPort() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unable to parse uri");
    connectionService.connect(URI.create("terracotta://localhost:4000,localhost:dd45"), new Properties());
  }
}