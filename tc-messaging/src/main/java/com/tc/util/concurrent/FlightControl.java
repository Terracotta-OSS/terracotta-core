/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
