package com.terracotta.connection.api;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
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
import java.util.Collection;
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

  public boolean handlesURI(URI uri) {
    return scheme.equals(uri.getScheme());
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

    properties.put(ClientBuilderFactory.CLIENT_BUILDER_TYPE, ClientBuilderFactory.ClientBuilderType.of(scheme));
    clientConfig.addGenericProperties(properties);

    final TerracottaInternalClient client = clientFactory.createL1Client(clientConfig);
    try {
      client.init();
    } catch (Exception e) {
      throw new ConnectionException(e);
    }
    return new TerracottaConnection(client.getClientEntityManager(), endpointConnector, client::shutdown);
  }
}
