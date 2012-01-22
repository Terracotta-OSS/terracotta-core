/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Annotation for after throwing advice
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface AfterThrowing {
  /**
   * The pointcut expression to bind, when no type is specified for the throwned value
   */
  String value();

  /**
   * The pointcut expression to bind, when a type is specified for the throwned value
   */
  String pointcut();

  /**
   * The type pattern for the returned value
   */
  String type();
}
