/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

/**
 * Annotation for pointcut
 * <p/>
 * TODO: rename to Pointcut and remove pointcut as field ??
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface Expression {
  /**
   * The pointcut expression
   */
  String value();
}
