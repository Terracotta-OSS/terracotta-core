/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

/**
 * Holds static and reflective information about the join point (Runtime Type Information).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface Rtti {
  /**
   * Returns the name (f.e. name of method of field).
   *
   * @return
   */
  String getName();

  /**
   * Returns the target instance.
   *
   * @return the target instance
   */
  Object getTarget();

  /**
   * Returns the instance currently executing (this).
   *
   * @return the instance currently executing (this)
   */
  Object getThis();

  /**
   * Returns the declaring class.
   *
   * @return the declaring class
   */
  Class getDeclaringType();

  /**
   * Returns the modifiers for the signature. <p/>Could be used like this:
   * <p/>
   * <pre>
   * boolean isPublic = java.lang.reflect.Modifier.isPublic(signature.getModifiers());
   * </pre>
   *
   * @return the mofifiers
   */
  int getModifiers();

  Rtti cloneFor(Object targetInstance, Object thisInstance);
}