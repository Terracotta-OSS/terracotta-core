/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

/**
 * Interface for the code RTTI (Runtime Type Information).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface CodeRtti extends MemberRtti {
  /**
   * Returns the exception types declared by the code block.
   *
   * @return the exception types
   */
  Class[] getExceptionTypes();

  /**
   * Returns the parameter types.
   *
   * @return the parameter types
   */
  Class[] getParameterTypes();

  /**
   * Returns the values of the parameters.
   *
   * @return the values of the parameters
   */
  Object[] getParameterValues();

  /**
   * @param parameterValues
   * @TODO remove in 2.0
   * <p/>
   * Sets the values of the parameters.
   */
  void setParameterValues(Object[] parameterValues);
}