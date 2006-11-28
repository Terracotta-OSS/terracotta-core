/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
