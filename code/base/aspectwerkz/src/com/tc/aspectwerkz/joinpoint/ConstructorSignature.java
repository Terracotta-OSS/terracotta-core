/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.lang.reflect.Constructor;

/**
 * Interface for the constructor signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface ConstructorSignature extends CodeSignature {
  /**
   * Returns the constructor.
   *
   * @return the constructor
   */
  public Constructor getConstructor();
}