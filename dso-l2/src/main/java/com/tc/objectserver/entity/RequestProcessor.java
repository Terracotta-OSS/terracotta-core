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
import com.tc.async.api.StageManager;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.utils.L2Utils;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ConcurrencyStrategy;


public class RequestProcessor {
  private PassiveReplicationBroker passives;
  private final Sink<EntityRequest> requestExecution;
  private final Sink<EntityRequest> syncExecution;
  private boolean isActive = false;
  private static final Logger PLOGGER = LoggerFactory.getLogger(MessagePayload.class);
  
  public RequestProcessor(StageManager stageManager, int maxQueueSize, boolean use_direct) {
    int MIN_NUM_PROCESSORS = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.MIN_ENTITY_PROCESSOR_THREADS);
    int numOfProcessors = L2Utils.getOptimalApplyStageWorkerThreads(true);
    numOfProcessors = Math.max(MIN_NUM_PROCESSORS, numOfProcessors);
    requestExecution = stageManager.createStage(ServerConfigurationContext.REQUEST_PROCESSOR_STAGE, EntityRequest.class, new RequestProcessorHandler(), numOfProcessors, maxQueueSize, use_direct).getSink();
    syncExecution = stageManager.createStage(ServerConfigurationContext.REQUEST_PROCESSOR_DURING_SYNC_STAGE, EntityRequest.class, new SyncRequestProcessorHandler(), MIN_NUM_PROCESSORS, maxQueueSize, use_direct).getSink();
  }
//  TODO: do some accounting for transaction de-dupping on failover
  
  RequestProcessor(Sink<EntityRequest> requestExecution) {
    this(requestExecution, requestExecution);
  }
  
  RequestProcessor(Sink<EntityRequest> requestExecution, Sink<EntityRequest> syncExecution) {
    this.requestExecution = requestExecution;
    this.syncExecution = requestExecution;
  }

  public void enterActiveState() {
    isActive = true;
  }

  public ActivePassiveAckWaiter scheduleSync(SyncReplicationActivity activity, SessionID passive) {
    return passives.replicateActivity(activity, Collections.singleton(passive));
  }
  
  public void setReplication(PassiveReplicationBroker passives) {
    Assert.assertNull(this.passives);
    this.passives = passives;
  }

//  this is synchronized because both PTH and Request Processor thread has access to this method.  the replication and schduling on the executor needs
//  to happen in the same order.  synchronizing this method enforces that
  public synchronized void scheduleRequest(boolean inSync, EntityID eid, long version, FetchID fetchID, ServerEntityRequest request, MessagePayload payload, Consumer<ActivePassiveAckWaiter> call, boolean replicate, int concurrencyKey) {
    // Determine if this kind of action is one we want to replicate.
    final ServerEntityAction requestAction = (!replicate && request.requiresReceived()) ? ServerEntityAction.ORDER_PLACEHOLDER_ONLY : request.getAction();
    // We will try to replicate anything which isn't just a local flush operation.
    boolean isActionReplicated = requestAction.isReplicated();
    
    Supplier<ActivePassiveAckWaiter> token = ()->{
      // Unless this is a message type we allow to choose its own concurrency key, we will use management (default for all internal operations).
      Set<SessionID> replicateTo = (isActive && isActionReplicated && passives != null) ? request.replicateTo(passives.passives()) : Collections.emptySet();
  //  if there is somewhere to replicate to but replication was not required
      if (!replicateTo.isEmpty() && !replicate && !request.requiresReceived()) {
  //  ordering is not requested so don't bother replicating a placeholder
        replicateTo.clear();
      }
      if (PLOGGER.isDebugEnabled()) {
        PLOGGER.debug("SCHEDULING:{} {} on {} with concurrency:{} replicatedTo: {}",requestAction, payload.getDebugId(), eid, concurrencyKey, replicateTo);
      }
      return !replicateTo.isEmpty() 
        ? passives.replicateActivity(createReplicationActivity(eid, version, fetchID, request.getNodeID(), request.getClientInstance(), requestAction, 
            request.getTransaction(), request.getOldestTransactionOnClient(), payload, concurrencyKey), replicateTo)
        : NoReplicationBroker.NOOP_WAITER;
    };
    
    EntityRequest entityRequest =  new EntityRequest(eid, call, token, concurrencyKey, payload);

    if (inSync) {
      syncExecution.addToSink(entityRequest);
    } else {
      requestExecution.addToSink(entityRequest);
    }
  }

  private static SyncReplicationActivity createReplicationActivity(EntityID id, long version, FetchID fetchID, ClientID src, ClientInstanceID instance, 
      ServerEntityAction type, TransactionID tid, TransactionID oldest, MessagePayload payload, int concurrency) {
    SyncReplicationActivity.ActivityType actionCode = type.replicationType();
    Assert.assertNotNull(actionCode);
    
    // Handle our replicated message creations as special-cases, if they aren't normal invokes.
    SyncReplicationActivity activity = null;
    switch (actionCode) {
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        activity = SyncReplicationActivity.createStartEntityKeyMessage(id, version, fetchID, concurrency);
        break;
      case ORDERING_PLACEHOLDER:
        activity = SyncReplicationActivity.createOrderingPlaceholder(fetchID, src, instance, tid, oldest, payload.getDebugId());
        break;
      case INVOKE_ACTION:
        activity = SyncReplicationActivity.createInvokeMessage(fetchID, src, instance, tid, oldest, actionCode, payload.getByteBufferPayload(), concurrency, payload.getDebugId());
        break;
      default:
        // Normal replication.
        activity = SyncReplicationActivity.createLifecycleMessage(id, version, fetchID, src, instance, tid, oldest, actionCode, payload.getByteBufferPayload());
        break;
    }
    return activity;
  }
  
  public static class EntityRequest implements MultiThreadedEventContext, Runnable {
    private final EntityID entity;
    private final Consumer<ActivePassiveAckWaiter> invoke;
    private final int key;
    private final Supplier<ActivePassiveAckWaiter> waiter;
    private final MessagePayload debug;

    EntityRequest(EntityID entity, Consumer<ActivePassiveAckWaiter> runnable, Supplier<ActivePassiveAckWaiter> waiter, int key, MessagePayload debug) {
      this.entity = entity;
      this.invoke = runnable;
      this.key = key;
      this.waiter = waiter;
      this.debug = debug;
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
    
    ActivePassiveAckWaiter invoke()  {
        // NOTE:  No longer waiting for received for before invoke.  Wait has been moved to 
	// the completed/failure notification.  This should be fine for both client invokes 
	// and EntityMessenger

        // We can now run the invoke.
        ActivePassiveAckWaiter token = waiter.get();
        invoke.accept(token);
        return token;
    }

    @Override
    public boolean flush() {
// anything on the management key needs a complete flush of all the queues
// the hydrate stage does not need to be flushed as each client 
      return (key == ConcurrencyStrategy.MANAGEMENT_KEY);
    }

    @Override
    public String toString() {
      return "EntityRequest{" + "entity=" + entity + ", key=" + key + ", debug=" + debug.getDebugId() + '}';
    }
  }
}
