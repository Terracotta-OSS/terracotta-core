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
package com.tc.objectserver.testentity;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.PassiveSynchronizationChannel;


public class TestEntityServer implements ActiveServerEntity<EntityMessage, EntityResponse> {

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
  }

  @Override
  public ReconnectHandler startReconnect() {
    return new ReconnectHandler() {
      @Override
      public void handleReconnect(ClientDescriptor clientDescriptor, byte[] extendedReconnectData) {
        // Do nothing.
      }

      @Override
      public void close() {

      }
    };
  }

  


  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
  }

  @Override
  public EntityResponse invokeActive(ActiveInvokeContext context,
                                     EntityMessage message) throws EntityUserException {
    return null;
  }

  @Override
  public void createNew() {
  }

  @Override
  public void loadExisting() {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void notifyDestroyed(ClientSourceId id) {

  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<EntityMessage> syncChannel, int concurrencyKey) {
    // TODO:  Add synchronization support.
    throw new AssertionError("Synchronization not supported for this entity");
  }
}