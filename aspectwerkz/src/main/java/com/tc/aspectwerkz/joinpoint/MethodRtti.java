/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.lang.reflect.Method;

/**
 * Interface for the method RTTI (Runtime Type Information).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface MethodRtti extends CodeRtti {
  /**
   * Returns the method.
   *
   * @return the method
   */
  Method getMethod();

  /**
   * Returns the return type.
   *
   * @return the return type
   */
  Class getReturnType();

  /**
   * Returns the value of the return type.
   *
   * @return the value of the return type
   */
  Object getReturnValue();
}