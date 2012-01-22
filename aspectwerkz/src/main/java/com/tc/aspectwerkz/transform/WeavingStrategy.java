/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
