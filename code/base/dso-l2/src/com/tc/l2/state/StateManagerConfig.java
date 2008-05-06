/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

public interface StateManagerConfig {
  
  /**
   * @return election time in seconds
   */
  public int getElectionTimeInSecs();
}
