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
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Set;
import org.terracotta.entity.ConcurrencyStrategy;


public class RequestProcessor {
  private PassiveReplicationBroker passives;
  private final Sink<Runnable> requestExecution;
  private boolean isActive = false;
  private static final TCLogger PLOGGER = TCLogging.getLogger(MessagePayload.class);
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

  public ActivePassiveAckWaiter scheduleSync(SyncReplicationActivity activity, NodeID passive) {
    return passives.replicateActivity(activity, Collections.singleton(passive));
  }
  
  public void setReplication(PassiveReplicationBroker passives) {
    Assert.assertNull(this.passives);
    this.passives = passives;
  }

//  this is synchronized because both PTH and Request Processor thread has access to this method.  the replication and schduling on the executor needs
//  to happen in the same order.  synchronizing this method enforces that
  public synchronized ActivePassiveAckWaiter scheduleRequest(EntityID eid, long version, FetchID fetchID, ServerEntityRequest request, MessagePayload payload, Runnable call, boolean replicate, int concurrencyKey) {
    // Determine if this kind of action is one we want to replicate.
    ServerEntityAction requestAction = request.getAction();
    // We will try to replicate anything which isn't just a local flush operation.
    boolean isActionReplicated = !((ServerEntityAction.LOCAL_FLUSH == requestAction)
        || (ServerEntityAction.MANAGED_ENTITY_GC == requestAction)
        || (ServerEntityAction.LOCAL_FLUSH_AND_SYNC == requestAction));
    // Unless this is a message type we allow to choose its own concurrency key, we will use management (default for all internal operations).
    Set<NodeID> replicateTo = (isActive && isActionReplicated && passives != null) ? request.replicateTo(passives.passives()) : Collections.emptySet();
//  if there is somewhere to replicate to but replication was not required
    if (!replicateTo.isEmpty() && !replicate) {
      if (request.requiresReceived()) {
//  ordering symantics requested, send a special placeholder that is completed
//  as soon as it is received
        requestAction = ServerEntityAction.ORDER_PLACEHOLDER_ONLY;
      } else {
//  ordering is not requested so don't bother replicating a placeholder
        replicateTo = Collections.emptySet();
      }
    }
    ActivePassiveAckWaiter token = (!replicateTo.isEmpty())
        ? passives.replicateActivity(createReplicationActivity(eid, version, fetchID, request.getNodeID(), requestAction, 
            request.getTransaction(), request.getOldestTransactionOnClient(), payload, concurrencyKey), replicateTo)
        : NoReplicationBroker.NOOP_WAITER;
    EntityRequest entityRequest =  new EntityRequest(eid, call, concurrencyKey);
    if (PLOGGER.isDebugEnabled()) {
      PLOGGER.debug("SCHEDULING:" + payload.getDebugId() + " on " + eid + ":" + concurrencyKey);
    }
    requestExecution.addMultiThreaded(entityRequest);
    return token;
  }
  
  private static final EnumMap<ServerEntityAction, SyncReplicationActivity.ActivityType> typeMap  = new EnumMap<>(ServerEntityAction.class);
  
  static {
    typeMap.put(ServerEntityAction.CREATE_ENTITY, SyncReplicationActivity.ActivityType.CREATE_ENTITY);
    typeMap.put(ServerEntityAction.RECONFIGURE_ENTITY, SyncReplicationActivity.ActivityType.RECONFIGURE_ENTITY);
    typeMap.put(ServerEntityAction.DESTROY_ENTITY, SyncReplicationActivity.ActivityType.DESTROY_ENTITY);
    typeMap.put(ServerEntityAction.FETCH_ENTITY, SyncReplicationActivity.ActivityType.FETCH_ENTITY);
    typeMap.put(ServerEntityAction.INVOKE_ACTION, SyncReplicationActivity.ActivityType.INVOKE_ACTION);
    typeMap.put(ServerEntityAction.ORDER_PLACEHOLDER_ONLY, SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER);
    typeMap.put(ServerEntityAction.RELEASE_ENTITY, SyncReplicationActivity.ActivityType.RELEASE_ENTITY);
    typeMap.put(ServerEntityAction.REQUEST_SYNC_ENTITY, SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN);
    
  }
  
  private static SyncReplicationActivity createReplicationActivity(EntityID id, long version, FetchID fetchID, ClientID src,
      ServerEntityAction type, TransactionID tid, TransactionID oldest, MessagePayload payload, int concurrency) {
    SyncReplicationActivity.ActivityType actionCode = typeMap.get(type);
    Assert.assertNotNull(actionCode);
    
    // Handle our replicated message creations as special-cases, if they aren't normal invokes.
    SyncReplicationActivity activity = null;
    if (SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER == actionCode) {
      activity = SyncReplicationActivity.createOrderingPlaceholder(fetchID, src, tid, oldest, payload.getDebugId());
    } else if (SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN == actionCode) {
      activity = SyncReplicationActivity.createStartEntityKeyMessage(id, version, fetchID, concurrency);
    } else if (SyncReplicationActivity.ActivityType.INVOKE_ACTION == actionCode) {
      activity = SyncReplicationActivity.createInvokeMessage(fetchID, src, tid, oldest, actionCode, payload.getRawPayload(), concurrency, payload.getDebugId());
    } else {
      // Normal replication.
      activity = SyncReplicationActivity.createLifecycleMessage(id, version, fetchID, src, tid, oldest, actionCode, payload.getRawPayload());
    }
    return activity;
  }
  
  public static class EntityRequest implements MultiThreadedEventContext, Runnable {
    private final EntityID entity;
    private final Runnable invoke;
    private final int key;

    public EntityRequest(EntityID entity, Runnable runnable, int key) {
      this.entity = entity;
      this.invoke = runnable;
      this.key = key;
    }

    @Override
    public Object getSchedulingKey() {
      if (key == ConcurrencyStrategy.UNIVERSAL_KEY) {
        return null;
      }
//  create some additional entropy so all entities are not ordered the same
      return key ^ entity.hashCode();
    }
//  Runnable so handler can cast and execute
    @Override
    public void run() {
      invoke();
    }
    
    void invoke()  {
        // NOTE:  No longer waiting for received for before invoke.  Wait has been moved to 
	// the completed/failure notification.  This should be fine for both client invokes 
	// and EntityMessenger

        // We can now run the invoke.
        invoke.run();
    }

    @Override
    public boolean flush() {
// anything on the management key needs a complete flush of all the queues
// the hydrate stage does not need to be flushed as each client 
      return (key == ConcurrencyStrategy.MANAGEMENT_KEY);
    }
  }
}
