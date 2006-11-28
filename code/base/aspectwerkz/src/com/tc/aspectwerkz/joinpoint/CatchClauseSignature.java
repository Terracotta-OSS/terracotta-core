/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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