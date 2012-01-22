/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
