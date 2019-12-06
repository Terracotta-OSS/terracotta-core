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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TerracottaConnectionServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void connect() throws Exception {
    TerracottaInternalClientFactory clientFactoryMock = mock(TerracottaInternalClientFactory.class);
    when(clientFactoryMock.createL1Client(any())).thenReturn(mock(TerracottaInternalClient.class));
    TerracottaConnectionService terracottaConnectionService =
        new TerracottaConnectionService(mock(EndpointConnector.class), clientFactoryMock);
    Connection connection =
        terracottaConnectionService.connect(URI.create("terracotta://localhost:9410"), new Properties());
    assertThat(connection, notNullValue());
  }

  @Test
  public void connectWithNonTerracottaScheme() throws Exception {
    TerracottaInternalClientFactory clientFactoryMock = mock(TerracottaInternalClientFactory.class);
    when(clientFactoryMock.createL1Client(any())).thenReturn(mock(TerracottaInternalClient.class));
    TerracottaConnectionService terracottaConnectionService =
        new TerracottaConnectionService(mock(EndpointConnector.class), clientFactoryMock);
    expectedException.expect(IllegalArgumentException.class);
    terracottaConnectionService.connect(URI.create("non-terracotta://localhost:9410"), new Properties());
  }
}