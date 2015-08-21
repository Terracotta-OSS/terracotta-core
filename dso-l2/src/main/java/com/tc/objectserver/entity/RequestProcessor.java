/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.terracotta.entity.ConcurrencyStrategy;

public class RequestProcessor {
  
  private final PassiveReplicationBroker passives;
  private final Sink<Runnable> requestExecution;
//  TODO: do some accounting for transaction de-dupping on failover

  public RequestProcessor(PassiveReplicationBroker passives, Sink<Runnable> requestExecution) {
    this.passives = passives;
    this.requestExecution = requestExecution;
  }
  
  int scheduleRequest(ManagedEntityImpl entity, ConcurrencyStrategy strategy, ServerEntityRequest request) {
    int index = (strategy == null || request.getAction() != ServerEntityAction.INVOKE_ACTION) ? 
        ConcurrencyStrategy.MANAGEMENT_KEY : 
        strategy.concurrencyKey(request.getPayload());
    Future<Void> token = (request.requiresReplication())
        ? passives.replicateMessage(entity.getID(), request.getNodeID(), index, request.getAction(), request.getTransaction(), request.getPayload())
        : NoReplicationBroker.NOOP_FUTURE;
    EntityRequest entityRequest =  new EntityRequest(entity, request, index, token);
    requestExecution.addMultiThreaded(entityRequest);
    return index;
  }
  
  private static class EntityRequest implements MultiThreadedEventContext, Runnable {
    private final ManagedEntityImpl entity;
    private final ServerEntityRequest request;
    private final Future<Void>  token;
    private final int key;

    public EntityRequest(ManagedEntityImpl entity, ServerEntityRequest request, int concurrencyIndex, Future<Void>  token) {
      this.entity = entity;
      this.request = request;
      this.key = concurrencyIndex;
      this.token = token;
    }

    @Override
    public Object getSchedulingKey() {
      if (key == ConcurrencyStrategy.UNIVERSAL_KEY) {
        return null;
      }
//  create some additional entropy so all entities are not ordered the same
      return key ^ entity.getID().hashCode();
    }
//  Runnable so handler can cast and execute
    @Override
    public void run() {
      invoke();
    }
    
    void invoke()  {
      try {
        token.get();
        entity.invoke(request);
      } catch (InterruptedException interrupted) {
//  shutdown logic?  uniterruptable?
        throw new RuntimeException(interrupted);
      } catch (ExecutionException exec) {
//  what TODO?
        throw new RuntimeException(exec);
      }
    }
  }
  
}
