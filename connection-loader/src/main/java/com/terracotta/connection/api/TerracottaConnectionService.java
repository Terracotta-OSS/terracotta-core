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

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.terracotta.connection.EndpointConnector;
import com.terracotta.connection.EndpointConnectorImpl;
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
import java.util.concurrent.TimeoutException;


/**
 * This connection service handles the cases of connecting to a single stripe:  one active and potentially multiple passives
 * which are meant to logically appear as one "connection" to the rest of the platform.
 * This is possible because the underlying connection knows how to probe the stripe for active nodes on start-up or
 * fail-over.
 */
public class TerracottaConnectionService implements ConnectionService {
  private static final String SCHEME = "terracotta";

  private final EndpointConnector endpointConnector;

  @Override
  public boolean handlesURI(URI uri) {
    return SCHEME.equals(uri.getScheme());
  }

  public TerracottaConnectionService() {
    this(new EndpointConnectorImpl());
  }

  public TerracottaConnectionService(EndpointConnector endpointConnector) {
    this.endpointConnector = endpointConnector;
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
    
    clientConfig.addGenericProperties(properties);
    final TerracottaInternalClient client = TerracottaInternalClientStaticFactory.getOrCreateTerracottaInternalClient(clientConfig);
    try {
      client.init();
    } catch (TimeoutException exp) {
      throw new ConnectionException(exp);
    } catch (ConfigurationSetupException config) {
      throw new ConnectionException(config);
    } catch (InterruptedException ie) {
      throw new ConnectionException(ie);
    } catch (Throwable t) {
      throw new ConnectionException(t);
    }
    return new TerracottaConnection(client.getClientEntityManager(), endpointConnector, new Runnable() {
        public void run() {
          client.shutdown();
          }
        }
      );
  }
}
