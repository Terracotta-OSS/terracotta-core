/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
import com.tc.exception.ServerExceptionType;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityDescriptor;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ResultCapture;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.handler.EntityExistenceHelpers;
import com.tc.objectserver.persistence.Persistor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LifecycleResultCapture implements ResultCapture {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleResultCapture.class);

    private final ServerEntityRequest request;
    private final EntityDescriptor descriptor;
    private final long consumerID;
    private final byte[] config;
    private final Persistor persistor;
    private final EntityManager entityManager;
    private final Function<ClientID, MessageChannel> channelManager;

    private Supplier<ActivePassiveAckWaiter> setOnce;

    public LifecycleResultCapture(EntityDescriptor descriptor,
            long consumerID,
            ServerEntityRequest request,
            byte[] config, Persistor persistor,
            EntityManager entityManager,
            Function<ClientID, MessageChannel> channelManager) {
      this.request = request;
      this.descriptor = descriptor;
      this.consumerID = consumerID;
      this.config = config;
      this.persistor = persistor;
      this.entityManager = entityManager;
      this.channelManager = channelManager;
    }

    @Override
    public CompletionStage<Void> retired() {
      CompletableFuture<Void> complete = new CompletableFuture<>();
      setOnce.get().runWhenCompleted(()->complete.complete(null));
      return complete;
    }

    @Override
    public void message(byte[] message) {

    }

    @Override
    public void setWaitFor(Supplier<ActivePassiveAckWaiter> waiter) {
      this.setOnce = waiter;
    }

    @Override
    public void failure(ServerException e) {
      switch (request.getAction()) {
        case CREATE_ENTITY:
          persistor.getEntityPersistor().entityCreateFailed(descriptor.getEntityID(), request.getNodeID(), request.getTransaction().toLong(), request.getOldestTransactionOnClient().toLong(), e);
          break;
        case RECONFIGURE_ENTITY:
          EntityExistenceHelpers.recordReconfigureEntity(persistor.getEntityPersistor(), entityManager, request.getNodeID(), request.getTransaction(), request.getOldestTransactionOnClient(), descriptor.getEntityID(), descriptor.getClientSideVersion(), null, e);
          break;
        case DESTROY_ENTITY:
          EntityExistenceHelpers.recordDestroyEntity(persistor.getEntityPersistor(), entityManager, request.getNodeID(), request.getTransaction(), request.getOldestTransactionOnClient(), descriptor.getEntityID(), e);
          break;
        case FETCH_ENTITY:
          if (e.getType() != ServerExceptionType.ENTITY_NOT_FOUND && e.getType() != ServerExceptionType.ENTITY_BUSY_EXCEPTION) {
            // disconnect the client due to error after a reference count has been taken
            // NOT_FOUND is pre-reference count
            disconnectClientDueToFailure(request.getNodeID(), e);
          }
          break;
        case RELEASE_ENTITY:
          break;
        default:

      }
      if (setOnce != null) {
        ActivePassiveAckWaiter waiter = setOnce.get();
        waiter.waitForCompleted();
        if (waiter.verifyLifecycleResult(false)) {
          LOGGER.warn("ZAP occurred while processing " + request.getAction() + " on " + this.descriptor);
        }
      }
    }

    @Override
    public void complete() {
      switch(request.getAction()) {
        case CREATE_ENTITY:
          if (!request.getNodeID().isNull()) {
            persistor.getEntityPersistor().entityCreated(request.getNodeID(), request.getTransaction().toLong(), request.getOldestTransactionOnClient().toLong(), descriptor.getEntityID(), descriptor.getClientSideVersion(), consumerID, true, config);
          } else {
            persistor.getEntityPersistor().entityCreatedNoJournal(descriptor.getEntityID(), descriptor.getClientSideVersion(), consumerID, entityManager.canDelete(descriptor.getEntityID()), config);
          }
          break;
        case RECONFIGURE_ENTITY:
          EntityExistenceHelpers.recordReconfigureEntity(persistor.getEntityPersistor(), entityManager, request.getNodeID(), request.getTransaction(), request.getOldestTransactionOnClient(), descriptor.getEntityID(), descriptor.getClientSideVersion(), config, null);
          break;
        case DESTROY_ENTITY:
          EntityExistenceHelpers.recordDestroyEntity(persistor.getEntityPersistor(), entityManager, request.getNodeID(), request.getTransaction(), request.getOldestTransactionOnClient(), descriptor.getEntityID(), null);
          break;
        case FETCH_ENTITY:
        case RELEASE_ENTITY:
          break;
        default:
      }
      if (setOnce != null) {
        ActivePassiveAckWaiter waiter = setOnce.get();
        waiter.waitForCompleted();
        if (waiter.verifyLifecycleResult(true)) {
          LOGGER.warn("ZAP occurred while processing " + request.getAction() + " on " + this.descriptor);
        }
      }
    }

  @Override
  public void complete(byte[] raw) {
    complete();
  }

  @Override
  public synchronized void received() {

  }

  private void disconnectClientDueToFailure(ClientID clientID, Exception exp) {
    LOGGER.info("disconnecting " + clientID + " due to an error", exp);
    MessageChannel channel = channelManager.apply(clientID);
    if (channel != null) {
      channel.close();
    }
  }
}
