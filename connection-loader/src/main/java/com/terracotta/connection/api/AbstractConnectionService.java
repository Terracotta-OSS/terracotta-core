package com.terracotta.connection.api;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;

import com.tc.object.ClientBuilderFactory;
import com.terracotta.connection.EndpointConnector;
import com.terracotta.connection.EndpointConnectorImpl;
import com.terracotta.connection.TerracottaConnection;
import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientFactory;
import com.terracotta.connection.TerracottaInternalClientFactoryImpl;
import com.terracotta.connection.client.TerracottaClientConfigParams;
import java.net.InetSocketAddress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

abstract class AbstractConnectionService implements ConnectionService {

  private final String scheme;
  private final EndpointConnector endpointConnector;
  private final TerracottaInternalClientFactory clientFactory;

  AbstractConnectionService(String scheme) {
    this(scheme, new EndpointConnectorImpl(), new TerracottaInternalClientFactoryImpl());
  }

  AbstractConnectionService(String scheme,
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
    return this.scheme.equals(connectionType);
  }

  @Override
  public final Connection connect(URI uri, Properties properties) throws ConnectionException {
    if (!handlesURI(uri)) {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    // We may be specifying a comma-delimited list of servers in the stripe so parse the URI with this possibility in mind.
    TerracottaClientConfigParams clientConfig = new TerracottaClientConfigParams();
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
      int port = oneHost.getPort() < 0 ? 0 : oneHost.getPort();
      InetSocketAddress address = InetSocketAddress.createUnresolved(oneHost.getHost(), port);
      clientConfig.addStripeMember(address);
    }

    return createConnection(properties, clientConfig);
  }

  @Override
  public final Connection connect(Iterable<InetSocketAddress> servers, Properties properties) throws ConnectionException {
    String connectionType = properties.getProperty(ConnectionPropertyNames.CONNECTION_TYPE, "terracotta");
    if (!handlesConnectionType(connectionType)) {
      throw new IllegalArgumentException("Unknown connectionType " + connectionType);
    }

    TerracottaClientConfigParams clientConfig = new TerracottaClientConfigParams();
    servers.forEach(clientConfig::addStripeMember);
    return createConnection(properties, clientConfig);
  }

  private Connection createConnection(Properties properties, TerracottaClientConfigParams clientConfig) throws DetailedConnectionException {
    properties.put(ClientBuilderFactory.CLIENT_BUILDER_TYPE, ClientBuilderFactory.ClientBuilderType.of(scheme));
    clientConfig.addGenericProperties(properties);

    final TerracottaInternalClient client = clientFactory.createL1Client(clientConfig);
    client.init();
    return new TerracottaConnection(client.getClientEntityManager(), endpointConnector, client::shutdown);
  }
}