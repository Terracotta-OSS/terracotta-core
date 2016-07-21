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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.terracotta.exception.EntityException;

/**
 *
 */
public class BarrierCompletion implements SimpleCompletion {
  private final Semaphore gate = new Semaphore(0);
  
  @Override
  public void waitForCompletion() {
    try {
      gate.acquire();
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }
  
  public void waitForCompletion(long time, TimeUnit units) {
    try {
      gate.tryAcquire(time, units);
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }
  
  public void complete() {
    gate.release();
  }
  
  public void complete(byte[] raw) {
    gate.release();
  }
  
  public void failure(EntityException ee) {
    gate.release();
  }
}
