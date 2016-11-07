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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.ServiceConfiguration;


public class PassthroughMessengerServiceProvider implements PassthroughImplementationProvidedServiceProvider, Closeable {
  private final PassthroughTimerThread timerThread;
  private final PassthroughServerProcess passthroughServerProcess;
  private final PassthroughConnection pseudoConnection;
  
  public PassthroughMessengerServiceProvider(PassthroughServerProcess passthroughServerProcess, PassthroughConnection connection) {
    this.timerThread = new PassthroughTimerThread();
    this.passthroughServerProcess = passthroughServerProcess;
    this.pseudoConnection = connection;
    
    this.timerThread.setName("PassthroughTimerThread");
    this.timerThread.start();
  }

  @Override
  public <T> T getService(String entityClassName, String entityName, long consumerID, DeferredEntityContainer container, ServiceConfiguration<T> configuration) {
    return configuration.getServiceType().cast(new PassthroughMessengerService(this.timerThread, this.passthroughServerProcess, this.pseudoConnection, container, entityClassName, entityName));
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
