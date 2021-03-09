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

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;

import com.terracotta.connection.EndpointConnectorImpl;
import com.terracotta.connection.TerracottaConnection;
import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientFactory;
import com.terracotta.connection.TerracottaInternalClientFactoryImpl;
import java.net.InetSocketAddress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.terracotta.entity.EndpointConnector;

abstract class AbstractConnectionService implements ConnectionService {

  private final List<String> scheme;
  private final EndpointConnector endpointConnector;
  private final TerracottaInternalClientFactory clientFactory;

  AbstractConnectionService(List<String> scheme) {
    this(scheme, new EndpointConnectorImpl(), new TerracottaInternalClientFactoryImpl());
  }

  AbstractConnectionService(List<String> scheme,
                            EndpointConnector endpointConnector,
                            TerracottaInternalClientFactory clientFactory) {
    this.scheme = scheme;
    this.endpointConnector = endpointConnector;
    this.clientFactory = clientFactory;
  }

  @Override
  public boolean handlesURI(URI uri) {
    return handlesConnectionType(uri.getScheme());
  }

  @Override
  public boolean handlesConnectionType(String connectionType) {
    return this.scheme.contains(connectionType);
  }

  @Override
  public final Connection connect(URI uri, Properties properties) throws ConnectionException {
    if (!handlesURI(uri)) {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    List<InetSocketAddress> serverAddresses = new ArrayList<>();

    // We may be specifying a comma-delimited list of servers in the stripe so parse the URI with this possibility in mind.
    String[] hosts = uri.getSchemeSpecificPart().split(",");
    for(String host : hosts) {
      // Note that we will need the "//" prefix in order to make sure that the URI is parsed correctly (only the first in
      // the list normally has this).
      if (0 != host.indexOf("//")) {
        host = "//" + host;
      }
      // Make this back into a URI so that we can parse out the user info, etc, using high-level routines.
      URI oneHost;
      try {
        // parse given host uri's authority as server based authority, this is used to validate port number currently
        // Note that new URI(host) doesn't throw URISyntaxException for uri of the form {ipv4/hostname:dd45} as it can be
        // parsed registry based authority but throws URISyntaxException when it contains ipv6 address though as they
        // can't be parsed as registry based authority
        oneHost = new URI(host).parseServerAuthority();
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Unable to parse uri " + uri, e);
      }
      int port = Math.max(oneHost.getPort(), 0);
      serverAddresses.add(InetSocketAddress.createUnresolved(oneHost.getHost(), port));
    }
    return createConnection(uri.getScheme(), serverAddresses, properties);
  }

  @Override
  public final Connection connect(Iterable<InetSocketAddress> serverAddresses, Properties properties) throws ConnectionException {
    String connectionType = properties.getProperty(ConnectionPropertyNames.CONNECTION_TYPE, scheme.get(0));
    if (!handlesConnectionType(connectionType)) {
      throw new IllegalArgumentException("Unknown connectionType " + connectionType);
    }

    return createConnection(connectionType, serverAddresses, properties);
  }

  private Connection createConnection(String type, Iterable<InetSocketAddress> serverAddresses, Properties properties) throws DetailedConnectionException {
    final TerracottaInternalClient client = clientFactory.createL1Client(type, serverAddresses, properties);
    properties.put("connection", serverAddresses);
    client.init();
    return new TerracottaConnection(properties, client::getClientEntityManager, endpointConnector, client::shutdown);
  }
}