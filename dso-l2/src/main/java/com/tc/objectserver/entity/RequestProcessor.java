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

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.state.StateChangeListener;
import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.ManagedEntityImpl.ManagedEntityRequest;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodec;


public class RequestProcessor implements StateChangeListener {
  private PassiveReplicationBroker passives;
  private final Sink<Runnable> requestExecution;
//  TODO: do some accounting for transaction de-dupping on failover

  public RequestProcessor(Sink<Runnable> requestExecution) {
    this.requestExecution = requestExecution;
  }

  public Future<Void> scheduleSync(PassiveSyncMessage msg, NodeID passive) {
    return passives.replicateMessage(msg, Collections.singleton(passive));
  }
  
  public void setReplication(PassiveReplicationBroker passives) {
    Assert.assertNull(this.passives);
    this.passives = passives;
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
//  do nothing
  }
  
  public void scheduleRequest(ManagedEntityImpl impl, EntityDescriptor entity, ServerEntityRequest request, EntityMessage message, int concurrencyKey, MessageCodec<EntityMessage> codec) {
    // Unless this is a message type we allow to choose its own concurrency key, we will use management (default for all internal operations).
    Set<NodeID> replicateTo = (passives != null && request.requiresReplication()) ? passives.passives() : Collections.emptySet();
    Future<Void> token = (!replicateTo.isEmpty())
        ? passives.replicateMessage(createReplicationMessage(entity, request.getNodeID(), request.getAction(), 
            request.getTransaction(), request.getOldestTransactionOnClient(), request.getPayload(), concurrencyKey), replicateTo)
        : NoReplicationBroker.NOOP_FUTURE;
    ManagedEntityRequest managedRequest = new ManagedEntityRequest(request, codec);
    EntityRequest entityRequest =  new EntityRequest(impl, entity, managedRequest, concurrencyKey, token, message);
    requestExecution.addMultiThreaded(entityRequest);
  }
  
  private static ReplicationMessage createReplicationMessage(EntityDescriptor id, NodeID src,
      ServerEntityAction type, TransactionID tid, TransactionID oldest, byte[] payload, int concurrency) {
    ReplicationMessage.ReplicationType actionCode = ReplicationMessage.ReplicationType.NOOP;
    switch (type) {
      case CREATE_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.CREATE_ENTITY;
        break;
      case DESTROY_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.DESTROY_ENTITY;
        break;
      case FETCH_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.NOOP;
        break;
      case INVOKE_ACTION:
        actionCode = ReplicationMessage.ReplicationType.INVOKE_ACTION;
        break;
      case NOOP:
        actionCode = ReplicationMessage.ReplicationType.NOOP;
        break;
      case PROMOTE_ENTITY_TO_ACTIVE:
        actionCode = ReplicationMessage.ReplicationType.NOOP;
        break;
      case RELEASE_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.RELEASE_ENTITY;
        break;
      case REQUEST_SYNC_ENTITY:
//  this marks the start of entity sync for a concurrency key.  practically, this means that
//  all replicated messages for this key and entity must be forwarded to passives
        actionCode = ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_BEGIN;
        break;
      default:
        break;
    }
//  TODO: Evaluate what to replicate...right now, everything is replicated.  Evaluate whether
//  NOOP should be replicated.  For now, NOOPs hold ordering
    return new ReplicationMessage(id, src, tid, oldest, actionCode, payload, concurrency);
  }
  
  private static class EntityRequest implements MultiThreadedEventContext, Runnable {
    private final ManagedEntityImpl impl;
    private final EntityDescriptor entity;
    private final ManagedEntityRequest request;
    private final Future<Void>  token;
    private final int key;
    private final EntityMessage message;

    public EntityRequest(ManagedEntityImpl impl, EntityDescriptor entity, ManagedEntityRequest managedRequest, int concurrencyIndex, Future<Void>  token, EntityMessage message) {
      this.impl = impl;
      this.entity = entity;
      this.request = managedRequest;
      this.key = concurrencyIndex;
      this.token = token;
      this.message = message;
    }

    @Override
    public Object getSchedulingKey() {
      if (key == ConcurrencyStrategy.UNIVERSAL_KEY) {
        return null;
      }
//  create some additional entropy so all entities are not ordered the same
      return key ^ entity.getEntityID().hashCode();
    }
//  Runnable so handler can cast and execute
    @Override
    public void run() {
      invoke();
    }
    
    void invoke()  {
      try {
        token.get();
        impl.invoke(request, this.key, this.message);
      } catch (InterruptedException interrupted) {
//  shutdown logic?  uniterruptable?
        throw new RuntimeException(interrupted);
      } catch (ExecutionException exec) {
//  what TODO?
        throw new RuntimeException(exec);
      }
    }

    @Override
    public boolean flush() {
// anything on the management key needs a complete flush of all the queues
// the hydrate stage does not need to be flushed as each client 
      return (key == ConcurrencyStrategy.MANAGEMENT_KEY);
    }
  }
  
}
