/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
