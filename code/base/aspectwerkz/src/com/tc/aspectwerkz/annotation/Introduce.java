/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Introduce annotation for interface introduction
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 */
public interface Introduce {
  /**
   * The type pointcut (composition of within / hasField / hasMethod) the annotated field type
   * will be introduced to
   */
  public String value();
}
