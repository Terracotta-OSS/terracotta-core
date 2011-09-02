/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
