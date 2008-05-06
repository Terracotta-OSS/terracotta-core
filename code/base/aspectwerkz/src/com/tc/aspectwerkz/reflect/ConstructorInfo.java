/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Interface for the constructor info implementations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface ConstructorInfo extends MemberInfo {
  /**
   * Returns the parameter types.
   *
   * @return the parameter types
   */
  ClassInfo[] getParameterTypes();

  /**
   * Returns the exception types.
   *
   * @return the exception types
   */
  ClassInfo[] getExceptionTypes();
}