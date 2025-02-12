/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.util.ArrayList;
import java.util.List;


public class PassthroughServerCrasher implements IAsynchronousServerCrasher {
  private final PassthroughClusterControl control;
  private boolean isRunning = false;
  private final List<PassthroughServerProcess> victimQueue;
  private final Thread background = new Thread() {
    @Override
    public void run() {
      PassthroughServerProcess victim = waitNextRequest();
      while (null != victim) {
        PassthroughServerCrasher.this.control.restartOneServerFromInside(victim);
        victim = waitNextRequest();
      }
    }
  };
  
  public PassthroughServerCrasher(PassthroughClusterControl control) {
    this.control = control;
    this.victimQueue = new ArrayList<PassthroughServerProcess>();
  }

  @Override
  public synchronized void terminateServerProcess(PassthroughServerProcess victim) {
    boolean shouldNotify = this.victimQueue.isEmpty();
    this.victimQueue.add(victim);
    if (shouldNotify) {
      this.notifyAll();
    }
  }

  public synchronized PassthroughServerProcess waitNextRequest() {
    while (this.isRunning && this.victimQueue.isEmpty()) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        // Not expected.
        Assert.unexpected(e);
      }
    }
    PassthroughServerProcess process = null;
    if (this.isRunning) {
      process = this.victimQueue.remove(0);
    }
    return process;
  }

  public void start() {
    this.isRunning = true;
    this.background.start();
  }
  
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="IS2_INCONSISTENT_SYNC")
  public void waitForStop() {
    synchronized (this) {
      this.isRunning = false;
      this.notifyAll();
    }
    try {
      this.background.join();
    } catch (InterruptedException e) {
      // Not expected.
      Assert.unexpected(e);
    }
  }
}
