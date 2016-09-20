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
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.terracotta.entity.ActiveOnlyEntityMessage;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.Execution;


public class RequestProcessor {
  private PassiveReplicationBroker passives;
  private final Sink<Runnable> requestExecution;
  private boolean isActive = false;
//  TODO: do some accounting for transaction de-dupping on failover

  public RequestProcessor(Sink<Runnable> requestExecution) {
    this.requestExecution = requestExecution;
  }

  public void enterActiveState() {
    passives.enterActiveState();
    isActive = true;
  }
  
  public Set<NodeID> passives() {
    return passives.passives();
  }

  public ActivePassiveAckWaiter scheduleSync(PassiveSyncMessage msg, NodeID passive) {
    return passives.replicateMessage(msg, Collections.singleton(passive));
  }
  
  public void setReplication(PassiveReplicationBroker passives) {
    Assert.assertNull(this.passives);
    this.passives = passives;
  }

//  this is synchronized because both PTH and Request Processor thread has access to this method.  the replication and schduling on the executor needs
//  to happen in the same order.  synchronizing this method enforces that
  public synchronized ActivePassiveAckWaiter scheduleRequest(EntityDescriptor entity, ServerEntityRequest request, MessagePayload payload, Runnable call, int concurrencyKey) {
    // Unless this is a message type we allow to choose its own concurrency key, we will use management (default for all internal operations).
    Set<NodeID> replicateTo = (isActive && passives != null) ? request.replicateTo(passives.passives()) : Collections.emptySet();
    ServerEntityAction action = request.getAction();
    ActivePassiveAckWaiter token = NoReplicationBroker.NOOP_WAITER;
//  check requiresReplication for backwards compatibility.  The default behavior is to replicate
    if (payload.requiresReplication() && !replicateTo.isEmpty()) {
      if (isActive && action == ServerEntityAction.INVOKE_ACTION) {
        Execution[] whereToExec = payload.getEntityMessage().getClass().getAnnotationsByType(Execution.class);
        if (payload.getEntityMessage() instanceof ActiveOnlyEntityMessage ||
          (whereToExec != null && Arrays.asList(whereToExec).stream().noneMatch(e->Arrays.asList(e.value()).contains(Execution.ServerState.PASSIVE)))) {
      // convert this action to a NOOP if it is not supposed to run on the passive.  Converting to NOOP preserves the transaction's 
      //  place in line but does not execute
          action = ServerEntityAction.NOOP;
        }
      }
      token = passives.replicateMessage(createReplicationMessage(entity, request.getNodeID(), action, 
            request.getTransaction(), request.getOldestTransactionOnClient(), payload.getRawPayload(), concurrencyKey), replicateTo);
    }

    EntityRequest entityRequest =  new EntityRequest(entity, call, concurrencyKey, token);
    requestExecution.addMultiThreaded(entityRequest);
    return token;
  }
  
  private static ReplicationMessage createReplicationMessage(EntityDescriptor id, ClientID src,
      ServerEntityAction type, TransactionID tid, TransactionID oldest, byte[] payload, int concurrency) {
    ReplicationMessage.ReplicationType actionCode = ReplicationMessage.ReplicationType.NOOP;
    switch (type) {
      case CREATE_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.CREATE_ENTITY;
        break;
      case RECONFIGURE_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.RECONFIGURE_ENTITY;
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
      case RELEASE_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.NOOP;
        break;
      case REQUEST_SYNC_ENTITY:
//  this marks the start of entity sync for a concurrency key.  practically, this means that
//  all replicated messages for this key and entity must be forwarded to passives
        actionCode = ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_BEGIN;
        break;
      default:
        // Unknown message type.
        Assert.fail();
        break;
    }
//  TODO: Evaluate what to replicate...right now, everything is replicated.  Evaluate whether
//  NOOP should be replicated.  For now, NOOPs hold ordering
    return ReplicationMessage.createReplicatedMessage(id, src, tid, oldest, actionCode, payload, concurrency);
  }
  
  public static class EntityRequest implements MultiThreadedEventContext, Runnable {
    private final EntityDescriptor entity;
    private final Runnable invoke;
    private final ActivePassiveAckWaiter replicationWaiter;
    private final int key;
    private boolean done = false;

    public EntityRequest(EntityDescriptor entity, Runnable runnable, int key, ActivePassiveAckWaiter replicationWaiter) {
      this.entity = entity;
      this.invoke = runnable;
      this.replicationWaiter = replicationWaiter;
      this.key = key;
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
        // NOTE:  We want to wait to hear that the passive has received the replicated invoke.
        this.replicationWaiter.waitForReceived();
        // We can now run the invoke.
        invoke.run();
        // Now that we are done, wait for the passive to finish.
        this.replicationWaiter.waitForCompleted();
        // We are now completely finished.
        finish();
      } catch (InterruptedException interrupted) {
//  shutdown logic?  uniterruptable?
        throw new RuntimeException(interrupted);
      }
    }

    @Override
    public boolean flush() {
// anything on the management key needs a complete flush of all the queues
// the hydrate stage does not need to be flushed as each client 
      return (key == ConcurrencyStrategy.MANAGEMENT_KEY);
    }
    
    private synchronized void finish() {
      done = true;
      this.notifyAll();
    }
    
    synchronized boolean isDone() {
      return done;
    }
    
    public void waitForPassives() {
      try {
        this.replicationWaiter.waitForCompleted();
      } catch (InterruptedException ie) {
        throw new RuntimeException(ie);
      }
    }
  }
}
