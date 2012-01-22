/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.proxy;


import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.exception.WrappedRuntimeException;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Get proxy classes from target classes and weaves in all matching aspects deployed in the class loader
 * and defined by the <code>META-INF/aop.xml</code> file.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class ProxySubclassingStrategy {

  /**
   * The suffix for the compiled proxy classes.
   */
  public static final String PROXY_SUFFIX = "$$ProxiedByAWSubclassing$$";

  /**
   * Cache for the compiled proxy classes. Target class is key.
   */
  private static final Map PROXY_CLASS_CACHE = new WeakHashMap();

  /**
   * Creates a new proxy instance based for the class specified and instantiates it using its default no-argument
   * constructor. <p/> The proxy will be cached and non-advisable.
   *
   * @param clazz the target class to make a proxy for
   * @return the proxy instance
   */
  static Object newInstance(
          final Class clazz, final SystemDefinition definition) {
    try {
      Class proxyClass = getProxyClassFor(clazz, true, false, definition);
      return proxyClass.newInstance();
    } catch (Throwable t) {
      throw new WrappedRuntimeException(t);
    }
  }

  /**
   * Creates a new proxy instance for the class specified and instantiates it using the constructor matching
   * the argument type array specified.
   * <p/>
   * The proxy will be cached and non-advisable.
   *
   * @param clazz          the target class to make a proxy for
   * @param argumentTypes  the argument types matching the signature of the constructor to use when instantiating the proxy
   * @param argumentValues the argument values to use when instantiating the proxy
   * @return the proxy instance
   */
  static Object newInstance(
          final Class clazz, final Class[] argumentTypes, final Object[] argumentValues,
          final SystemDefinition definition) {
    try {
      Class proxyClass = getProxyClassFor(clazz, true, false, definition);
      return proxyClass.getDeclaredConstructor(argumentTypes).newInstance(argumentValues);
    } catch (Throwable t) {
      throw new WrappedRuntimeException(t);
    }
  }

  /**
   * Creates a new proxy instance based for the class specified and instantiates it using its default no-argument
   * constructor.
   *
   * @param clazz         the target class to make a proxy for
   * @param useCache      true if a cached instance of the proxy classed should be used
   * @param makeAdvisable true if the proxy class should implement the <code>Advisable</code> interface,
   *                      e.g. be prepared for programmatic, runtime, per instance hot deployement of advice
   * @return the proxy instance
   */
  static Object newInstance(
          final Class clazz, final boolean useCache, final boolean makeAdvisable,
          final SystemDefinition definition) {
    try {
      Class proxyClass = getProxyClassFor(clazz, useCache, makeAdvisable, definition);
      return proxyClass.newInstance();
    } catch (Throwable t) {
      throw new WrappedRuntimeException(t);
    }
  }

  /**
   * Creates a new proxy instance for the class specified and instantiates it using the constructor matching
   * the argument type array specified.
   *
   * @param clazz          the target class to make a proxy for
   * @param argumentTypes  the argument types matching the signature of the constructor to use when instantiating the proxy
   * @param argumentValues the argument values to use when instantiating the proxy
   * @param useCache       true if a cached instance of the proxy classed should be used
   * @param makeAdvisable  true if the proxy class should implement the <code>Advisable</code> interface,
   *                       e.g. be prepared for programmatic, runtime, per instance hot deployement of advice
   * @return the proxy instance
   */
  static Object newInstance(
          final Class clazz, final Class[] argumentTypes, final Object[] argumentValues,
          final boolean useCache, final boolean makeAdvisable,
          final SystemDefinition definition) {
    try {
      Class proxyClass = getProxyClassFor(clazz, useCache, makeAdvisable, definition);
      return proxyClass.getDeclaredConstructor(argumentTypes).newInstance(argumentValues);
    } catch (Throwable t) {
      throw new WrappedRuntimeException(t);
    }
  }

  /**
   * Compiles and returns a proxy class for the class specified.
   *
   * @param clazz         the target class to make a proxy for
   * @param useCache      true if a cached instance of the proxy classed should be used
   * @param makeAdvisable true if the proxy class should implement the <code>Advisable</code> interface,
   *                      e.g. be prepared for programmatic, runtime, per instance hot deployement of advice
   * @return the proxy class
   */
  static Class getProxyClassFor(
          final Class clazz, final boolean useCache, final boolean makeAdvisable,
          final SystemDefinition definition) {

    // TODO - add support for proxying java.* classes
    if (clazz.getName().startsWith("java.")) {
      throw new RuntimeException(
              "can not create proxies from system classes (java.*)");
    }
    final Class proxyClass;
    if (!useCache) {
      proxyClass = getNewProxyClassFor(clazz, makeAdvisable, definition);
    } else {
      synchronized (PROXY_CLASS_CACHE) {
        Object cachedProxyClass = PROXY_CLASS_CACHE.get(clazz);
        if (cachedProxyClass != null) {
          return (Class) cachedProxyClass;
        }
        proxyClass = getNewProxyClassFor(clazz, makeAdvisable, definition);
        PROXY_CLASS_CACHE.put(clazz, proxyClass);
      }
    }
    ProxyCompilerHelper.compileJoinPoint(proxyClass, definition);
    return proxyClass;
  }

  /**
   * Compiles and returns a proxy class for the class specified.
   * No cache is used, but compiles a new one each invocation.
   *
   * @param clazz
   * @param makeAdvisable true if the proxy class should implement the <code>Advisable</code> interface,
   *                      e.g. be prepared for programmatic, runtime, per instance hot deployement of advice
   * @return the proxy class
   */
  private static Class getNewProxyClassFor(
          final Class clazz, final boolean makeAdvisable,
          final SystemDefinition definition) {
    ClassLoader loader = clazz.getClassLoader();
    String proxyClassName = definition.getUuid();
    if (makeAdvisable) {
      Proxy.makeProxyAdvisable(proxyClassName, loader, definition);
    }
    final byte[] bytes = ProxySubclassingCompiler.compileProxyFor(clazz, proxyClassName);
    return ProxyCompilerHelper.weaveAndDefineProxyClass(
            bytes, loader, proxyClassName.replace('/', '.'), definition);
  }

  /**
   * Returns a unique name for the proxy class.
   *
   * @param proxyClassName
   * @return the class name beeing proxied
   */
  static String getUniqueClassNameFromProxy(
          final String proxyClassName) {
    int index = proxyClassName.lastIndexOf(PROXY_SUFFIX);
    if (index > 0) {
      return proxyClassName.substring(0, index);
    } else {
      return null;
    }
  }
}
