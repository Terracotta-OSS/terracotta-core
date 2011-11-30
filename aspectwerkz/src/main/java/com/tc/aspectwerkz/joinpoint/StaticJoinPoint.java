/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import com.tc.aspectwerkz.joinpoint.management.JoinPointType;

/**
 * Implements the join point concept, e.g. defines a well defined point in the program flow.
 * <p/>
 * Provides access to only static data, is therefore much more performant than the usage of the {@link
 * com.tc.aspectwerkz.joinpoint.JoinPoint} interface.
 * <p/>
 * Note that it is possible to call proceed() on a StaticJoinPoint instance. The optimization comes from the fact
 * that StaticJoinPoint does not host RTTI information (caller, callee instances and args). It can be used with
 * pcd "args()", "this()" and "target()".
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface StaticJoinPoint {
  public static final String METHOD_EXECUTION = "METHOD_EXECUTION";
  public static final String METHOD_CALL = "METHOD_CALL";
  public static final String CONSTRUCTOR_EXECUTION = "CONSTRUCTOR_EXECUTION";
  public static final String CONSTRUCTOR_CALL = "CONSTRUCTOR_CALL";
  public static final String FIELD_SET = "FIELD_SET";
  public static final String FIELD_GET = "FIELD_GET";
  public static final String HANDLER = "HANDLER";
  public static final String STATIC_INITIALIZATION = "STATIC_INITIALIZATION";

  /**
   * Walks through the pointcuts and invokes all its advices. When the last advice of the last pointcut has been
   * invoked, the original method is invoked. Is called recursively.
   *
   * @return the result from the next invocation
   * @throws Throwable
   */
  Object proceed() throws Throwable;

  /**
   * Returns metadata matchingn a specfic key.
   *
   * @param key the key to the metadata
   * @return the value
   */
  Object getMetaData(Object key);

  /**
   * Adds metadata.
   *
   * @param key   the key to the metadata
   * @param value the value
   */
  void addMetaData(Object key, Object value);

  /**
   * Returns the signature for the join point.
   *
   * @return the signature
   */
  Signature getSignature();

  /**
   * Returns the caller class.
   *
   * @return the caller class
   */
  Class getCallerClass();

  /**
   * Returns the callee class.
   *
   * @return the target class
   */
  Class getCalleeClass();

  /**
   * Returns the callee class.
   *
   * @return the target class
   * @deprecated use getCalleeClass() instead
   */
  Class getTargetClass();

  /**
   * Returns the join point type.
   *
   * @return the type
   */
  JoinPointType getType();

  /**
   * Returns the enclosing static joinpoint.
   *
   * @return the enclosing static joinpoint
   */
  EnclosingStaticJoinPoint getEnclosingStaticJoinPoint();
}