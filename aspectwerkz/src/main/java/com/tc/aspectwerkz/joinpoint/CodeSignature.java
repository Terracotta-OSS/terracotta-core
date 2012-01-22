/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

/**
 * Interface for the code signature (method and constructor).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface CodeSignature extends MemberSignature {
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
}
