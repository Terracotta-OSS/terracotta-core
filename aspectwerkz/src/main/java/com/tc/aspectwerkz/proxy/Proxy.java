/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.proxy;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.DefinitionParserHelper;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.intercept.AdvisableImpl;

/**
 * Facade for Proxy service. Proxy are exposed to the weaver upon compilation, and can be made Advisable as well. <p/>
 * We provide 2 proxy strategy: one by subclassing a non final concrete class, and thus having the proxy delegate to the
 * real implementation thru super.xxx(..) calls, and one by delegating to N implementations of N interfaces. <p/> Proxy
 * strategy provide a cache mechanism if ones wants to cache the compiled proxy. <p/> Pointcut to match delegating
 * proxies should use a "+" as for regular subtype matching. <p/> Pointcut to match subclassing proxies don't need to
 * use a "+" - precisely to avoid pointcut refactoring to match them.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas BonÂr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class Proxy {

  /**
   * Creates a new subclassing proxy instance based for the class specified and instantiates it using its default
   * no-argument constructor. <p/> The proxy will be cached and non-advisable.
   *
   * @param clazz the target class to make a proxy for
   * @return the proxy instance
   */
  public static Object newInstance(final Class clazz,
                                   final SystemDefinition definition) {
    return ProxySubclassingStrategy.newInstance(clazz, definition);
  }

  /**
   * Creates a new subclassing proxy instance for the class specified and instantiates it using the constructor matching
   * the argument type array specified. <p/> The proxy will be cached and non-advisable.
   *
   * @param clazz          the target class to make a proxy for
   * @param argumentTypes  the argument types matching the signature of the constructor to use when instantiating the
   *                       proxy
   * @param argumentValues the argument values to use when instantiating the proxy
   * @return the proxy instance
   */
  public static Object newInstance(final Class clazz,
                                   final Class[] argumentTypes,
                                   final Object[] argumentValues,
                                   final SystemDefinition definition) {
    return ProxySubclassingStrategy.newInstance(
            clazz, argumentTypes, argumentValues, definition);
  }

  /**
   * Creates a new subclassing proxy instance based for the class specified and instantiates it using its default
   * no-argument constructor.
   *
   * @param clazz         the target class to make a proxy for
   * @param useCache      true if a cached instance of the proxy classed should be used
   * @param makeAdvisable true if the proxy class should implement the <code>Advisable</code> interface, e.g. be
   *                      prepared for programmatic, runtime, per instance hot deployement of advice
   * @return the proxy instance
   */
  public static Object newInstance(final Class clazz,
                                   final boolean useCache,
                                   final boolean makeAdvisable,
                                   final SystemDefinition definition) {
    return ProxySubclassingStrategy.newInstance(
            clazz, useCache, makeAdvisable, definition);
  }

  /**
   * Creates a new subclassing proxy instance for the class specified and instantiates it using the constructor matching
   * the argument type array specified.
   *
   * @param clazz          the target class to make a proxy for
   * @param argumentTypes  the argument types matching the signature of the constructor to use when instantiating the
   *                       proxy
   * @param argumentValues the argument values to use when instantiating the proxy
   * @param useCache       true if a cached instance of the proxy classed should be used
   * @param makeAdvisable  true if the proxy class should implement the <code>Advisable</code> interface, e.g. be
   *                       prepared for programmatic, runtime, per instance hot deployement of advice
   * @return the proxy instance
   */
  public static Object newInstance(final Class clazz,
                                   final Class[] argumentTypes,
                                   final Object[] argumentValues,
                                   final boolean useCache,
                                   final boolean makeAdvisable,
                                   final SystemDefinition definition) {
    return ProxySubclassingStrategy.newInstance(
            clazz,
            argumentTypes,
            argumentValues,
            useCache,
            makeAdvisable,
            definition);
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
  public static Object newInstance(final Class[] interfaces,
                                   final Object[] implementations,
                                   final boolean useCache,
                                   final boolean makeAdvisable,
                                   final SystemDefinition definition) {
    return ProxyDelegationStrategy.newInstance(
            interfaces,
            implementations,
            useCache,
            makeAdvisable,
            definition);
  }

  /**
   * Enhances the proxy class with the Advisable mixin, to allow runtime per instance additions of interceptors. Simply
   * register in the system definition.
   *
   * @param proxyClassName
   * @param loader
   */
  static void makeProxyAdvisable(final String proxyClassName,
                                 ClassLoader loader,
                                 final SystemDefinition definition) {
    // changes occurs in the virtual definition only
    String withinPointcut = "within(" + proxyClassName.replace('/', '.') + ')';
    definition.addMixinDefinition(DefinitionParserHelper.createAndAddMixinDefToSystemDef(AdvisableImpl.CLASS_INFO,
            withinPointcut,
            DeploymentModel.PER_INSTANCE,
            false,
            definition));
    DefinitionParserHelper.createAndAddAdvisableDef('(' + withinPointcut + " && execution(!static * *.*(..)))",
            definition);
  }
}
                 
