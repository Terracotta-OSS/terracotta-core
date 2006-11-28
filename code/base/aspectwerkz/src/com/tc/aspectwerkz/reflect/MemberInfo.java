/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Marker interface for the member info classes (field and method).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface MemberInfo extends ReflectionInfo {
  /**
   * Returns the declaring type.
   *
   * @return the declaring type
   */
  ClassInfo getDeclaringType();
}
