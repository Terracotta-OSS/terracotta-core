/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
