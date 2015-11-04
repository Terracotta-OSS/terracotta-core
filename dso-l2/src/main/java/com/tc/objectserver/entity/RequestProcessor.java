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
import com.tc.l2.state.StateChangeListener;
import com.tc.object.EntityDescriptor;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.util.Assert;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;

public class RequestProcessor implements StateChangeListener {
  
  private PassiveReplicationBroker passives;
  private final Sink<Runnable> requestExecution;
//  TODO: do some accounting for transaction de-dupping on failover

  public RequestProcessor(Sink<Runnable> requestExecution) {
    this.requestExecution = requestExecution;
  }
  
  public void setReplication(PassiveReplicationBroker passives) {
    Assert.assertNull(this.passives);
    this.passives = passives;
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    if (passives != null) {
      this.passives.setActive(sce.movedToActive());
    }
  }
  
  public int scheduleRequest(ManagedEntityImpl impl, EntityDescriptor entity, ConcurrencyStrategy<EntityMessage> strategy, ServerEntityRequest request, EntityMessage message) {
    int index = (strategy == null || request.getAction() != ServerEntityAction.INVOKE_ACTION) ? 
        ConcurrencyStrategy.MANAGEMENT_KEY : 
        strategy.concurrencyKey(message);
    Future<Void> token = (passives != null && request.requiresReplication())
        ? passives.replicateMessage(entity, impl.getVersion(), request.getNodeID(), request.getAction(), 
            request.getTransaction(), request.getOldestTransactionOnClient(), request.getPayload())
        : NoReplicationBroker.NOOP_FUTURE;
    EntityRequest entityRequest =  new EntityRequest(impl, entity, request, index, token);
    requestExecution.addMultiThreaded(entityRequest);
    return index;
  }
  
  private static class EntityRequest implements MultiThreadedEventContext, Runnable {
    private final ManagedEntityImpl impl;
    private final EntityDescriptor entity;
    private final ServerEntityRequest request;
    private final Future<Void>  token;
    private final int key;

    public EntityRequest(ManagedEntityImpl impl, EntityDescriptor entity, ServerEntityRequest request, int concurrencyIndex, Future<Void>  token) {
      this.impl = impl;
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
        impl.invoke(request);
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
