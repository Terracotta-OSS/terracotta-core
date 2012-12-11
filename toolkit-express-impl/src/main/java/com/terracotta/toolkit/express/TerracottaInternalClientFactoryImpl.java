/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.client.TerracottaClientConfig;
import com.terracotta.toolkit.express.TerracottaInternalClientImpl.ClientShutdownException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TerracottaInternalClientFactoryImpl implements TerracottaInternalClientFactory {
  private final Map<String, TerracottaInternalClient>      clientsByUrl = new ConcurrentHashMap<String, TerracottaInternalClient>();

  private final ConcurrentMap<String, Map<String, Object>> envByUrl     = new ConcurrentHashMap<String, Map<String, Object>>();

  // public nullary constructor needed as entry point from SPI
  public TerracottaInternalClientFactoryImpl() {
    testForWrongTcconfig();

    System.setProperty("tc.active", "true");
    System.setProperty("tc.dso.globalmode", "false");
  }

  @Override
  public TerracottaInternalClient getOrCreateL1Client(TerracottaClientConfig config) {
    String tcConfig = config.getTcConfigSnippetOrUrl();
    if (config.isUrl()) {
      tcConfig = URLConfigUtil.translateSystemProperties(tcConfig);
    }
    return getOrCreateClient(tcConfig, config.isUrl(), config.isRejoin(), config.getTunnelledMBeanDomains());
  }

  @Override
  public void remove(TerracottaInternalClient client, String tcConfig, boolean isUrlConfig) {
    if (client.isDedicatedClient()) {
      // nothing to clean up for dedicated/non-shared clients
      return;
    }

    if (isUrlConfig) {
      synchronized (tcConfig.intern()) {
        TerracottaInternalClient removed = clientsByUrl.remove(tcConfig);
        if (removed != client) { throw new AssertionError("removed: " + removed + ", expecting: " + client + ", for "
                                                          + tcConfig); }
      }
    }
  }

  private TerracottaInternalClient getOrCreateClient(String tcConfig, boolean isUrlConfig, final boolean rejoinClient,
                                                     Set<String> tunneledMBeanDomains) {
    TerracottaInternalClient client = null;
    if (rejoinClient) {
      return createClient(tcConfig, isUrlConfig, rejoinClient, tunneledMBeanDomains);
    } else {
      if (!isUrlConfig) {
        return createClient(tcConfig, isUrlConfig, rejoinClient, tunneledMBeanDomains);
      } else {
        synchronized (tcConfig.intern()) {
          client = clientsByUrl.get(tcConfig);
          if (client == null) {
            // create a new client
            client = createClient(tcConfig, isUrlConfig, rejoinClient, tunneledMBeanDomains);
            clientsByUrl.put(tcConfig, client);
          } else {
            // check if existing client is initializing or online, if not create a new one else reuse
            if (!client.isInitialized() || client.isOnline()) {
              return joinSharedClient(client, tunneledMBeanDomains);
            } else {
              // create a new client and update the mapping too
              client = createClient(tcConfig, isUrlConfig, rejoinClient, tunneledMBeanDomains);
              clientsByUrl.put(tcConfig, client);
            }
          }
        }
      }
    }
    return client;
  }

  private TerracottaInternalClient joinSharedClient(TerracottaInternalClient client, Set<String> tunneledMBeanDomains) {
    try {
      client.join(tunneledMBeanDomains);
      return client;
    } catch (ClientShutdownException e) {
      throw new IllegalStateException("this client is already shutdown", e);
    }
  }

  private TerracottaInternalClient createClient(String tcConfig, boolean isUrlConfig, final boolean rejoinClient,
                                                Set<String> tunneledMBeanDomains) {

    Map<String, Object> env = createEnvIfAbsent(tcConfig);
    TerracottaInternalClient client = new TerracottaInternalClientImpl(tcConfig, isUrlConfig, getClass()
        .getClassLoader(), this, rejoinClient, tunneledMBeanDomains, new ConcurrentHashMap<String, Object>(env));
    return client;
  }

  private static void testForWrongTcconfig() {
    String tcConfigValue = System.getProperty("tc.config");
    if (tcConfigValue != null) {
      //
      throw new RuntimeException("The Terracotta config file should not be set through -Dtc.config in this usage.");
    }
  }

  private Map<String, Object> createEnvIfAbsent(final String tcConfig) {
    Map<String, Object> env = envByUrl.get(tcConfig);
    if (env == null) {
      synchronized (envByUrl) {
        env = envByUrl.get(tcConfig);
        if (env != null) { return env; }
        env = createNewEnv();
        final Map<String, Object> previous = envByUrl.putIfAbsent(tcConfig, env);
        if (previous != null) { throw new IllegalStateException("Some environment map was already present for config "
                                                                + tcConfig); }
      }
    }
    return env;
  }

  private Map<String, Object> createNewEnv() {
    final Map<String, Object> env = new HashMap<String, Object>();
    final String secretProviderClass = System.getProperty(TerracottaInternalClientImpl.SECRET_PROVIDER);
    if (secretProviderClass != null) {
      Object instance = newInstance(secretProviderClass);
      try {
        instance.getClass().getMethod("fetchSecret").invoke(instance);
      } catch (Exception e) {
        throw new RuntimeException("Error invoking fetchSecret on " + secretProviderClass, e);
      }
      env.put(TerracottaInternalClientImpl.SECRET_PROVIDER, instance);
    }
    return env;
  }

  @SuppressWarnings("unchecked")
  private static <T> T newInstance(final String secretProviderClass) {
    try {
      return (T) Class.forName(secretProviderClass).newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException("Couldn't instantiate a new instance of " + secretProviderClass, e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Couldn't load or instantiate a new instance of " + secretProviderClass, e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Couldn't load Class " + secretProviderClass, e);
    }
  }
}
