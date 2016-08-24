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
package com.tc.util.concurrent;

import java.io.Serializable;

/**
 * 
 */
public final class FlightControl implements Serializable {

  private int inFlightOps;

  public FlightControl() {
  }

  public synchronized void startOperation() {
    inFlightOps += 1;
  }
  
  public synchronized boolean finishOperation() {
    inFlightOps -= 1;
    if (inFlightOps == 0) {
      this.notifyAll();
      return true;
    } else {
      return false;
    }
  }
  
  public synchronized void waitForOperationsToComplete() {
    boolean interrupted = false;
    while (inFlightOps > 0) {
      try {
        this.wait();
      } catch (InterruptedException ie) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
