/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.io.Serializable;

/**
 * Provides static and reflective information about the join point.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface Signature extends Serializable {
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

  /**
   * Returns the name (f.e. name of method of field).
   *
   * @return
   */
  String getName();

}