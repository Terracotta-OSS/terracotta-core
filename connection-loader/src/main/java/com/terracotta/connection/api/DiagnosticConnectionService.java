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
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionService;

import com.terracotta.connection.TerracottaConnection;
import com.terracotta.connection.URLConfigUtil;
import com.terracotta.connection.client.TerracottaClientConfigParams;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;
import com.terracotta.diagnostic.DiagnosticClientImpl;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeoutException;


/**
 * This connection service handles the cases of connecting to a single stripe:  one active and potentially multiple passives
 * which are meant to logically appear as one "connection" to the rest of the platform.
 * This is possible because the underlying connection knows how to probe the stripe for active nodes on start-up or
 * fail-over.
 */
public class DiagnosticConnectionService implements ConnectionService {
  private static final String SCHEME = "diagnostic";

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

    clientConfig.addStripeMemberUri(uri.getHost() + ":" + uri.getPort());
    clientConfig.addGenericProperties(properties);
    TerracottaClientStripeConnectionConfig stripeConnectionConfig = new TerracottaClientStripeConnectionConfig();
    
    for (String memberUri : clientConfig.getStripeMemberUris()) {
      String expandedMemberUri = URLConfigUtil.translateSystemProperties(memberUri);
      stripeConnectionConfig.addStripeMemberUri(expandedMemberUri);
    }

    final DiagnosticClientImpl client = new DiagnosticClientImpl(stripeConnectionConfig, properties);
    try {
      client.init();
    } catch (TimeoutException exp) {
      throw new ConnectionException(exp);
    } catch (ConfigurationSetupException config) {
      throw new ConnectionException(config);
    } catch (InterruptedException ie) {
      throw new ConnectionException(ie);
    }
    
    return new TerracottaConnection(client.getClientEntityManager(), new Runnable() {
        public void run() {
          client.shutdown();
          }
        }
      );
  }
}
