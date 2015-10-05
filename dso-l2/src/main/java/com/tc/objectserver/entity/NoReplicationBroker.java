/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 * Stubbed implementation which provides no replication.
 */
public class NoReplicationBroker implements PassiveReplicationBroker {
  
  public static final Future<Void> NOOP_FUTURE = new Future<Void>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return true;
      }

      @Override
      public Void get() throws InterruptedException, ExecutionException {
        return null;
      }

      @Override
      public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
      }
  };

  @Override
  public Future<Void> replicateMessage(EntityDescriptor id, long version, NodeID src, int concurrency, ServerEntityAction type, TransactionID tid, TransactionID oldest, byte[] payload) {
    return NOOP_FUTURE;
  }

  @Override
  public boolean isActive() {
    return false;
}

  @Override
  public void setActive(boolean active) {
    throw new IllegalStateException("this replication broker cannot go active");
  }
  
  

}
