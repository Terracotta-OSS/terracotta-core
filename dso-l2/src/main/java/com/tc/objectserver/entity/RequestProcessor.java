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
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;
import java.util.Collections;
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
  public synchronized ActivePassiveAckWaiter scheduleRequest(EntityDescriptor entity, ServerEntityRequest request, MessagePayload payload, Runnable call, boolean replicate, int concurrencyKey) {
    // Determine if this kind of action is one we want to replicate.
    ServerEntityAction requestAction = request.getAction();
    // We will try to replicate anything which isn't just a local flush operation.
    boolean isActionReplicated = !((ServerEntityAction.LOCAL_FLUSH == requestAction)
        || (ServerEntityAction.MANAGED_ENTITY_GC == requestAction)
        || (ServerEntityAction.LOCAL_FLUSH_AND_SYNC == requestAction));
    // Unless this is a message type we allow to choose its own concurrency key, we will use management (default for all internal operations).
    Set<NodeID> replicateTo = (isActive && isActionReplicated && passives != null) ? request.replicateTo(passives.passives()) : Collections.emptySet();
    ActivePassiveAckWaiter token = (isActionReplicated && !replicateTo.isEmpty())
        ? passives.replicateActivity(createReplicationActivity(entity, request.getNodeID(), replicate ? requestAction : ServerEntityAction.ORDER_PLACEHOLDER_ONLY, 
            request.getTransaction(), request.getOldestTransactionOnClient(), payload, concurrencyKey), replicateTo)
        : NoReplicationBroker.NOOP_WAITER;
    EntityRequest entityRequest =  new EntityRequest(entity, call, concurrencyKey);
    if (PLOGGER.isDebugEnabled()) {
      PLOGGER.debug("SCHEDULING:" + payload.getDebugId() + " on " + entity + ":" + concurrencyKey);
    }
    requestExecution.addMultiThreaded(entityRequest);
    return token;
  }
  
  private static SyncReplicationActivity createReplicationActivity(EntityDescriptor id, ClientID src,
      ServerEntityAction type, TransactionID tid, TransactionID oldest, MessagePayload payload, int concurrency) {
    SyncReplicationActivity.ActivityType actionCode = SyncReplicationActivity.ActivityType.INVALID;
    switch (type) {
      case CREATE_ENTITY:
        actionCode = SyncReplicationActivity.ActivityType.CREATE_ENTITY;
        break;
      case RECONFIGURE_ENTITY:
        actionCode = SyncReplicationActivity.ActivityType.RECONFIGURE_ENTITY;
        break;
      case DESTROY_ENTITY:
        actionCode = SyncReplicationActivity.ActivityType.DESTROY_ENTITY;
        break;
      case FETCH_ENTITY:
        actionCode = SyncReplicationActivity.ActivityType.FETCH_ENTITY;
        break;
      case INVOKE_ACTION:
        actionCode = SyncReplicationActivity.ActivityType.INVOKE_ACTION;
        break;
      case ORDER_PLACEHOLDER_ONLY:
        actionCode = SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER;
        break;
      case RELEASE_ENTITY:
        actionCode = SyncReplicationActivity.ActivityType.RELEASE_ENTITY;
        break;
      case REQUEST_SYNC_ENTITY:
//  this marks the start of entity sync for a concurrency key.  practically, this means that
//  all replicated messages for this key and entity must be forwarded to passives
        actionCode = SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN;
        break;
      default:
        // Unknown message type.
        Assert.fail("Unknown message type: " + type);
        break;
    }
    
    // Handle our replicated message creations as special-cases, if they aren't normal invokes.
    SyncReplicationActivity activity = null;
    if (SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER == actionCode) {
      activity = SyncReplicationActivity.createOrderingPlaceholder(id, src, tid, oldest, payload.getDebugId());
    } else if (SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN == actionCode) {
      activity = SyncReplicationActivity.createStartEntityKeyMessage(id.getEntityID(), id.getClientSideVersion(), concurrency);
    } else {
      // Normal replication.
      activity = SyncReplicationActivity.createReplicatedMessage(id, src, tid, oldest, actionCode, payload.getRawPayload(), concurrency, payload.getDebugId());
    }
    return activity;
  }
  
  public static class EntityRequest implements MultiThreadedEventContext, Runnable {
    private final EntityDescriptor entity;
    private final Runnable invoke;
    private final int key;

    public EntityRequest(EntityDescriptor entity, Runnable runnable, int key) {
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
      return key ^ entity.getEntityID().hashCode();
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
