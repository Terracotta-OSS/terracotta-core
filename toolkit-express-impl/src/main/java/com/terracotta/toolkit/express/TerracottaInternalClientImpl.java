/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.tc.util.FindbugsSuppressWarnings;
import com.terracotta.toolkit.express.loader.Handler;
import com.terracotta.toolkit.express.loader.Jar;
import com.terracotta.toolkit.express.loader.JarManager;
import com.terracotta.toolkit.express.loader.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

class TerracottaInternalClientImpl implements TerracottaInternalClient {

  private static final String  SPI_INIT              = "com.terracotta.toolkit.express.SpiInit";
  private static final boolean DEBUG_ON              = Boolean.getBoolean("com.terracotta.express.debug");
  private static final Logger  LOG                   = Logger.getLogger(TerracottaInternalClientImpl.class.getName());

  public static final String   SECRET_PROVIDER       = "com.terracotta.express.SecretProvider";

  private static final String  EE_SECRET_DELEGATE    = "com.terracotta.toolkit.DelegatingSecretProvider";
  private static final String  SECRET_PROVIDER_CLASS = "org.terracotta.toolkit.SecretProvider";

  static class ClientShutdownException extends Exception {
    //
  }

  private final ClusteredStateLoader            clusteredStateLoader;
  private final AppClassLoader                  appClassLoader;
  private final JarManager                      jarManager;
  private final DSOContextControl               contextControl;
  private final Map<Class, Boolean>             introspected = new WeakHashMap<Class, Boolean>();
  private final AtomicInteger                   refCount     = new AtomicInteger(1);
  private final TerracottaInternalClientFactory parent;
  private final String                          tcConfig;
  private boolean                               shutdown     = false;
  private final boolean                         isUrlConfig;
  private final boolean                         rejoinEnabled;

  @Override
  public boolean isDedicatedClient() {
    return rejoinEnabled;
  }

  @Override
  public synchronized void join() throws ClientShutdownException {
    if (shutdown) throw new ClientShutdownException();
    refCount.incrementAndGet();
    // Collection<URL> modules = introspectModules(introspectionSources);
    // contextControl.activateModules(modules);
  }

  @FindbugsSuppressWarnings("DMI_COLLECTION_OF_URLS")
  private Collection<URL> introspectModules(Class[] sources) {
    // XXX: Class instance probably isn't a good enough unique key to prevent dupes

    Collection<URL> modules = new HashSet<URL>();
    if (sources != null) {
      for (Class source : sources) {
        synchronized (introspected) {
          if (introspected.containsKey(source)) {
            continue;
          }
          introspected.put(source, Boolean.TRUE);
          this.appClassLoader.addLoader(source.getClassLoader());
        }

        try {
          URL sourceJar = source.getProtectionDomain().getCodeSource().getLocation();
          debugLog("Raw sourceJar=%s", sourceJar);
          String jarName = new File(sourceJar.toExternalForm()).getName();
          sourceJar = Util.fixUpUrl(sourceJar);
          debugLog("Fixed sourceJar=%s", sourceJar);

          if (Util.isDirectoryUrl(sourceJar)) {
            debugLog("sourceJar is a directory");
            File dir = new File(sourceJar.toURI());
            File jarFile = new File(new File(dir, ".."), jarName);
            if (jarFile.exists() && jarName.endsWith(".jar")) {
              // this is to accommodate the fact that JBoss explodes the jar into a directory
              // but still have the jar itself intact, specifically during EAR deployment
              sourceJar = jarFile.getCanonicalFile().toURI().toURL();
              debugLog("jarFile exist: %s. Handling sourceJar %s as url", jarFile, sourceJar);
              modules = handleJarUrl(sourceJar, source.getClassLoader());
            } else {
              debugLog("jarFile doesn't exist: %s. Handling sourceJar as directory", jarFile);
              modules = handleDirectoryUrl(sourceJar, source.getClassLoader());
            }
          } else {
            debugLog("sourceJar is a not a directory, handling it as url");
            modules = handleJarUrl(sourceJar, source.getClassLoader());
          }
        } catch (Exception e) {
          if (e instanceof RuntimeException) { throw (RuntimeException) e; }
          throw new RuntimeException(e);
        }

        for (URL url : modules) {
          clusteredStateLoader.addURL(url);
        }
      }
    }
    return modules;
  }

  private Collection<URL> handleDirectoryUrl(URL sourceJar, ClassLoader loader) throws IOException, URISyntaxException {
    FileInputStream publicTypeIn = null;

    Collection<URL> modules = new HashSet<URL>();
    File dir = new File(sourceJar.toURI());
    List<File> list = new LinkedList<File>();
    list.addAll(Arrays.asList(dir.listFiles()));

    try {
      while (!list.isEmpty()) {
        File entry = list.remove(0);
        if (entry.isDirectory()) {
          list.addAll(Arrays.asList(entry.listFiles()));
        } else {
          if (entry.getAbsolutePath().contains("META-INF" + File.separator + "terracotta" + File.separator + "TIMs"
                                                   + File.separator)) {
            if (entry.getName().endsWith(".jar")) {
              URL url = Util.toURL(entry);
              Jar jar = jarManager.getOrCreate(url.toExternalForm(), url);
              modules.add(newTcJarUrl(jar.getSource()));
            }
          }
        }
      }
    } finally {
      Util.closeQuietly(publicTypeIn);
    }
    return modules;
  }

  private Collection<URL> handleJarUrl(URL sourceJar, ClassLoader loader) throws IOException {
    Collection<URL> modules = new HashSet<URL>();

    InputStream in = null;
    JarInputStream jarIn = null;

    try {
      in = sourceJar.openStream();
      jarIn = new JarInputStream(in);
      ZipEntry entry;
      while ((entry = jarIn.getNextEntry()) != null) {
        String entryName = entry.getName();
        if (entryName.startsWith("META-INF/terracotta/TIMs/")) {
          if (entryName.endsWith(".jar")) {
            URL url = loader.getResource(entryName);
            url = Util.fixUpUrl(url);

            Jar jar = jarManager.getOrCreate(url.toExternalForm(), url);
            modules.add(newTcJarUrl(jar.getSource()));
          }
        }
      }
    } finally {
      Util.closeQuietly(in);
      Util.closeQuietly(jarIn);
    }

    return modules;
  }

  @Override
  public <T> T instantiate(String className, Class[] cstrArgTypes, Object[] cstrArgs) throws Exception {
    Class clazz = clusteredStateLoader.loadClass(className);
    Constructor cstr = clazz.getConstructor(cstrArgTypes);
    return (T) cstr.newInstance(cstrArgs);
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
                               ClassLoader appLoader, List<Jar> l1Jars, Class[] moduleIntrospectionSources,
                               TerracottaInternalClientFactory parent, boolean rejoinEnabled,
                               Set<String> tunneledMBeanDomains, Map<String, Object> env) {
    this.rejoinEnabled = rejoinEnabled;
    this.tcConfig = tcConfig;
    this.isUrlConfig = isUrlConfig;
    this.jarManager = jarManager;
    this.parent = parent;

    try {
      this.appClassLoader = new AppClassLoader(appLoader);
      this.clusteredStateLoader = createClusteredStateLoader(appLoader, timJars, l1Jars);

      Collection<URL> modules = introspectModules(moduleIntrospectionSources);

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

      contextControl.init(tunneledMBeanDomains);
      contextControl.activateModules(modules);

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

  private void debugLog(String message, Object... objects) {
    if (DEBUG_ON) {
      LOG.info(String.format("XXX: " + message, objects));
    }
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
