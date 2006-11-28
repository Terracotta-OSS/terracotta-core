/*
 * Created on Jan 12, 2004
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