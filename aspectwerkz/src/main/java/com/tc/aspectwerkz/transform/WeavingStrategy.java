/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform;


/**
 * Interface that all the weaving strategy implementations must implement.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface WeavingStrategy {

  /**
   * @param className
   * @param context
   */
  public abstract void transform(final String className, final InstrumentationContext context);
}