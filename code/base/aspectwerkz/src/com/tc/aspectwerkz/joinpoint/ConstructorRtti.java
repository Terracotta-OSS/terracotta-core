/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.lang.reflect.Constructor;

/**
 * Interface for the constructor RTTI (Runtime Type Information).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface ConstructorRtti extends CodeRtti {
  /**
   * Returns the constructor.
   *
   * @return the constructor
   */
  public Constructor getConstructor();

}