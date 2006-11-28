/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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