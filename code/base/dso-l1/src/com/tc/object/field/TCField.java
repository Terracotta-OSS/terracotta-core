/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.field;

import com.tc.object.TCClass;

/**
 * @author orion
 */
public interface TCField {
  /**
   * Returns the TCClass which declared this field.
   *
   * @return
   */
  public TCClass getDeclaringTCClass();

  public boolean isFinal();

  public boolean isPortable();

  public boolean isArray();

  public boolean canBeReference();
  
  public String getName();
}