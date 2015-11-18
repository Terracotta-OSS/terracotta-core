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

import com.google.common.base.Throwables;
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.net.protocol.tcm.MessageChannel;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageDeserializer;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;

import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.exception.EntityUserException;


public class ManagedEntityImpl implements ManagedEntity {
  private final RequestProcessor executor;

  private final EntityID id;
  private final long version;
  private final ServiceRegistry registry;
  private final ClientEntityStateManager clientEntityStateManager;
  private final ServerEntityService<? extends ActiveServerEntity<EntityMessage>, ? extends PassiveServerEntity<EntityMessage>> factory;
  // isInActiveState defines which entity type to check/create - we need the flag to represent the pre-create state.
  private boolean isInActiveState;
  private volatile ActiveServerEntity<EntityMessage> activeServerEntity;
  private volatile PassiveServerEntity<EntityMessage> passiveServerEntity;
  //  reconnect access has to be exclusive.  it is out-of-band from normal invoke access
  private final ReadWriteLock reconnectAccessLock = new ReentrantReadWriteLock();
  // NOTE:  This may be removed in the future if we change how we access the config from the ServerEntityService but
  //  it presently holds the config we used when we first created passiveServerEntity (if it isn't null).  It is used
  //  when we promote to an active.
  private byte[] constructorInfo;

  ManagedEntityImpl(EntityID id, long version, ServiceRegistry registry, ClientEntityStateManager clientEntityStateManager,
                    RequestProcessor process, ServerEntityService<? extends ActiveServerEntity<EntityMessage>, ? extends PassiveServerEntity<EntityMessage>> factory,
                    boolean isInActiveState) {
    this.id = id;
    this.version = version;
    this.registry = registry;
    this.clientEntityStateManager = clientEntityStateManager;
    this.factory = factory;
    this.executor = process;
    this.isInActiveState = isInActiveState;
  }

  @Override
  public EntityID getID() {
    return id;
  }

  @Override
  public long getVersion() {
    return version;
  }

  @Override
  public void addRequest(ServerEntityRequest request) {
    ClientDescriptor client = request.getSourceDescriptor();
    ServerEntityAction action = request.getAction();
    if ((ServerEntityAction.INVOKE_ACTION == action)
        || (ServerEntityAction.RECEIVE_SYNC_PAYLOAD == action)) {
      // Invoke and payload requests need to wait for the entity creation so that they can request the concurrency strategy.
      waitForEntityCreation();
    }
    ConcurrencyStrategy<EntityMessage> concurrencyStrategy = activeServerEntity != null ? activeServerEntity.getConcurrencyStrategy() : null;
    EntityMessage message = null;
    // We only decode messages for INVOKE and RECEIVE_SYNC_PAYLOAD requests.
    if ((ServerEntityAction.INVOKE_ACTION == action)
        || (ServerEntityAction.RECEIVE_SYNC_PAYLOAD == action)) {
      // We might get replicated invokes before the create has completed
      CommonServerEntity<EntityMessage> entity = (null != this.activeServerEntity) ? this.activeServerEntity : this.passiveServerEntity;
      Assert.assertNotNull(entity);
      MessageDeserializer<EntityMessage> deserializer = entity.getMessageDeserializer();
      Assert.assertNotNull(deserializer);
      byte[] payload = request.getPayload();
      Assert.assertNotNull(payload);
      if (ServerEntityAction.INVOKE_ACTION == action) {
        message = deserializer.deserialize(payload);
      } else if (ServerEntityAction.RECEIVE_SYNC_PAYLOAD == action) {
        message = deserializer.deserializeForSync(request.getConcurrencyKey(), payload);
      }
    }
    // Since concurrency key is pulled out in different ways for these different message types, we will do that here.
    int concurrencyKey = ConcurrencyStrategy.MANAGEMENT_KEY;
    if ((null != concurrencyStrategy) && (ServerEntityAction.INVOKE_ACTION == action)) {
      Assert.assertNotNull(concurrencyStrategy);
      concurrencyKey = concurrencyStrategy.concurrencyKey(message);
    }
    if ((ServerEntityAction.REQUEST_SYNC_ENTITY == action)
        || (ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_START == action)
        || (ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END == action)
        || (ServerEntityAction.RECEIVE_SYNC_PAYLOAD == action)) {
      concurrencyKey = request.getConcurrencyKey();
    }
    executor.scheduleRequest(this, getEntityDescriptorForSource(client), request, message, concurrencyKey);
  }

  private synchronized void waitForEntityCreation() {
    while ((null == this.activeServerEntity) && (null == this.passiveServerEntity)) {
      try {
        wait();
      } catch (InterruptedException e) {
        // We have no way of handling an interruption at this wait point.
        Throwables.propagate(e);
      }
    }
  }

  @Override
  public void reconnectClient(NodeID nodeID, ClientDescriptor clientDescriptor, byte[] extendedReconnectData) {
    EntityDescriptor entityDescriptor = getEntityDescriptorForSource(clientDescriptor);
    clientEntityStateManager.addReference(nodeID, entityDescriptor);
    if (!this.isInActiveState) {
      throw new IllegalStateException("server is not active");
    }
    Assert.assertNotNull(this.activeServerEntity);
    Lock write = reconnectAccessLock.writeLock();
    try {
      write.lock();
      this.activeServerEntity.connected(clientDescriptor);
      this.activeServerEntity.handleReconnect(clientDescriptor, extendedReconnectData);      
    } finally {
      write.unlock();
    }

  }
  

  /**
   * Runs on a concurrent thread from RequestProcessor (meaning that this could be invoked concurrently on the same entity)
   * to handle one request.
   *  
   * @param request The request to process
   * @param concurrencyKey The key this thread is processing by running this request
   * @param message 
   */
  public void invoke(ServerEntityRequest request, int concurrencyKey, EntityMessage message) {
    Lock read = reconnectAccessLock.readLock();
      try {
        read.lock();
        switch (request.getAction()) {
          case CREATE_ENTITY:
            createEntity(request);
            break;
          case INVOKE_ACTION:
            performAction(request, message);
            break;
          case FETCH_ENTITY:
            getEntity(request);
            break;
          case RELEASE_ENTITY:
            releaseEntity(request);
            break;
          case DESTROY_ENTITY:
//  all request queues are flushed because this action is on the MGMT_KEY
            destroyEntity(request);
            break;
          case PROMOTE_ENTITY_TO_ACTIVE:
            promoteEntity(request);
            break;
          case REQUEST_SYNC_ENTITY:
//  use typing for this distinction since it is server generated?
            performSync(request, concurrencyKey);
            break;
          case LOAD_EXISTING_ENTITY:
            loadExisting(request);
            break;
          case RECEIVE_SYNC_ENTITY_START:
            receiveSyncEntityStart(request);
            break;
          case RECEIVE_SYNC_ENTITY_END:
            receiveSyncEntityEnd(request);
            break;
          case RECEIVE_SYNC_ENTITY_KEY_START:
            receiveSyncEntityKeyStart(request, concurrencyKey);
            break;
          case RECEIVE_SYNC_ENTITY_KEY_END:
            receiveSyncEntityKeyEnd(request, concurrencyKey);
            break;
          case RECEIVE_SYNC_PAYLOAD:
            receiveSyncEntityPayload(request, message);
            break;
  
          case DOES_EXIST:
          case NOOP:
            // Never occur on this level.
            throw new IllegalArgumentException("Unexpected request " + request);
          default:
            throw new IllegalArgumentException("Unknown request " + request);
        }
      } catch (Exception e) {
        // Wrap this exception.
        EntityUserException wrapper = new EntityUserException(id.getClassName(), id.getEntityName(), e);
        request.failure(wrapper);
      } finally {
        read.unlock();
      }
  }
  
  private void receiveSyncEntityStart(ServerEntityRequest request) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.startSyncEntity();
  }

  private void receiveSyncEntityEnd(ServerEntityRequest request) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.endSyncEntity();
  }

  private void receiveSyncEntityKeyStart(ServerEntityRequest request, int concurrencyKey) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.startSyncConcurrencyKey(concurrencyKey);
  }

  private void receiveSyncEntityKeyEnd(ServerEntityRequest request, int concurrencyKey) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.endSyncConcurrencyKey(concurrencyKey);
  }

  private void receiveSyncEntityPayload(ServerEntityRequest request, EntityMessage message) {
    // This only makes sense if we have a passive instance.
    Assert.assertNotNull(this.passiveServerEntity);
    this.passiveServerEntity.invoke(message);
  }

  private void destroyEntity(ServerEntityRequest request) {
    CommonServerEntity<EntityMessage> commonServerEntity = this.isInActiveState
        ? activeServerEntity
        : passiveServerEntity;
    if (null != commonServerEntity) {
      ClientDescriptor sourceDescriptor = request.getSourceDescriptor();
      EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
      clientEntityStateManager.removeReference(request.getNodeID(), entityDescriptor);
      commonServerEntity.destroy();
    }
    request.complete();
  }

  private void createEntity(ServerEntityRequest createEntityRequest) {
    constructorInfo = createEntityRequest.getPayload();
    CommonServerEntity<EntityMessage> entityToCreate = null;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " already exists.");
      } else {
        createActiveEntityAndNotify();
        entityToCreate = this.activeServerEntity;
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        createPassiveEntityAndNotify();
        // Store the configuration in case we promote.
        entityToCreate = this.passiveServerEntity;
      }
    }
    createEntityRequest.complete();
    // We currently don't support loading an entity from a persistent back-end and this call is in response to creating a new
    //  instance so make that call.
    entityToCreate.createNew();
  }

  private synchronized void createPassiveEntityAndNotify() {
    this.passiveServerEntity = this.factory.createPassiveEntity(this.registry, this.constructorInfo);
    notifyAll();
  }

  private synchronized void createActiveEntityAndNotify() {
    this.activeServerEntity = this.factory.createActiveEntity(this.registry, this.constructorInfo);
    notifyAll();
  }

  //  TODO: stub implementation.  This is supposed to send the data to the passive server for sync
  private void performSync(ServerEntityRequest wrappedRequest, int concurrencyKey) {
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        PassiveSyncServerEntityRequest unwrappedRequest = ((PassiveSyncServerEntityRequest)wrappedRequest);
        long version = this.version;
        // Create the channel which will send the payloads over the wire.
        PassiveSynchronizationChannel syncChannel = new PassiveSynchronizationChannel() {
          @Override
          public void synchronizeToPassive(byte[] payload) {
            executor.scheduleSync(PassiveSyncMessage.createPayloadMessage(id, version, concurrencyKey, payload), wrappedRequest.getNodeID());
          }};
//  start is handled by the sync request that triggered this action
        this.activeServerEntity.synchronizeKeyToPassive(syncChannel, concurrencyKey);
        executor.scheduleSync(PassiveSyncMessage.createEndEntityKeyMessage(id, version, concurrencyKey), wrappedRequest.getNodeID());
        wrappedRequest.complete();
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
//  doing nothing for sync
      }
    }
  }
  
  private void performAction(ServerEntityRequest wrappedRequest, EntityMessage message) {
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        wrappedRequest.complete(this.activeServerEntity.invoke(wrappedRequest.getSourceDescriptor(), message));
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        this.passiveServerEntity.invoke(message);
        wrappedRequest.complete();
      }
    }
  }

  private void getEntity(ServerEntityRequest getEntityRequest) {
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        ClientDescriptor sourceDescriptor = getEntityRequest.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        clientEntityStateManager.addReference(getEntityRequest.getNodeID(), entityDescriptor);
        this.activeServerEntity.connected(sourceDescriptor);
        getEntityRequest.complete(this.activeServerEntity.getConfig());
      } else {
        getEntityRequest.complete();
      }
    } else {
      throw new IllegalStateException("GET called on passive entity.");
    }
  }

  private void releaseEntity(ServerEntityRequest request) {
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        ClientDescriptor sourceDescriptor = request.getSourceDescriptor();
        EntityDescriptor entityDescriptor = getEntityDescriptorForSource(sourceDescriptor);
        clientEntityStateManager.removeReference(request.getNodeID(), entityDescriptor);
        this.activeServerEntity.disconnected(sourceDescriptor);
      }
      request.complete();
    } else {
      throw new IllegalStateException("RELEASE called on passive entity.");
    }
  }
  
  private EntityDescriptor getEntityDescriptorForSource(ClientDescriptor sourceDescriptor) {
    // We are in internal code so downcast the descriptor.
    ClientDescriptorImpl rawDescriptor = (ClientDescriptorImpl)sourceDescriptor;
    return rawDescriptor.getEntityDescriptor();
  }

  private void promoteEntity(ServerEntityRequest request) {
    // Can't enter active state twice.
    Assert.assertFalse(this.isInActiveState);
    Assert.assertNull(this.activeServerEntity);
    
    this.isInActiveState = true;
    if (null != this.passiveServerEntity) {
      this.activeServerEntity = factory.createActiveEntity(this.registry, constructorInfo);
      this.activeServerEntity.loadExisting();
      this.passiveServerEntity = null;
    } else {
      throw new IllegalStateException("no entity to promote");
    }
    request.complete();
  }

  @Override
  public void sync(NodeID passive) {
    executor.scheduleSync(new PassiveSyncMessage(id, version, constructorInfo), passive);
// TODO:  This is a stub, the real implementation is to be designed
// iterate through all the concurrency keys of an entity
    for (Integer concurrency : this.activeServerEntity.getConcurrencyStrategy().getKeysForSynchronization()) {
      PassiveSyncServerEntityRequest req = new PassiveSyncServerEntityRequest(id, version, concurrency, passive);
      // We don't actually use the message in the direct strategy so this is safe.
      EntityMessage message = null;
      EntityDescriptor entityDescriptor = new EntityDescriptor(this.id, ClientInstanceID.NULL_ID, this.version);
      executor.scheduleRequest(this, entityDescriptor, req, message, concurrency);
      req.waitFor();
    }
//  end passive sync for an entity
    executor.scheduleSync(new PassiveSyncMessage(id, version, null), passive);
  }

  private void loadExisting(ServerEntityRequest loadEntityRequest) {
    constructorInfo = loadEntityRequest.getPayload();
    CommonServerEntity<EntityMessage> entityToLoad = null;
    // Create the appropriate kind of entity, based on our active/passive state.
    if (this.isInActiveState) {
      if (null != this.activeServerEntity) {
        throw new IllegalStateException("Active entity " + id + " already exists.");
      } else {
        this.activeServerEntity = factory.createActiveEntity(registry, constructorInfo);
        entityToLoad = this.activeServerEntity;
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        this.passiveServerEntity = factory.createPassiveEntity(registry, constructorInfo);
        // Store the configuration in case we promote.
        entityToLoad = this.passiveServerEntity;
      }
    }
    loadEntityRequest.complete();
    entityToLoad.loadExisting();
  }

  private static class PassiveSyncServerEntityRequest extends AbstractServerEntityRequest {
    
    private final int concurrencyKey;
    
    public PassiveSyncServerEntityRequest(EntityID eid, long version, int concurrency, NodeID passive) {
      super(new EntityDescriptor(eid,ClientInstanceID.NULL_ID,version), ServerEntityAction.REQUEST_SYNC_ENTITY, makePayload(concurrency), null, null, passive, false);
      this.concurrencyKey = concurrency;
    }

    public static byte[] makePayload(int concurrency) {
      return ByteBuffer.allocate(Integer.BYTES).putInt(concurrency).array();
    }
    
    @Override
    public boolean requiresReplication() {
      return true;
    }

    @Override
    public Optional<MessageChannel> getReturnChannel() {
      return Optional.empty();
    }

    @Override
    public ClientDescriptor getSourceDescriptor() {
      return null;
    }
    
    public synchronized void waitFor() {
      try {
        while (!isDone()) {
          this.wait();
        }
      } catch (InterruptedException ie) {
        //  TODO
        throw new RuntimeException(ie);
      }
    }

    @Override
    public synchronized void complete() {
      this.notifyAll();
      super.complete();
    }

    @Override
    public int getConcurrencyKey() {
      return this.concurrencyKey;
    }
  }
}
