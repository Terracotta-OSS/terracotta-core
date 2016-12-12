/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.entity;

import com.tc.util.Assert;
import java.io.Serializable;

/**
 * Special control structure to make sure sync and lifecycle operations
 * are properly controlled through the sync process
 */
public final class ManagedEntitySyncInterop implements Serializable {
//  single threaded lifecycle
  private int lifecycleOccuring;
//  multiple sync threads possible
  private int syncsReadyToStart;
  private int syncsStarted;

  public ManagedEntitySyncInterop() {
  }

  public synchronized void startSync() {
    try {
      while (lifecycleOccuring > 0) {
        this.wait();
      }
      syncsReadyToStart += 1;
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }

  public synchronized void startLifecycle() {
    try {
      while (syncsStarted > 0 || syncsReadyToStart > 0) {
        this.wait();
      }
      lifecycleOccuring += 1;
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
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
      throw new RuntimeException(ie);
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
  
  public synchronized void syncFinished() {
    Assert.assertTrue(syncsStarted > 0);
    syncsStarted -= 1;
    notifyAll();
  }
  
  public synchronized void finishLifecycle() {
    Assert.assertTrue(lifecycleOccuring > 0);
    lifecycleOccuring -= 1;
    notifyAll();
  }
}
