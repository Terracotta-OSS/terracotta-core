package com.terracotta.connection.api;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionService;

import com.terracotta.connection.client.TerracottaClientConfigParams;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

abstract class AbstractConnectionService implements ConnectionService {

  @Override
  public final Connection connect(URI uri, Properties properties) throws ConnectionException {
    if (!handlesURI(uri)) {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    // TODO: hook in the connection listener

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
      String userInfo = oneHost.getUserInfo();
      String stripeUri = ((null != userInfo) ? (userInfo + "@") : "") + oneHost.getHost() + ":" + oneHost.getPort();
      clientConfig.addStripeMemberUri(stripeUri);
    }
    clientConfig.addGenericProperties(properties);
    return internalConnect(clientConfig);
  }

  abstract Connection internalConnect(TerracottaClientConfigParams configParams)
      throws ConnectionException;
}
