/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Interface for the field info implementations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface FieldInfo extends MemberInfo {
  /**
   * Returns the type.
   *
   * @return the type
   */
  ClassInfo getType();
}