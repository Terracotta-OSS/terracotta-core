/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

/**
 * Interface for the catch clause signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
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