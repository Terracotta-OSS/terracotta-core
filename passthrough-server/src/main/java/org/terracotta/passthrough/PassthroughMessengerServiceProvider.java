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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.ServiceConfiguration;


public class PassthroughMessengerServiceProvider implements PassthroughImplementationProvidedServiceProvider, Closeable {
  private final PassthroughTimerThread timerThread;
  private final PassthroughServerProcess passthroughServerProcess;
  
  public PassthroughMessengerServiceProvider(PassthroughServerProcess passthroughServerProcess) {
    this.timerThread = new PassthroughTimerThread();
    this.passthroughServerProcess = passthroughServerProcess;
    
    this.timerThread.setName("PassthroughTimerThread");
    this.timerThread.start();
  }

  @Override
  @SuppressWarnings({ "unchecked" })
  public <T> T getService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration) {
    boolean chain = false;
    if (configuration instanceof Supplier) {
      chain = ((Supplier<Boolean>)configuration).get();
    }
    return configuration.getServiceType().cast(new PassthroughMessengerService(this.timerThread, this.passthroughServerProcess, container, chain, entityClassName, entityName));
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    // Using Collections.singleton here complains about trying to unify between different containers of different class
    // bindings so doing it manually satisfies the compiler (seems to work in Java8 but not Java6).
    Set<Class<?>> set = new HashSet<Class<?>>();
    set.add(IEntityMessenger.class);
    return set;
  }

  @Override
  public void close() throws IOException {
    this.timerThread.shutdown();
    try {
      this.timerThread.join();
    } catch (InterruptedException e) {
      // We don't expect interruptions on this thread.
      Assert.unexpected(e);
    }
  }
}
