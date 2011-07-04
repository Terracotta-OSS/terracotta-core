/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.connectivity;

/**
 * Invokes the method for an instance mapped to a specific handle.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface Invoker {
  /**
   * Invokes a specific method on the object mapped to the role specified.
   *
   * @param handle     the handle to the implementation class (class name, mapped name, UUID etc.)
   * @param methodName the name of the method
   * @param paramTypes the parameter types
   * @param args       the arguments to the method
   * @param context    the context with the users principal and credentials
   * @return the result from the invocation
   */
  public Object invoke(final String handle,
                       final String methodName,
                       final Class[] paramTypes,
                       final Object[] args,
                       final Object context);
}