/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.lang.reflect.Method;

/**
 * Interface for the method signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface MethodSignature extends CodeSignature {
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
}