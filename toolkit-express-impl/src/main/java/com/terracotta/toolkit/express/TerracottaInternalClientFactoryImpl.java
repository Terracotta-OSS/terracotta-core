/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.client.TerracottaClientConfig;
import com.terracotta.toolkit.express.TerracottaInternalClientImpl.ClientShutdownException;
import com.terracotta.toolkit.express.loader.Handler;
import com.terracotta.toolkit.express.loader.Jar;
import com.terracotta.toolkit.express.loader.JarManager;
import com.terracotta.toolkit.express.loader.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class TerracottaInternalClientFactoryImpl implements TerracottaInternalClientFactory {
  private static final String                              EHCACHE_EXPRESS_ENTERPRISE_TERRACOTTA_CLUSTERED_INSTANCE_FACTORY = "net.sf.ehcache.terracotta.ExpressEnterpriseTerracottaClusteredInstanceFactory";
  private static final String                              EHCACHE_EXPRESS_TERRACOTTA_CLUSTERED_INSTANCE_FACTORY            = "net.sf.ehcache.terracotta.StandaloneTerracottaClusteredInstanceFactory";
  private static final boolean                             DEBUG_ON                                                         = Boolean
                                                                                                                                .getBoolean("com.terracotta.express.debug");
  private static final Logger                              LOG                                                              = Logger
                                                                                                                                .getLogger(TerracottaInternalClientImpl.class
                                                                                                                                    .getName());
  private static final String                              TOOLKIT_CONTENT_RESOURCE                                         = "/toolkit-content.txt";
  private final JarManager                                 jarManager                                                       = new JarManager();
  private final Map<String, URL>                           virtualTimJars                                                   = new ConcurrentHashMap<String, URL>();
  private final Map<String, URL>                           virtualEhcacheJars                                               = new ConcurrentHashMap<String, URL>();
  private final Map<String, TerracottaInternalClient>      clientsByUrl                                                     = new ConcurrentHashMap<String, TerracottaInternalClient>();
  private final List<Jar>                                  l1Jars                                                           = Collections
                                                                                                                                .synchronizedList(new ArrayList<Jar>());

  private final ConcurrentMap<String, Map<String, Object>> envByUrl                                                         = new ConcurrentHashMap<String, Map<String, Object>>();

  // public nullary constructor needed as entry point from SPI
  public TerracottaInternalClientFactoryImpl() {
    testForWrongTcconfig();

    System.setProperty("tc.active", "true");
    System.setProperty("tc.dso.globalmode", "false");
    List<String> resourceEntries = loadResourceEntries();
    categorizeJars(resourceEntries, checkEmbeddedEhcacheRequired());
  }

  private void categorizeJars(List<String> resourceEntries, boolean loadEhcache) {
    for (String entry : resourceEntries) {
      URL resourceUrl;
      try {
        // only care about embedded jars for now
        if (!entry.endsWith(".jar")) {
          continue;
        }
        URL rawResource = TerracottaInternalClientFactoryImpl.class.getResource("/" + entry);
        if (rawResource == null) { throw new RuntimeException("Can't load resource from toolkit: " + entry); }
        debugLog("raw resource=%s", rawResource);
        resourceUrl = Util.fixUpUrl(rawResource);
        debugLog("fixed resource=%s", resourceUrl);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (entry.startsWith("L1")) {
        l1Jars.add(jarManager.getOrCreate(resourceUrl.toExternalForm(), resourceUrl));
      } else if (entry.startsWith("TIMs")) {
        jarManager.getOrCreate(resourceUrl.toExternalForm(), resourceUrl);
        virtualTimJars.put(baseName(entry), newTcJarUrl(resourceUrl));
      } else if (entry.contains("exported-classes.jar")) {
        jarManager.getOrCreate(resourceUrl.toExternalForm(), resourceUrl);
      } else if (loadEhcache && entry.contains("ehcache")) {
        jarManager.getOrCreate(resourceUrl.toExternalForm(), resourceUrl);
        virtualEhcacheJars.put(baseName(entry), newTcJarUrl(resourceUrl));
      }
    }
  }

  private List<String> loadResourceEntries() {
    InputStream in = TerracottaInternalClientFactoryImpl.class.getResourceAsStream(TOOLKIT_CONTENT_RESOURCE);
    if (in == null) throw new RuntimeException("Couldn't load resource entries file at: " + TOOLKIT_CONTENT_RESOURCE);
    BufferedReader reader = null;
    try {
      List<String> entries = new ArrayList<String>();
      reader = new BufferedReader(new InputStreamReader(in));
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.length() > 0) {
          entries.add(line);
        }
      }
      return entries;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } finally {
      Util.closeQuietly(in);
    }
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
        client = clientsByUrl.get(tcConfig);
        if (client != null) {
          return joinSharedClient(client, tunneledMBeanDomains);
        } else {
          synchronized (tcConfig.intern()) {
            client = clientsByUrl.get(tcConfig);
            if (client != null) {
              return joinSharedClient(client, tunneledMBeanDomains);
            } else {
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
    // XXX: This is far from correct.
    List<URL> timJars = new ArrayList<URL>();
    for (Entry<String, URL> entry : virtualTimJars.entrySet()) {
      if (entry.getKey().startsWith("tim-") || entry.getKey().startsWith("terracotta-toolkit")) {
        timJars.add(entry.getValue());
      }
    }

    for (Entry<String, URL> entry : virtualEhcacheJars.entrySet()) {
      timJars.add(entry.getValue());
    }

    Map<String, Object> env = createEnvIfAbsent(tcConfig);
    TerracottaInternalClient client = new TerracottaInternalClientImpl(tcConfig, isUrlConfig, jarManager,
                                                                       timJars.toArray(new URL[] {}), getClass()
                                                                           .getClassLoader(), l1Jars,
                                                                       this, rejoinClient, tunneledMBeanDomains, env);
    return client;
  }

  private static void testForWrongTcconfig() {
    String tcConfigValue = System.getProperty("tc.config");
    if (tcConfigValue != null) {
      //
      throw new RuntimeException("The Terracotta config file should not be set through -Dtc.config in this usage.");
    }
  }

  private static String baseName(final String entry) {
    return new File(entry).getName();
  }

  private URL newTcJarUrl(final URL embedded) {
    try {
      return new URL(Handler.TC_JAR_PROTOCOL, "", -1, Handler.TAG + embedded.toExternalForm() + Handler.TAG + "/",
                     new Handler(jarManager));
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean checkEmbeddedEhcacheRequired() {
    // ehcache-core needs to be around
    try {
      Class.forName("net.sf.ehcache.CacheManager");
    } catch (ClassNotFoundException e) {
      return true;
    }
    // One of the ClusteredInstanceFactory classes need to be around (ehcache-terracotta)
    try {
      Class.forName(EHCACHE_EXPRESS_TERRACOTTA_CLUSTERED_INSTANCE_FACTORY);
      return false;
    } catch (ClassNotFoundException e) {
      try {
        Class.forName(EHCACHE_EXPRESS_ENTERPRISE_TERRACOTTA_CLUSTERED_INSTANCE_FACTORY);
        return false;
      } catch (ClassNotFoundException e2) {
        return true;
      }
    }
  }

  private void debugLog(String message, Object... objects) {
    if (DEBUG_ON) {
      LOG.info(String.format("XXX: " + message, objects));
    }
  }

  private Map<String, Object> createEnvIfAbsent(final String tcConfig) {
    Map<String, Object> env = envByUrl.get(tcConfig);
    if (env == null) {
      env = createNewEnv();
    }
    final Map<String, Object> previous = envByUrl.putIfAbsent(tcConfig, env);
    if (previous != null) {
      env = previous;
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
        throw new RuntimeException("Couldn't invoke fetchSecret on " + secretProviderClass);
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
