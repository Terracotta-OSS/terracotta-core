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
package com.tc.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.terracotta.server.Server;

/**
 *
 */
public class ServerFuture implements Future<Boolean> {

  private final Server server;
  private final ThreadGroup threadGroup;

  public ServerFuture(Server server, ThreadGroup threadGroup) {
    this.server = server;
    this.threadGroup = threadGroup;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (server.isStopped()) {
      return false;
    } else {
      server.stop();
      return true;
    }
  }

  @Override
  public boolean isCancelled() {
    return !server.isStopped();
  }

  @Override
  public boolean isDone() {
    return server.isStopped();
  }

  @Override
  public Boolean get() throws InterruptedException, ExecutionException {
    return server.waitUntilShutdown();
  }

  @Override
  public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //  for completeness , do not use
    long end = System.currentTimeMillis() + unit.toMillis(timeout);
    while (System.currentTimeMillis() < end) {
      if (!server.isStopped()) {
        Thread.sleep(500);
      } else {
        return server.waitUntilShutdown();
      }
    }
    throw new TimeoutException();
  }
  // for galvan compatiblity

  public boolean waitUntilShutdown() {
    try {
      return get();
    } catch (ExecutionException | InterruptedException e) {
      return false;
    }
  }

  public Object getManagement() {
    return server.getManagement();
  }

  public Object getServer() {
    return server;
  }

  public Object getServerThreadGroup() {
    return threadGroup;
  }
}
