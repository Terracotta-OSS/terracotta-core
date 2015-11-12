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

import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.protocol.tcm.MessageChannel;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
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
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ReplicableActiveServerEntity;
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
                    RequestProcessor process, 
                    ServerEntityService<? extends ActiveServerEntity<EntityMessage>, ? extends PassiveServerEntity<EntityMessage>> factory,
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
    ConcurrencyStrategy<EntityMessage> concurrencyStrategy = activeServerEntity != null ? activeServerEntity.getConcurrencyStrategy() : null;
    EntityMessage message = null;
    // We only decode messages for INVOKE requests.
    if (ServerEntityAction.INVOKE_ACTION == request.getAction()) {
      message = (null != this.activeServerEntity) ? this.activeServerEntity.getMessageDeserializer().deserialize(request.getPayload()) : null;
    }
    executor.scheduleRequest(this, getEntityDescriptorForSource(client), concurrencyStrategy, request, message);
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
   * Pull one request off the request sequencer and service it. 
   *  
   * TODO: Should we return a boolean here for scheduling hints?  
   * @param request
   */
  public void invoke(ServerEntityRequest request) {
    Lock read = reconnectAccessLock.readLock();
      try {
        read.lock();
        switch (request.getAction()) {
          case CREATE_ENTITY:
            createEntity(request);
            break;
          case INVOKE_ACTION:
            performAction(request);
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
          case SYNC_ENTITY:
//  use typing for this distinction since it is server generated?
            performSync(request);
          case LOAD_EXISTING_ENTITY:
            loadExisting(request);
            break;
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
        this.activeServerEntity = factory.createActiveEntity(registry, constructorInfo);
        entityToCreate = this.activeServerEntity;
      }
    } else {
      if (null != this.passiveServerEntity) {
        throw new IllegalStateException("Passive entity " + id + " already exists.");
      } else {
        this.passiveServerEntity = factory.createPassiveEntity(registry, constructorInfo);
        // Store the configuration in case we promote.
        entityToCreate = this.passiveServerEntity;
      }
    }
    createEntityRequest.complete();
    // We currently don't support loading an entity from a persistent back-end and this call is in response to creating a new
    //  instance so make that call.
    entityToCreate.createNew();
  }

  //  TODO: stub implementation.  This is supposed to send the data to the passive server for sync
  private void performSync(ServerEntityRequest wrappedRequest) {
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        int concurrency = PassiveSyncServerEntityRequest.getConcurrency(wrappedRequest.getPayload());
        PassiveSynchronizationChannel syncChannel = new PassiveSynchronizationChannel() {
          @Override
          public void synchronizeToPassive(byte[] payload) {
            ((PassiveSyncServerEntityRequest)wrappedRequest).sendToPassive(new PassiveSyncMessage(id, concurrency, payload));
          }};
//  cast is ok here because theree is no way to get here without this entity being replicable
        ((ReplicableActiveServerEntity)this.activeServerEntity).synchronizeKeyToPassive(syncChannel, concurrency);
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
  
  private void performAction(ServerEntityRequest wrappedRequest) {
    if (this.isInActiveState) {
      if (null == this.activeServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        EntityMessage message = this.activeServerEntity.getMessageDeserializer().deserialize(wrappedRequest.getPayload());
        wrappedRequest.complete(this.activeServerEntity.invoke(wrappedRequest.getSourceDescriptor(), message));
      }
    } else {
      if (null == this.passiveServerEntity) {
        throw new IllegalStateException("Actions on a non-existent entity.");
      } else {
        EntityMessage message = this.passiveServerEntity.getMessageDeserializer().deserialize(wrappedRequest.getPayload());
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
  public void sync(NodeID passive, GroupManager mgr) throws GroupException {
    mgr.sendTo(passive, new PassiveSyncMessage(id, version, constructorInfo));
// TODO:  This is a stub, the real implementation is to be designed
// iterate through all the concurrency keys of an entity
    if (activeServerEntity instanceof ReplicableActiveServerEntity) {
      ReplicableActiveServerEntity replication = (ReplicableActiveServerEntity)activeServerEntity;
      for (Integer concurrency : this.activeServerEntity.getConcurrencyStrategy().getKeysForSynchronization()) {
  // send the start message of a concurrency index and of an entity
        mgr.sendTo(passive, new PassiveSyncMessage(id, concurrency, true));
        PassiveSyncServerEntityRequest req = new PassiveSyncServerEntityRequest(id, version, concurrency, mgr, passive);
        // We don't actually use the message in the direct strategy so this is safe.
        EntityMessage message = null;
        executor.scheduleRequest(this, getEntityDescriptorForSource(req.getSourceDescriptor()), new DirectConcurrencyStrategy(concurrency), req, message);
        req.waitFor();
  // send the end message of a concurrency index and of an entity
        mgr.sendTo(passive, new PassiveSyncMessage(id, concurrency, false));
      }
    }
//  end passive sync for an entity
    mgr.sendTo(passive, new PassiveSyncMessage(id, version, null));
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
  
  private static class DirectConcurrencyStrategy implements ConcurrencyStrategy<EntityMessage> {
    private final int target;

    public DirectConcurrencyStrategy(int target) {
      this.target = target;
    }
    
    @Override
    public int concurrencyKey(EntityMessage payload) {
      return target;
    }

    @Override
    public Set<Integer> getKeysForSynchronization() {
      Assert.fail("Synchronization not applicable to directory concurrency strategy");
      return null;
    }    
  }

  private static class PassiveSyncServerEntityRequest extends AbstractServerEntityRequest {
    
    private final GroupManager group;
    private final NodeID passive;

    public PassiveSyncServerEntityRequest(EntityID eid, long version, int concurrency, GroupManager group, NodeID passive) {
      super(new EntityDescriptor(eid,ClientInstanceID.NULL_ID,version), ServerEntityAction.SYNC_ENTITY, makePayload(concurrency), null, null, null, false);
      this.group = group;
      this.passive = passive;
    }

    @Override
    public ServerEntityAction getAction() {
      return ServerEntityAction.SYNC_ENTITY;
    }
    
    public static byte[] makePayload(int concurrency) {
      return ByteBuffer.allocate(Integer.BYTES).putInt(concurrency).array();
    }
    
    public static int getConcurrency(byte[] payload) {
      return ByteBuffer.wrap(payload).getInt();
    }
    
    @Override
    public boolean requiresReplication() {
      return false;
    }
    
    public void sendToPassive(GroupMessage msg) {
      try {
        group.sendTo(passive, msg);
      } catch (GroupException ge) {
        throw new RuntimeException(ge);
      }
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
  }
}
