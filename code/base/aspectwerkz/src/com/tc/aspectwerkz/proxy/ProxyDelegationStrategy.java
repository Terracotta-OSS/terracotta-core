/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.proxy;


import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.exception.WrappedRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Get proxy classes from target classes that implement target interfaces and weaves in all matching aspects deployed in
 * the class loader and defined by the <code>META-INF/aop.xml</code> file.
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
 */
public class ProxyDelegationStrategy {

  /**
   * Suffix for proxy class name. A UUID is further suffixed.
   */
  public static final String PROXY_SUFFIX = "$$ProxiedByAWDelegation$$";

  /**
   * Cache for the compiled proxy classes. Implemented interfaces classes are composite key.
   */
  private final static Map PROXY_CLASS_CACHE = new WeakHashMap();

  /**
   * Compile or retrieve from cache a delegation proxy for the given interfaces.
   *
   * @param proxyName
   * @param interfaces
   * @param useCache
   * @param makeAdvisable
   * @return
   */
  static Class getProxyClassFor(String proxyName,
                                Class[] interfaces,
                                boolean useCache,
                                boolean makeAdvisable,
                                final SystemDefinition definition) {
    final Class proxyClass;
    if (!useCache) {
      proxyClass = getNewProxyClassFor(
          proxyName,
          interfaces,
          makeAdvisable,
          definition);
    } else {
      CompositeClassKey key = new CompositeClassKey(interfaces);
      synchronized (PROXY_CLASS_CACHE) {
        Object cachedProxyClass = PROXY_CLASS_CACHE.get(key);
        if (cachedProxyClass != null) {
          return (Class) cachedProxyClass;
        }
        proxyClass = getNewProxyClassFor(
                proxyName,
                interfaces,
                makeAdvisable,
                definition);
        PROXY_CLASS_CACHE.put(key, proxyClass);
      }
    }
    ProxyCompilerHelper.compileJoinPoint(proxyClass, definition);
    return proxyClass;
 }

  /**
   * Create a delegation proxy or retrieve it from cache and instantiate it, using the given implementations. <p/> Each
   * implementation must implement the respective given interface.
   *
   * @param interfaces
   * @param implementations
   * @param useCache
   * @param makeAdvisable
   * @return
   */
  static Object newInstance(final Class[] interfaces,
                            final Object[] implementations,
                            final boolean useCache,
                            final boolean makeAdvisable,
                            final SystemDefinition definition) {
    if (!implementsRespectively(interfaces, implementations)) {
      throw new RuntimeException(
              "Given implementations not consistents with given interfaces");
    }
    Class proxy = getProxyClassFor(
            definition.getUuid(),
            interfaces,
            useCache,
            makeAdvisable,
            definition
    );
    try {
      return proxy.getConstructor(interfaces).newInstance(implementations);
    } catch (InvocationTargetException t) {
      throw new WrappedRuntimeException(t.getCause());
    } catch (Throwable t) {
      throw new WrappedRuntimeException(t);
    }
  }

  /**
   * Return true if each implementation implement the respective given interface.
   *
   * @param interfaces
   * @param implementations
   * @return
   */
  private static boolean implementsRespectively(final Class[] interfaces,
                                                final Object[] implementations) {
    if (interfaces.length != implementations.length) {
      return false;
    }
    for (int i = 0; i < interfaces.length; i++) {
      if (!interfaces[i].isAssignableFrom(implementations[i].getClass())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compile a new proxy class and attach it to the lowest shared classloader of the given interfaces (as for JDK
   * proxy).
   *
   * @param proxyName
   * @param interfaces
   * @param makeAdvisable
   * @return
   */
  private static Class getNewProxyClassFor(String proxyName,
                                           Class[] interfaces,
                                           boolean makeAdvisable,
                                           final SystemDefinition definition) {
    ClassLoader loader = getLowestClassLoader(interfaces);
    if (makeAdvisable) {
      Proxy.makeProxyAdvisable(proxyName, loader, definition);
    }
    final byte[] bytes = ProxyDelegationCompiler.compileProxyFor(loader, interfaces, proxyName);
    return ProxyCompilerHelper.weaveAndDefineProxyClass(
            bytes,
            loader,
            proxyName,
            definition
    );
  }

  /**
   * Returns the lowest (childest) shared classloader or fail it detects parallel hierarchies.
   *
   * @param classes
   * @return
   */
  private static ClassLoader getLowestClassLoader(Class[] classes) {
    ClassLoader loader = classes[0].getClassLoader();
    for (int i = 1; i < classes.length; i++) {
      Class other = classes[i];
      if (SystemDefinitionContainer.isChildOf(other.getClassLoader(), loader)) {
        loader = other.getClassLoader();
      } else if (SystemDefinitionContainer.isChildOf(loader, other.getClassLoader())) {
        // loader is fine
      } else {
        throw new RuntimeException("parallel classloader hierarchy not supported");
      }
    }
    return loader;
  }

  /**
   * A composite key for the proxy cache.
   */
  private static class CompositeClassKey {
    private final Class[] m_interfaces;

    CompositeClassKey(Class[] interfaces) {
      m_interfaces = interfaces;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CompositeClassKey)) return false;

      final CompositeClassKey compositeClassKey = (CompositeClassKey) o;

      if (!Arrays.equals(m_interfaces, compositeClassKey.m_interfaces)) return false;

      return true;
    }

    public int hashCode() {
      int result = 1;
      for (int i = 0; i < m_interfaces.length; i++) {
        result = 31 * result + m_interfaces[i].hashCode();
      }
      return result;
    }
  }

}
