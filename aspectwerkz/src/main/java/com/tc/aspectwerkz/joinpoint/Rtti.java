/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

/**
 * Holds static and reflective information about the join point (Runtime Type Information).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
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
