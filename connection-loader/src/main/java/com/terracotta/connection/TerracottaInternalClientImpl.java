/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import com.google.common.base.Throwables;
import com.tc.object.ClientEntityManager;
import com.tc.object.locks.ClientLockManager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

class TerracottaInternalClientImpl implements TerracottaInternalClient {

  private static final String CLIENT_HANDLE_IMPL                                               = "com.terracotta.connection.ClientHandleImpl";
  public static final String  SECRET_PROVIDER                                                  = "com.terracotta.express.SecretProvider";

  static class ClientShutdownException extends Exception {
    //
  }

  private final ClusteredStateLoader  clusteredStateLoader;
  private final AppClassLoader        appClassLoader;
  private final ClientCreatorCallable clientCreator;
  private final Set<String>           tunneledMBeanDomains = new HashSet<>();
  private volatile ClientHandle       clientHandle;
  private volatile boolean            shutdown             = false;
  private volatile boolean            isInitialized        = false;

  TerracottaInternalClientImpl(String tcConfig, boolean isUrlConfig, ClassLoader appLoader, boolean rejoinEnabled,
                               Set<String> tunneledMBeanDomains, String productId, Map<String, Object> env) {
    if (tunneledMBeanDomains != null) {
      this.tunneledMBeanDomains.addAll(tunneledMBeanDomains);
    }
    try {
      this.appClassLoader = new AppClassLoader(appLoader);
      this.clusteredStateLoader = createClusteredStateLoader();

      @SuppressWarnings("unchecked")
      Class<Callable<ClientCreatorCallable>> bootClass = (Class<Callable<ClientCreatorCallable>>) clusteredStateLoader.loadClass(CreateClient.class.getName());
      
      Constructor<Callable<ClientCreatorCallable>> cstr = bootClass.getConstructor(String.class, Boolean.TYPE, ClassLoader.class, Boolean.TYPE,
                                                     String.class, Map.class);

      Callable<ClientCreatorCallable> boot = cstr.newInstance(tcConfig, isUrlConfig, clusteredStateLoader, rejoinEnabled, productId, env);
      this.clientCreator = boot.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized void init() {
    if (isInitialized) { return; }

    try {
      Class<?> clientHandleImpl = clusteredStateLoader.loadClass(CLIENT_HANDLE_IMPL);
      clientHandle = (ClientHandle) clientHandleImpl.getConstructor(Object.class).newInstance(clientCreator.call());

      isInitialized = true;
      join(tunneledMBeanDomains);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public String getUuid() {
    return clientCreator.getUuid();
  }

  private synchronized void join(Set<String> tunnelledMBeanDomainsParam) throws ClientShutdownException {
    if (shutdown) throw new ClientShutdownException();

    if (isInitialized) {
      clientHandle.activateTunnelledMBeanDomains(tunnelledMBeanDomainsParam);
    } else {
      if (tunnelledMBeanDomainsParam != null) {
        this.tunneledMBeanDomains.addAll(tunnelledMBeanDomainsParam);
      }
    }
  }

  @Override
  public <T> T instantiate(String className, Class<?>[] cstrArgTypes, Object[] cstrArgs) throws Exception {
    try {
      @SuppressWarnings("unchecked")
      Class<T> clazz = (Class<T>) clusteredStateLoader.loadClass(className);
      
      Constructor<T> cstr = clazz.getConstructor(cstrArgTypes);
      return cstr.newInstance(cstrArgs);
    } catch (InvocationTargetException e) {
      Throwable targetEx = e.getTargetException();
      throw Throwables.propagate(targetEx);
    }
  }

  @Override
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    return clusteredStateLoader.loadClass(className);
  }

  @Override
  public synchronized boolean isShutdown() {
    return shutdown;
  }

  @Override
  public synchronized void shutdown() {
    shutdown = true;
    try {
      if (clientHandle != null) {
        clientHandle.shutdown();
      }
    } finally {
      clientHandle = null;
      appClassLoader.clear();
    }
  }

  private byte[] getClassBytes(Class<?> klass) {
    ClassLoader loader = getClass().getClassLoader();
    String res = klass.getName().replace('.', '/').concat(".class");
    try {
      return Util.extract(loader.getResourceAsStream(res));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ClusteredStateLoader createClusteredStateLoader() {
    ClusteredStateLoader loader = new ClusteredStateLoaderImpl(appClassLoader);
    loader.addExtraClass(ClientHandleImpl.class.getName(), getClassBytes(ClientHandleImpl.class));
    loader.addExtraClass(CreateClient.class.getName(), getClassBytes(CreateClient.class));
    loader.addExtraClass(CreateClient.ClientCreatorCallableImpl.class.getName(),
                         getClassBytes(CreateClient.ClientCreatorCallableImpl.class));
    return loader;
  }

  @Override
  public boolean isInitialized() {
    return isInitialized;
  }
  
  @Override
  public ClientEntityManager getClientEntityManager() {
    return clientHandle.getClientEntityManager();
  }
  
  @Override
  public ClientLockManager getClientLockManager() {
    return clientHandle.getClientLockManager();
  }
}
