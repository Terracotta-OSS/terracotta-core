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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

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
  private static final String SCHEME = "passthrough";

  @Override
  public boolean handlesURI(URI uri) {
    return SCHEME.equals(uri.getScheme());
  }

  @Override
  public Connection connect(URI uri, Properties properties) throws ConnectionException {
    Connection connection = null;
    String serverName = uri.getHost();
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
