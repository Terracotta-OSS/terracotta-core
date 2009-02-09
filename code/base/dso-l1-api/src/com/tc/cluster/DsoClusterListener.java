/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

public interface DsoClusterListener {
  public void nodeJoined(DsoClusterEvent event);

  public void nodeLeft(DsoClusterEvent event);

  public void operationsEnabled(DsoClusterEvent event);

  public void operationsDisabled(DsoClusterEvent event);
}