/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.net.utils.L2Utils;
import com.tc.util.Assert;


/**
 * Special control structure to make sure sync and lifecycle operations
 * are properly controlled through the sync process
 */
public final class ManagedEntitySyncInterop {
//  single threaded lifecycle
  private int lifecycleOccuring;
//  multiple sync threads possible
  private int syncsReadyToStart;
  private int syncsStarted;
  private int syncsFinishing;

  public ManagedEntitySyncInterop() {
  }

  public synchronized void startSync() {
    try {
      while (lifecycleOccuring > 0) {
        this.wait();
      }
      syncsReadyToStart += 1;
    } catch (InterruptedException ie) {
      L2Utils.handleInterrupted(null, ie);
    }
  }

  public synchronized void abortSync() {
    try {
      while (lifecycleOccuring > 0) {
        this.wait();
      }
      syncsReadyToStart -= 1;
    } catch (InterruptedException ie) {
      L2Utils.handleInterrupted(null, ie);
    }
  }
  
  public synchronized void startLifecycle() {
    try {
      while (syncsStarted > 0 || syncsReadyToStart > 0) {
        this.wait();
      }
      lifecycleOccuring += 1;
    } catch (InterruptedException ie) {
      L2Utils.handleInterrupted(null, ie);
    }
  }
  
  public synchronized boolean tryStartLifecycle() {
    if (syncsStarted > 0 || syncsReadyToStart > 0) {
      return false;
    } else {
      lifecycleOccuring += 1;
      return true;
    }
  }
  
  public synchronized void startReference() {
    try {
      while (syncsReadyToStart > 0) {
        this.wait();
      }
      lifecycleOccuring += 1;
    } catch (InterruptedException ie) {
      L2Utils.handleInterrupted(null, ie);
    }
  } 

  public synchronized boolean tryStartReference() {
    if (syncsReadyToStart > 0) {
      return false;
    } else {
      lifecycleOccuring += 1;
      return true;
    }
  } 
  
  public synchronized void syncStarted() {
    Assert.assertTrue(syncsReadyToStart > 0);
    Assert.assertTrue(lifecycleOccuring == 0);
    syncsReadyToStart -= 1;
    syncsStarted += 1;
    notifyAll();
  }
  
  public synchronized boolean isSyncing() {
    return syncsStarted > 0 || syncsFinishing > 0;
  }
  
  public synchronized void syncFinishing() {
    Assert.assertTrue(syncsStarted > 0);
    syncsStarted -= 1;
    syncsFinishing += 1;
    notifyAll();
  }
  
  public synchronized void syncFinished() {
    Assert.assertTrue(syncsFinishing > 0);
    syncsFinishing -= 1;
    notifyAll();
  }
  
  public synchronized void finishLifecycle() {
    Assert.assertTrue(lifecycleOccuring > 0);
    lifecycleOccuring -= 1;
    notifyAll();
  }
}
