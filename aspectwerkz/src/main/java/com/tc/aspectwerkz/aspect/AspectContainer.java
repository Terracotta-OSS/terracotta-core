/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.aspect;

/**
 * Interface that all aspect container implementations must implement.
 * <p/>
 * An implementation can have a no-arg constructor or a constructor with 5 args:
 * Class aspectClass, ClassLoader aopSystemClassLoader, String systemUuid, String aspectQualifiedName (composed of systemUuid and given aspect name
 * in the aop.xml that defines it), Map (of parameters declared in the aop.xml for this aspect declaration).
 * <p/>
 * An Aspect can have no aspect container at all. In such a case, the aspect is simply created using its no-arg
 * constructor (thus in a faster way).
 * <p/>
 * Note that the container will only be invoked for aspect instantiation, but not for aspect lookup (association).
 * The lookup is handled by the deployment model semantics and thus by the framework.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface AspectContainer {

  /**
   * Creates a new perJVM cross-cutting instance, if it already exists then return it.
   *
   * @return the cross-cutting instance
   */
  Object aspectOf();

  /**
   * Creates a new perClass cross-cutting instance, if it already exists then return it.
   *
   * @param klass
   * @return the cross-cutting instance
   */
  Object aspectOf(Class klass);

  /**
   * Creates a new perInstance cross-cutting instance, if it already exists then return it.
   *
   * @param instance
   * @return the cross-cutting instance
   */
  Object aspectOf(Object instance);

  /**
   * Creates a new perThread cross-cutting instance, if it already exists then return it.
   *
   * @param thread the thread for the aspect
   * @return the cross-cutting instance
   */
  Object aspectOf(Thread thread);

}
