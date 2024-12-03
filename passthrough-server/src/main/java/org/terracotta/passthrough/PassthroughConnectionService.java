/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.net.InetSocketAddress;
import java.net.URI;
import java.rmi.UnknownHostException;
import java.util.Properties;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;


/**
 * Accesses PassthroughServer instances by URI.
 * The named server must first be registered in the shared PassthroughServerRegistry shared instance for the host name given
 * in the URI.  A URI of the shape "passthrough://serverName" will attempt to resolve the server registered as "serverName".
 * 
 * The special concern to realize, regarding our use of passthrough, is that we will treat an entire stripe (an active and
 * any number of corresponding passives) as the same "named" PassthroughServer.  Even if this changes, it should always be
 * sufficient to describe a stripe as a "name", instead of worrying about individual servers within a stripe (since there is
 * no network).
 * 
 * Note that this ConnectionService is only accessible if "org.terracotta.passthrough.PassthroughConnectionService"
 * is listed in "META-INF/services/org.terracotta.connection.ConnectionService".
 */
public class PassthroughConnectionService implements ConnectionService {
  private static final String CONNECTION_TYPE = "passthrough";

  @Override
  public boolean handlesURI(URI uri) {
    return handlesConnectionType(uri.getScheme());
  }

  @Override
  public boolean handlesConnectionType(String connectionType) {
    return CONNECTION_TYPE.equals(connectionType);
  }

  @Override
  public Connection connect(URI uri, Properties properties) throws ConnectionException {
    return getConnection(uri.getHost(), properties);
  }

  @Override
  public Connection connect(Iterable<InetSocketAddress> servers, Properties properties) throws ConnectionException {
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TYPE, CONNECTION_TYPE);
    return getConnection(servers.iterator().next().getHostString(), properties);
  }

  private Connection getConnection(String serverName, Properties properties) throws ConnectionException {
    Connection connection;
    PassthroughServer server = PassthroughServerRegistry.getSharedInstance().getServerForName(serverName);
    if (null != server) {
      String connectionName = properties.getProperty(ConnectionPropertyNames.CONNECTION_NAME);
      if (null == connectionName) {
        connectionName = "";
      }
      connection = server.connectNewClient(connectionName);
    } else {
      throw new ConnectionException(new UnknownHostException(serverName));
    }
    return connection;
  }
}
