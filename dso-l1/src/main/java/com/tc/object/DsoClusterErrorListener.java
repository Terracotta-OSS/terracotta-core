/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.object;

import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;

/**
 *
 * @author mscott
 */
public abstract class DsoClusterErrorListener implements DsoClusterListener {
//  nodeError needs to be implemented by subclass;

  @Override
  public void nodeRejoined(DsoClusterEvent event) {
    
  }

  @Override
  public void operationsDisabled(DsoClusterEvent event) {
    
  }

  @Override
  public void operationsEnabled(DsoClusterEvent event) {
    
  }

  @Override
  public void nodeLeft(DsoClusterEvent event) {
    
  }

  @Override
  public void nodeJoined(DsoClusterEvent event) {
    
  }
  
}
