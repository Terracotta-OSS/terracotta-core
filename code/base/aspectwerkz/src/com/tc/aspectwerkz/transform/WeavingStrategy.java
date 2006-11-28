/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.transform;


/**
 * Interface that all the weaving strategy implementations must implement.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface WeavingStrategy {

  /**
   * @param className
   * @param context
   */
  public abstract void transform(final String className, final InstrumentationContext context);
}