/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.impl;

import java.lang.reflect.Method;

/**
 * Contains a pair of the original method and the wrapper method if such a method exists.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class MethodTuple /* implements Serializable */ {
  private final Method m_wrapperMethod;

  private final Method m_originalMethod;

  private final Class m_declaringClass;

  /**
   * @param wrapperMethod
   * @param originalMethod
   */
  public MethodTuple(Method wrapperMethod, Method originalMethod) {
    if (originalMethod == null) {
      originalMethod = wrapperMethod;
    }
    if (wrapperMethod.getDeclaringClass() != originalMethod.getDeclaringClass()) {
      throw new RuntimeException(
              wrapperMethod.getName()
                      + " and "
                      + originalMethod.getName()
                      + " does not have the same declaring class"
      );
    }
    m_declaringClass = wrapperMethod.getDeclaringClass();
    m_wrapperMethod = wrapperMethod;
    m_wrapperMethod.setAccessible(true);
    m_originalMethod = originalMethod;
    m_originalMethod.setAccessible(true);
  }

  public boolean isWrapped() {
    return m_originalMethod != null;
  }

  public Class getDeclaringClass() {
    return m_declaringClass;
  }

  public Method getWrapperMethod() {
    return m_wrapperMethod;
  }

  public Method getOriginalMethod() {
    return m_originalMethod;
  }

  public String getName() {
    return m_wrapperMethod.getName();
  }
}
