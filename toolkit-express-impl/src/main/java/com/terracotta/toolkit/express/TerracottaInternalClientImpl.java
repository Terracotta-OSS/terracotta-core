/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import static com.terracotta.management.security.SecretUtils.TERRACOTTA_CUSTOM_SECRET_PROVIDER_PROP;

import org.terracotta.toolkit.ToolkitRuntimeException;

import com.terracotta.toolkit.express.loader.Util;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

class TerracottaInternalClientImpl implements TerracottaInternalClient {

  private static final String SPI_INIT                                                         = "com.terracotta.toolkit.express.SpiInit";
  public static final String  SECRET_PROVIDER                                                  = "com.terracotta.express.SecretProvider";
  public static final String  DSO_CONTEXT_IMPL                                                 = "com.tc.object.bytecode.hook.impl.DSOContextImpl";

  private static final String EE_SECRET_DELEGATE                                               = "com.terracotta.toolkit.DelegatingSecretProvider";
  private static final String SECRET_PROVIDER_CLASS                                            = "org.terracotta.toolkit.SecretProvider";

  private static final String EHCACHE_EXPRESS_ENTERPRISE_TERRACOTTA_CLUSTERED_INSTANCE_FACTORY = "net.sf.ehcache.terracotta.ExpressEnterpriseTerracottaClusteredInstanceFactory";
  private static final String EHCACHE_EXPRESS_TERRACOTTA_CLUSTERED_INSTANCE_FACTORY            = "net.sf.ehcache.terracotta.StandaloneTerracottaClusteredInstanceFactory";

  static class ClientShutdownException extends Exception {
    //
  }

  private final ClusteredStateLoader            clusteredStateLoader;
  private final AppClassLoader                  appClassLoader;
  private volatile DSOContextControl            contextControl;
  private final AtomicInteger                   refCount             = new AtomicInteger(0);
  private boolean                               shutdown             = false;
  private volatile Object                       dsoContext;
  private final Set<String>                     tunneledMBeanDomains = new HashSet<String>();
  private volatile boolean                      isInitialized        = false;

  TerracottaInternalClientImpl(String tcConfig, boolean isUrlConfig, ClassLoader appLoader, boolean rejoinEnabled,
                               Set<String> tunneledMBeanDomains, final String productId, Map<String, Object> env) {
    if (tunneledMBeanDomains != null) {
      this.tunneledMBeanDomains.addAll(tunneledMBeanDomains);
    }
    try {
      this.appClassLoader = new AppClassLoader(appLoader);
      this.clusteredStateLoader = createClusteredStateLoader(appLoader);

      Class bootClass = clusteredStateLoader.loadClass(StandaloneL1Boot.class.getName());
      Constructor<?> cstr = bootClass.getConstructor(String.class, Boolean.TYPE, ClassLoader.class, Boolean.TYPE,
                                                     String.class, Map.class);

      // XXX: It's be nice to not use Object here, but exposing the necessary type (DSOContext) seems wrong too)
      if (isUrlConfig && isRequestingSecuredEnv(tcConfig)) {
        if (env != null) {
          env.put(TERRACOTTA_CUSTOM_SECRET_PROVIDER_PROP,
                  newSecretProviderDelegate(clusteredStateLoader, env.get(TerracottaInternalClientImpl.SECRET_PROVIDER)));
        }
      }
      Callable<Object> boot = (Callable<Object>) cstr.newInstance(tcConfig, isUrlConfig, clusteredStateLoader,
                                                                  rejoinEnabled, productId, env);

      Object context = boot.call();
      this.dsoContext = context;
    } catch (Exception e) {
      throw new ToolkitRuntimeException(e);
    }
  }

  @Override
  public synchronized void init() {
    if (isInitialized) { return; }

    try {
      Class dsoContextClass = clusteredStateLoader.loadClass(DSO_CONTEXT_IMPL);
      Method method = dsoContextClass.getMethod("init");

      method.invoke(dsoContext);

      Class spiInit = clusteredStateLoader.loadClass(SPI_INIT);
      contextControl = (DSOContextControl) spiInit.getConstructor(Object.class).newInstance(dsoContext);
      isInitialized = true;
      join(tunneledMBeanDomains);
      setSecretHackOMFG();
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  private void setSecretHackOMFG() {
    try {
      // Let's make sure we even care about security stuff at all...
      // if so, then check if the thing that cares is the BM Connector's TkSecurityManager
      appClassLoader.loadClass("com.terracotta.management.keychain.KeyChain");
      final Class<?> omfg = appClassLoader.loadClass("net.sf.ehcache.thrift.server.tc.TkSecurityManager");
      final Field secret = omfg.getDeclaredField("SECRET");
      if(secret.getType() == byte[].class) {
        secret.setAccessible(true);
        Class dsoContextClass = clusteredStateLoader.loadClass(DSO_CONTEXT_IMPL);
        Method method = dsoContextClass.getMethod("getSecret");
        secret.set(null, method.invoke(dsoContext));
      }
    } catch (ClassNotFoundException e) {
      // That's fine, moving on
    } catch (Throwable t) {
      //
    }
  }

  @Override
  public Object getPlatformService() {
    try {
      Class dsoContextClass = clusteredStateLoader.loadClass(DSO_CONTEXT_IMPL);
      Method method = dsoContextClass.getMethod("getPlatformService");
      return method.invoke(dsoContext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isDedicatedClient() {
    return true;
  }

  @Override
  public synchronized void join(Set<String> tunnelledMBeanDomainsParam) throws ClientShutdownException {
    if (shutdown) throw new ClientShutdownException();
    refCount.incrementAndGet();
    if (isInitialized) {
      contextControl.activateTunnelledMBeanDomains(tunnelledMBeanDomainsParam);
    } else {
      if (tunnelledMBeanDomainsParam != null) {
        this.tunneledMBeanDomains.addAll(tunnelledMBeanDomainsParam);
      }
    }
  }

  @Override
  public <T> T instantiate(String className, Class[] cstrArgTypes, Object[] cstrArgs) throws Exception {
    try {
    Class clazz = clusteredStateLoader.loadClass(className);
    Constructor cstr = clazz.getConstructor(cstrArgTypes);
    return (T) cstr.newInstance(cstrArgs);
    } catch (InvocationTargetException e) {
      Throwable targetEx = e.getTargetException();
      throw (targetEx instanceof ToolkitRuntimeException) ? (ToolkitRuntimeException) targetEx
          : new ToolkitRuntimeException(targetEx);
    }
  }

  @Override
  public Class loadClass(String className) throws ClassNotFoundException {
    return clusteredStateLoader.loadClass(className);
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

  private static boolean isEmbeddedEhcacheRequired() {
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

  private ClusteredStateLoader createClusteredStateLoader(ClassLoader appLoader) {
    ClusteredStateLoader loader = null;
    URL devmodeUrl = DevmodeClusteredStateLoader.devModeResource();
    if (devmodeUrl != null) {
      loader = new DevmodeClusteredStateLoader(devmodeUrl, appClassLoader, isEmbeddedEhcacheRequired());
    } else {
      loader = new ClusteredStateLoaderImpl(appClassLoader, isEmbeddedEhcacheRequired());
    }
    loader.addExtraClass(SpiInit.class.getName(), getClassBytes(SpiInit.class));
    loader.addExtraClass(StandaloneL1Boot.class.getName(), getClassBytes(StandaloneL1Boot.class));
    return loader;
  }

  private static boolean isRequestingSecuredEnv(final String tcConfig) {
    return tcConfig.contains("@");
  }

  private static Object newSecretProviderDelegate(final ClassLoader loader, final Object backEnd) {
    try {
      Class<?> customClass = Class.forName(SECRET_PROVIDER_CLASS);
      Class<?> tkClass = loader.loadClass(EE_SECRET_DELEGATE);
      return tkClass.getConstructor(customClass).newInstance(backEnd);
    } catch (Exception e) {
      String message;
      if (backEnd != null) {
        message = "Couldn't wrap the custom impl. " + backEnd.getClass().getName() + " in an instance of "
                  + EE_SECRET_DELEGATE;
      } else {
        message = "Couldn't fetch keychain password from the console";
      }
      throw new RuntimeException(message, e);
    }
  }

  @Override
  public boolean isOnline() {
    return contextControl.isOnline();
  }

  @Override
  public boolean isInitialized() {
    return isInitialized;
  }
}
