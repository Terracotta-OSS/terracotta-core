package com.terracotta.connection.api;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionService;

import com.terracotta.connection.TerracottaConnection;
import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientStaticFactory;
import com.terracotta.connection.client.TerracottaClientConfig;
import com.terracotta.connection.client.TerracottaClientConfigParams;

import java.net.URI;
import java.util.Properties;


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

    String tcConfigUrlParam = (uri.getUserInfo() != null ? uri.getUserInfo() + "@" : "") + (uri.getHost() + ":" + uri.getPort());
    TerracottaClientConfig clientConfig = new TerracottaClientConfigParams()
        .tcConfigUrl(tcConfigUrlParam)
        .newTerracottaClientConfig();
    final TerracottaInternalClient client = TerracottaInternalClientStaticFactory.getOrCreateTerracottaInternalClient(clientConfig);
    client.init();
    try {
      return new TerracottaConnection(client.getClientEntityManager(), client.getClientLockManager(), (Runnable) () -> client.shutdown());
    } catch (Exception e) {
      throw new ConnectionException(e);
    }
  }
}
