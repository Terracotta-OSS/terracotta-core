/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Annotation for after returning advice
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface AfterReturning {
  /**
   * The pointcut expression to bind, when no type is specified for the returned value
   */
  String value();

  /**
   * The pointcut expression to bind, when a type is specified for the returned value
   */
  String pointcut();

  /**
   * The type pattern for the returned value
   */
  String type();
}
