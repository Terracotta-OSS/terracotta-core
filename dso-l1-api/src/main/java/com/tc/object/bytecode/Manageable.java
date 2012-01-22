/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.TCObject;

/**
 * Manageable interface for locks, etc.
 */
public interface Manageable {

  /**
   * Pass in TCObject peer for this object
   * 
   * @param t TCObject
   */
  public void __tc_managed(TCObject t);

  /**
   * Get TCObject for this object
   * 
   * @return The TCObject
   */
  public TCObject __tc_managed();

  /**
   * Check whether this object is managed
   * 
   * @return True if managed
   */
  public boolean __tc_isManaged();

}
