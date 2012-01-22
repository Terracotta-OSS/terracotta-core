/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import java.lang.reflect.Constructor;

/**
 * Interface for the constructor RTTI (Runtime Type Information).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface ConstructorRtti extends CodeRtti {
  /**
   * Returns the constructor.
   *
   * @return the constructor
   */
  public Constructor getConstructor();

}
