/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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