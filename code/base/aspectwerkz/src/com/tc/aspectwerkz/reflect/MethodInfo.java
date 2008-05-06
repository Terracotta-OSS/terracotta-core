/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Interface for the method info implementations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface MethodInfo extends MemberInfo {
  /**
   * Returns the return type.
   *
   * @return the return type
   */
  ClassInfo getReturnType();

  /**
   * Returns the parameter types.
   *
   * @return the parameter types
   */
  ClassInfo[] getParameterTypes();

  /**
   * Returns the parameter names as they appear in the source code.
   * This information is available only when class are compiled with javac -g (debug info), but is required
   * for Aspect that are using args() and target()/this() bindings.
   * <p/>
   * It returns null if not available.
   *
   * @return
   */
  String[] getParameterNames();

  /**
   * Returns the exception types.
   *
   * @return the exception types
   */
  ClassInfo[] getExceptionTypes();

}