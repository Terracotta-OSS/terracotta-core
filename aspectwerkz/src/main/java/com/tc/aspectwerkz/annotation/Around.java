/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Annotation for around advice
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface Around {
  /**
   * The pointcut expression to bind
   * TODO: defaults to "" for JAOO sample - might not be good to keep.
   */
  String value();
}
