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
package com.tc.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashSet;
import java.util.Set;

public class ClientShutdownManager {
  private static final Logger logger = LoggerFactory.getLogger(ClientShutdownManager.class);

  private final Set<Runnable>                      beforeShutdown = new HashSet<Runnable>();
  private final DistributedObjectClient            client;

  public ClientShutdownManager(DistributedObjectClient client) {
    this.client = client;
  }

  public void registerBeforeShutdownHook(Runnable beforeShutdownHook) {
    synchronized (beforeShutdown) {
      beforeShutdown.add(beforeShutdownHook);
    }
  }

  public void unregisterBeforeShutdownHook(Runnable beforeShutdownHook) {
    synchronized (beforeShutdown) {
      beforeShutdown.remove(beforeShutdownHook);
    }
  }

  private void executeBeforeShutdownHooks() {
    Runnable[] beforeShutdowns;
    synchronized (beforeShutdown) {
      beforeShutdowns = beforeShutdown.toArray(new Runnable[beforeShutdown.size()]);
    }
    for (Runnable runnable : beforeShutdowns) {
      runnable.run();
    }
  }

  public void execute() {
    executeBeforeShutdownHooks();

    client.shutdownResources();
  }
}
