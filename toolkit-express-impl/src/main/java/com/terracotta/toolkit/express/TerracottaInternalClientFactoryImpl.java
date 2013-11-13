/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.client.TerracottaClientConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TerracottaInternalClientFactoryImpl implements TerracottaInternalClientFactory {
  private final ConcurrentMap<String, Map<String, Object>> envByUrl     = new ConcurrentHashMap<String, Map<String, Object>>();

  // public nullary constructor needed as entry point from SPI
  public TerracottaInternalClientFactoryImpl() {
    testForWrongTcconfig();

    System.setProperty("tc.active", "true");
    System.setProperty("tc.dso.globalmode", "false");
  }

  @Override
  public TerracottaInternalClient createL1Client(TerracottaClientConfig config) {
    String tcConfig = config.getTcConfigSnippetOrUrl();
    if (config.isUrl()) {
      tcConfig = URLConfigUtil.translateSystemProperties(tcConfig);
    }
    return createClient(tcConfig, config.isUrl(), config.isRejoin(), config.getTunnelledMBeanDomains(), config.getProductId());
  }


  private TerracottaInternalClient createClient(String tcConfig, boolean isUrlConfig, final boolean rejoinClient,
                                                Set<String> tunneledMBeanDomains, final String productId) {

    Map<String, Object> env = createEnvIfAbsent(tcConfig);
    TerracottaInternalClient client = new TerracottaInternalClientImpl(tcConfig, isUrlConfig, getClass()
        .getClassLoader(), rejoinClient, tunneledMBeanDomains, productId, new ConcurrentHashMap<String, Object>(env));
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
