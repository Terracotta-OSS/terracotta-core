package com.terracotta.connection.api;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionService;

import com.terracotta.connection.TerracottaInternalClient;
import com.terracotta.connection.TerracottaInternalClientStaticFactory;
import com.terracotta.connection.client.TerracottaClientConfig;
import com.terracotta.connection.client.TerracottaClientConfigParams;

import java.net.URI;
import java.util.Properties;

/*
 * @author twu
 */
public class TerracottaConnectionService implements ConnectionService {
  private static final String SCHEME = "terracotta";
  // We need the class names here instead of referencing the .class directly since we need to use the client's classloader.
  private static final String CLIENT_ENTITY_MANAGER_CLASS_NAME = "com.tc.object.ClientEntityManager";
  private static final String CLIENT_LOCK_MANAGER_CLASS_NAME = "com.tc.object.locks.ClientLockManager";
  private static final String CONNECTION_CLASS_NAME = "com.terracotta.connection.TerracottaConnection";

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

    String tcConfigSnippetOrUrlParam = (uri.getUserInfo() != null ? uri.getUserInfo() + "@" : "") + (uri.getHost() + ":" + uri.getPort());
    TerracottaClientConfig clientConfig = new TerracottaClientConfigParams().isUrl(true)
        .tcConfigSnippetOrUrl(tcConfigSnippetOrUrlParam).newTerracottaClientConfig();
    final TerracottaInternalClient client = TerracottaInternalClientStaticFactory.getOrCreateTerracottaInternalClient(clientConfig);
    client.init();
    try {
      return client.instantiate(CONNECTION_CLASS_NAME,
          new Class[] { client.loadClass(CLIENT_ENTITY_MANAGER_CLASS_NAME), client.loadClass(CLIENT_LOCK_MANAGER_CLASS_NAME), Runnable.class },
          new Object[] { client.getClientEntityManager(), client.getClientLockManager(), (Runnable) () -> client.shutdown()});
    } catch (Exception e) {
      throw new ConnectionException(e);
    }
  }
}
