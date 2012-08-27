/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.express.loader.Handler;
import com.terracotta.toolkit.express.loader.Jar;
import com.terracotta.toolkit.express.loader.JarManager;
import com.terracotta.toolkit.express.loader.Util;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

class TerracottaInternalClientImpl implements TerracottaInternalClient {

  private static final String SPI_INIT              = "com.terracotta.toolkit.express.SpiInit";
  public static final String  SECRET_PROVIDER       = "com.terracotta.express.SecretProvider";

  private static final String EE_SECRET_DELEGATE    = "com.terracotta.toolkit.DelegatingSecretProvider";
  private static final String SECRET_PROVIDER_CLASS = "org.terracotta.toolkit.SecretProvider";

  static class ClientShutdownException extends Exception {
    //
  }

  private final ClusteredStateLoader            clusteredStateLoader;
  private final AppClassLoader                  appClassLoader;
  private final JarManager                      jarManager;
  private final DSOContextControl               contextControl;
  private final AtomicInteger                   refCount = new AtomicInteger(0);
  private final TerracottaInternalClientFactory parent;
  private final String                          tcConfig;
  private final boolean                         isUrlConfig;
  private final boolean                         rejoinEnabled;
  private boolean                               shutdown = false;

  @Override
  public boolean isDedicatedClient() {
    return rejoinEnabled;
  }

  @Override
  public synchronized void join(Set<String> tunnelledMBeanDomains) throws ClientShutdownException {
    if (shutdown) throw new ClientShutdownException();
    refCount.incrementAndGet();
    contextControl.activateTunnelledMBeanDomains(tunnelledMBeanDomains);
  }

  @Override
  public <T> T instantiate(String className, Class[] cstrArgTypes, Object[] cstrArgs) throws Exception {
    Class clazz = clusteredStateLoader.loadClass(className);
    Constructor cstr = clazz.getConstructor(cstrArgTypes);
    return (T) cstr.newInstance(cstrArgs);
  }

  @Override
  public synchronized boolean isShutdown() {
    return shutdown;
  }

  @Override
  public synchronized void shutdown() {
    final boolean shutdownClient;
    if (isDedicatedClient()) {
      shutdownClient = true;
    } else {
      // decrement the reference counter by 1 as its shared client
      // destroy real client when count == 0;
      int count = refCount.decrementAndGet();
      if (count < 0) {
        //
        throw new IllegalStateException(
                                        "shutdown() called too many times, this represents a bug in the caller. count = "
                                            + count);
      }
      shutdownClient = count == 0;
    }

    if (shutdownClient) {
      shutdown = true;
      try {
        contextControl.shutdown();
      } finally {
        appClassLoader.clear();
        parent.remove(this, tcConfig, isUrlConfig);
      }
    }
  }

  private byte[] getClassBytes(Class klass) {
    ClassLoader loader = getClass().getClassLoader();
    String res = klass.getName().replace('.', '/').concat(".class");
    try {
      return Util.extract(loader.getResourceAsStream(res));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private URL[] toURLs(final List<Jar> jars) throws IOException {
    Jar[] jarArray = jars.toArray(new Jar[] {});
    URL[] urls = new URL[jarArray.length];
    for (int i = 0; i < jarArray.length; i++) {
      urls[i] = newTcJarUrl(jarArray[i].getSource());
    }

    return urls;
  }

  TerracottaInternalClientImpl(String tcConfig, boolean isUrlConfig, JarManager jarManager, URL[] timJars,
                               ClassLoader appLoader, List<Jar> l1Jars, TerracottaInternalClientFactory parent,
                               boolean rejoinEnabled, Set<String> tunneledMBeanDomains, Map<String, Object> env) {
    this.rejoinEnabled = rejoinEnabled;
    this.tcConfig = tcConfig;
    this.isUrlConfig = isUrlConfig;
    this.jarManager = jarManager;
    this.parent = parent;

    try {
      this.appClassLoader = new AppClassLoader(appLoader);
      this.clusteredStateLoader = createClusteredStateLoader(appLoader, timJars, l1Jars);

      Class bootClass = clusteredStateLoader.loadClass(StandaloneL1Boot.class.getName());
      Constructor<?> cstr = bootClass.getConstructor(String.class, Boolean.TYPE, ClassLoader.class, Boolean.TYPE,
                                                     Map.class);

      // XXX: It's be nice to not use Object here, but exposing the necessary type (DSOContext) seems wrong too)
      if (isUrlConfig && isRequestingSecuredEnv(tcConfig)) {
        if (env != null) {
          env.put("com.terracotta.SecretProvider", // replaces old instance if rejoin
                  newSecretProviderDelegate(clusteredStateLoader, env.get(TerracottaInternalClientImpl.SECRET_PROVIDER)));
        }
      }
      Callable<Object> boot = (Callable<Object>) cstr.newInstance(tcConfig, isUrlConfig, clusteredStateLoader,
                                                                  rejoinEnabled, env);

      Object context = boot.call();

      Class spiInit = clusteredStateLoader.loadClass(SPI_INIT);
      contextControl = (DSOContextControl) spiInit.getConstructor(Object.class).newInstance(context);
      join(tunneledMBeanDomains);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ClusteredStateLoader createClusteredStateLoader(ClassLoader appLoader, URL[] timJars, List<Jar> l1Jars)
      throws IOException {
    URL[] urls = new URL[timJars.length + l1Jars.size()];
    System.arraycopy(toURLs(l1Jars), 0, urls, 0, l1Jars.size());
    System.arraycopy(timJars, 0, urls, l1Jars.size(), timJars.length);
    ClusteredStateLoader loader = new ClusteredStateLoader(urls, appClassLoader);

    loader.addExtraClass(SpiInit.class.getName(), getClassBytes(SpiInit.class));
    loader.addExtraClass(StandaloneL1Boot.class.getName(), getClassBytes(StandaloneL1Boot.class));

    return loader;
  }

  private static boolean isRequestingSecuredEnv(final String tcConfig) {
    return tcConfig.contains("@");
  }

  private URL newTcJarUrl(final URL embedded) throws IOException {
    URL fixedEmbbeded = Util.fixUpUrl(embedded);

    return new URL(Handler.TC_JAR_PROTOCOL, "", -1, Handler.TAG + fixedEmbbeded.toExternalForm() + Handler.TAG + "/",
                   new Handler(jarManager));
  }

  private static Object newSecretProviderDelegate(final ClassLoader loader, final Object backEnd) {
    try {
      Class customClass = Class.forName(SECRET_PROVIDER_CLASS);
      Class tkClass = loader.loadClass(EE_SECRET_DELEGATE);
      return tkClass.getConstructor(customClass).newInstance(backEnd);
    } catch (Exception e) {
      throw new RuntimeException("Couldn't wrap the custom impl. " + backEnd.getClass().getName()
                                 + " in an instance of " + EE_SECRET_DELEGATE, e);
    }
  }

}
