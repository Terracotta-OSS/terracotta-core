/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Annotation for Aspect (optional)
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface Aspect {
  /**
   * Deployment model, when no aspect name is specified
   *
   * @org.codehaus.backport175.DefaultValue("perJVM")
   */
  String value();

  /**
   * Deployment model, when aspect name is specified
   *
   * @org.codehaus.backport175.DefaultValue("perJVM")
   */
  String deploymentModel();

  /**
   * Aspect name (defaults to fully qualified name of aspect class)
   */
  String name();
}
