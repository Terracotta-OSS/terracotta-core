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
package com.terracotta.connection;

import com.tc.object.ClientEntityManager;
import com.tc.object.DistributedObjectClient;


public class ClientHandleImpl implements ClientHandle {

  private final DistributedObjectClient client;

  public ClientHandleImpl(DistributedObjectClient client) {
    this.client = client;
  }

  @Override
  public void shutdown() {
    client.shutdown();
  }

  @Override
  public ClientEntityManager getClientEntityManager() {
    return client.getEntityManager();
  }

  @Override
  public boolean isShutdown() {
    return client.isShutdown();
  }
}
