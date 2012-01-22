/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

/**
 * Interface for the catch clause RTTI (Runtime Type Information).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @TODO rename to HandlerRtti
 */
public interface CatchClauseRtti extends Rtti {
  /**
   * Returns the parameter type.
   *
   * @return the parameter type
   */
  Class getParameterType();

  /**
   * Returns the value of the parameter.
   *
   * @return the value of the parameter
   */
  Object getParameterValue();
}
