/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Annotation for after finally advice
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface AfterFinally {
  /**
   * The pointcut expression to bind
   */
  String value();
}
