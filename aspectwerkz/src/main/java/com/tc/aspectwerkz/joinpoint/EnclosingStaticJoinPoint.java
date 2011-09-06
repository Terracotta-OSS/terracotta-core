/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import com.tc.aspectwerkz.joinpoint.management.JoinPointType;

/**
 * Implements the join point concept, e.g. defines a well defined point in the program flow.
 * <p/>
 * Provides access to only static data, is therefore much more performant than the usage of the {@link
 * com.tc.aspectwerkz.joinpoint.JoinPoint} interface.
 *
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public interface EnclosingStaticJoinPoint {
  /**
   * Returns the signature for the join point.
   *
   * @return the signature
   */
  Signature getSignature();

  /**
   * @return
   */
  JoinPointType getType();
}