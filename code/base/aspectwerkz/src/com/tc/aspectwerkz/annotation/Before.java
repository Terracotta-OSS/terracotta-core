/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Annotation for before advice
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface Before {
  /**
   * The pointcut expression to bind
   */
  String value();
}
