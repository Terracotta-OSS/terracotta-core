/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Marker interface for the member info classes (field and method).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Boner </a>
 */
public interface MemberInfo extends ReflectionInfo {
  /**
   * Returns the declaring type.
   *
   * @return the declaring type
   */
  ClassInfo getDeclaringType();
}
