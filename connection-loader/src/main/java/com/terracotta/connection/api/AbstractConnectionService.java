package com.terracotta.connection.api;

import com.tc.object.ClientBuilderFactory;
import com.tc.object.ConnectionType;
import com.terracotta.connection.EndpointConnector;
import com.terracotta.connection.EndpointConnectorImpl;
import com.terracotta.connection.TerracottaConnection;
import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientImpl;
import com.terracotta.connection.URLConfigUtil;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

abstract class AbstractConnectionService implements ConnectionService {

  private final String scheme;
  private final EndpointConnector endpointConnector;

  AbstractConnectionService(String scheme) {
    this(scheme, new EndpointConnectorImpl());
  }

  AbstractConnectionService(String scheme, EndpointConnector endpointConnector) {
    this.scheme = scheme;
    this.endpointConnector = endpointConnector;
  }

  @Override
  public boolean handlesURI(URI uri) {
    return scheme.equals(uri.getScheme());
  }

  @Override
  public final Connection connect(URI uri, Properties properties) throws ConnectionException {
    if (!handlesURI(uri)) {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    // We may be specifying a comma-delimited list of servers in the stripe so parse the URI with this possibility in mind.
    TerracottaClientStripeConnectionConfig stripeConnectionConfig = parseURI(uri);
    properties.put(ClientBuilderFactory.CONNECTION_TYPE_PROPERTY, ConnectionType.of(scheme));
    TerracottaInternalClient client = new TerracottaInternalClientImpl(stripeConnectionConfig, properties);

    try {
      client.init();
    } catch (Exception e) {
      throw new ConnectionException(e);
    }
    return new TerracottaConnection(client.getClientEntityManager(), endpointConnector, client::shutdown);
  }

  TerracottaClientStripeConnectionConfig parseURI(URI uri) {
    TerracottaClientStripeConnectionConfig stripeConnectionConfig = new TerracottaClientStripeConnectionConfig();
    String[] hosts = uri.getSchemeSpecificPart().split(",");
    for (String host : hosts) {
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
      String userInfo = oneHost.getUserInfo();
      String stripeUri = ((null != userInfo) ? (userInfo + "@") : "") + oneHost.getHost() + ":" + oneHost.getPort();
      stripeConnectionConfig.addStripeMemberUri(URLConfigUtil.translateSystemProperties(stripeUri));
    }
    return stripeConnectionConfig;
  }
}
