/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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