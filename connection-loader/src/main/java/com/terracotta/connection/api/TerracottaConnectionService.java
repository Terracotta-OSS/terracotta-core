package com.terracotta.connection.api;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionService;

import com.tc.util.Assert;
import com.terracotta.connection.TerracottaConnection;
import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientStaticFactory;
import com.terracotta.connection.client.TerracottaClientConfigParams;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;


/**
 * This connection service handles the cases of connecting to a single stripe:  one active and potentially multiple passives
 * which are meant to logically appear as one "connection" to the rest of the platform.
 * This is possible because the underlying connection knows how to probe the stripe for active nodes on start-up or
 * fail-over.
 */
public class TerracottaConnectionService implements ConnectionService {
  private static final String SCHEME = "terracotta";

  @Override
  public boolean handlesURI(URI uri) {
    return SCHEME.equals(uri.getScheme());
  }

  @Override
  public Connection connect(URI uri, Properties properties) throws ConnectionException {
    if (!handlesURI(uri)) {
      throw new IllegalArgumentException("Unknown URI " + uri);
    }

    // TODO: Make use of those properties
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
      URI oneHost = null;
      try {
        oneHost = new URI(host);
      } catch (URISyntaxException e) {
        // We just pulled this from a URI so we don't expect this failure.
        Assert.fail(e.getLocalizedMessage());
      }
      String userInfo = oneHost.getUserInfo();
      String stripeUri = ((null != userInfo) ? (userInfo + "@") : "") + oneHost.getHost() + ":" + oneHost.getPort();
      clientConfig.addStripeMemberUri(stripeUri);
    }
    final TerracottaInternalClient client = TerracottaInternalClientStaticFactory.getOrCreateTerracottaInternalClient(clientConfig);
    client.init();
    try {
      
      return new TerracottaConnection(client.getClientEntityManager(), client.getClientLockManager(), new Runnable() {
        public void run() {
          client.shutdown();
          }
        }
      );
    } catch (Exception e) {
      throw new ConnectionException(e);
    }
  }
}
