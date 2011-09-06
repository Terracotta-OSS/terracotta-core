/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Mixin annotation
 * Annotate the mixin implementation class
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 */
public interface Mixin {
  /**
   * Pointcut the mixin applies to (within / hasMethod / hasField)
   * When used, all others elements are assumed to their default value
   */
  public String value();

  /**
   * Pointcut the mixin applies to (within / hasMethod / hasField)
   * Used when deploymentModel / isTransient is specified
   */
  public String pointcut();

  /**
   * Mixin deployment model.
   * Defaults to "perInstance". Only "perClass" and "perInstance" are supported for now
   *
   * @org.codehaus.backport175.DefaultValue("perInstance")
   * @see com.tc.aspectwerkz.DeploymentModel
   */
  public String deploymentModel();

  /**
   * True if mixin should behave as transient and not be serialized alongside the class it is introduced to.
   * Defaults to false.
   *
   * @org.codehaus.backport175.DefaultValue(false)
   */
  public boolean isTransient();
}
