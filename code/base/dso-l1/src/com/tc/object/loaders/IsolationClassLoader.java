/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.object.ClientObjectManager;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerImpl;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.bytecode.hook.impl.DSOContextImpl;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.tx.ClientTransactionManager;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

/**
 * DSO Class loader for internal testing. The main purpose of this loader is to force test classes to be be defined
 * specifically in this loader (and consequently within an isolated DSO context)
 */
public class IsolationClassLoader extends URLClassLoader implements NamedClassLoader {
  private static final ClassLoader    SYSTEM_LOADER = ClassLoader.getSystemClassLoader();

  private final Manager               manager;
  private final DSOClientConfigHelper config;
  private final HashMap               onLoadErrors;
  private final StandardClassProvider classProvider;

  public IsolationClassLoader(DSOClientConfigHelper config, PreparedComponentsFromL2Connection connectionComponents) {
    this(config, true, null, null, connectionComponents);
  }

  public IsolationClassLoader(DSOClientConfigHelper config, ClientObjectManager objectManager,
                              ClientTransactionManager txManager) {
    this(config, false, objectManager, txManager, null);
  }

  private IsolationClassLoader(DSOClientConfigHelper config, boolean startClient, ClientObjectManager objectManager,
                               ClientTransactionManager txManager,
                               PreparedComponentsFromL2Connection connectionComponents) {
    super(getSystemURLS(), null);
    this.config = config;
    this.classProvider = new StandardClassProvider();
    this.manager = createManager(startClient, objectManager, txManager, config, connectionComponents);
    this.onLoadErrors = new HashMap();
  }

  public void init() {
    manager.init();
    ClassProcessorHelper.setContext(this, DSOContextImpl.createContext(config, classProvider, manager));
  }

  private static URL[] getSystemURLS() {
    return ((URLClassLoader) SYSTEM_LOADER).getURLs();
  }

  private Manager createManager(boolean startClient, ClientObjectManager objectManager,
                                ClientTransactionManager txManager, DSOClientConfigHelper theConfig,
                                PreparedComponentsFromL2Connection connectionComponents) {
    classProvider.registerNamedLoader(this);
    return new ManagerImpl(startClient, objectManager, txManager, theConfig, classProvider, connectionComponents, false);
  }

  public void stop() {
    this.manager.stop();
  }

  public Class loadClass(String name) throws ClassNotFoundException {
    throwIfNeeded(name);

    if (name.startsWith("com.tc.")) {
      return SYSTEM_LOADER.loadClass(name);
    } else {
      return super.loadClass(name);
    }
  }

  private void throwIfNeeded(String name) throws ClassNotFoundException {
    // throw exception if one is registered
    final String t = (String)onLoadErrors.get(name);
    if (t != null) {
      ClassNotFoundException rv = new ClassNotFoundException(t);
      throw rv;
    }
  }

  public String __tc_getClassLoaderName() {
    return getClass().getName();
  }

  public void __tc_setClassLoaderName(String name) {
    throw new AssertionError();
  }

  /**
   * a ClassNotFoundException or NoClassDefFoundError with errorMessage will to be thrown referencing class className
   */
  public void throwOnLoad(String className, String errorMessage) {
    onLoadErrors.put(className, errorMessage);
  }

  protected Class findClass(String name) throws ClassNotFoundException {
    throwIfNeeded(name);
    return super.findClass(name);
  }
}
