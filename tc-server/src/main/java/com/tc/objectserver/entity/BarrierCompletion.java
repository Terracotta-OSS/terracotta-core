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

import com.tc.exception.ServerException;
import com.tc.net.utils.L2Utils;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
      L2Utils.handleInterrupted(null, ie);
    }
  }
  
  public void waitForCompletion(long time, TimeUnit units) {
    try {
      gate.tryAcquire(time, units);
    } catch (InterruptedException ie) {
      L2Utils.handleInterrupted(null, ie);
    }
  }
  
  public void complete() {
    gate.release();
  }
  
  public void complete(byte[] raw) {
    gate.release();
  }
  
  public void failure(ServerException ee) {
    gate.release();
  }
}
