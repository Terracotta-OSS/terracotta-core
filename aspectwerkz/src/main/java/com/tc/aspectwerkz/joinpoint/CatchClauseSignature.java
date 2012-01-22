/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

/**
 * Interface for the catch clause signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 *         TODO rename to HandlerSignature in 2.0
 */
public interface CatchClauseSignature extends Signature {
  /**
   * Returns the parameter type.
   *
   * @return the parameter type
   */
  Class getParameterType();
}
