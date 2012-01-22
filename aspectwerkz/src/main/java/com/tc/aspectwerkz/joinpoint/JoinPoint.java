/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

/**
 * Implements the join point concept, e.g. defines a well defined point in the program flow.
 * <p/>
 * Provides access to runtime type information (RTTI), is therefore significantly <b>slower</b>
 * than the usage of the {@link com.tc.aspectwerkz.joinpoint.StaticJoinPoint} interface.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface JoinPoint extends StaticJoinPoint {

  /**
   * Returns the callee instance.
   *
   * @return the callee instance
   */
  Object getCallee();

  /**
   * Returns the caller instance.
   *
   * @return the caller instance
   */
  Object getCaller();

  /**
   * Returns the 'this' instance (the one currently executing).
   *
   * @return 'this'
   */
  Object getThis();

  /**
   * Returns the target instance. If the join point is executing in a static context it returns null.
   *
   * @return the target instance
   */
  Object getTarget();

  /**
   * Returns the JoinPoint RTTI
   *
   * @return the Rtti
   */
  Rtti getRtti();
}
