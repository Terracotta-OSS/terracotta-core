/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler;
import com.tc.objectserver.handler.GlobalTransactionIDBatchRequestHandler.GlobalTransactionIDBatchRequestContext;
import com.tc.test.TCTestCase;
import com.tc.util.sequence.BatchSequenceReceiver;

import static org.hamcrest.CoreMatchers.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlobalTransactionIDBatchRequestHandlerTest extends TCTestCase {

  private ReplicatedClusterStateManager          clusterStateManager;
  private GlobalTransactionIDBatchRequestHandler provider;
  private TestMutableSequence                    persistentSequence;
  private Sink                                   requestBatchSink;

  @Override
  public void setUp() throws Exception {
    persistentSequence = spy(new TestMutableSequence());
    requestBatchSink = mock(Sink.class);
    clusterStateManager = mock(ReplicatedClusterStateManager.class);

    provider = new GlobalTransactionIDBatchRequestHandler(persistentSequence);
    provider.setRequestBatchSink(requestBatchSink);

    TestServerConfigurationContext scc = new TestServerConfigurationContext();
    scc.l2Coordinator = mock(L2Coordinator.class);
    when(scc.l2Coordinator.getReplicatedClusterStateManager()).thenReturn(clusterStateManager);
    provider.initializeContext(scc);
  }

  public void testRequestBatch() throws Exception {
    BatchSequenceReceiver receiver = mock(BatchSequenceReceiver.class);
    provider.requestBatch(receiver, 5);
    verify(requestBatchSink).add((EventContext) argThat(allOf(hasBatchSize(5), hasReceiver(receiver))));
  }

  public void testHandleRequest() throws Exception {
    BatchSequenceReceiver receiver = mock(BatchSequenceReceiver.class);
    GlobalTransactionIDBatchRequestContext context = new GlobalTransactionIDBatchRequestContext(receiver, 5);
    provider.handleEvent(context);
    verify(clusterStateManager).publishNextAvailableGlobalTransactionID(5);
    verify(persistentSequence).nextBatch(5);
    verify(receiver).setNextBatch(0, 5);
  }

  private static Matcher<EventContext> hasReceiver(final BatchSequenceReceiver receiver) {
    return new BaseMatcher<EventContext>() {
      @Override
      public boolean matches(final Object o) {
        if (o instanceof GlobalTransactionIDBatchRequestContext) {
          return receiver.equals(((GlobalTransactionIDBatchRequestContext)o).getReceiver());
        } else {
          return false;
        }
      }

      @Override
      public void describeTo(final Description description) {
        //
      }
    };
  }

  private static Matcher<EventContext> hasBatchSize(final int batchSize) {
    return new BaseMatcher<EventContext>() {
      @Override
      public boolean matches(final Object o) {
        if (o instanceof GlobalTransactionIDBatchRequestContext) {
          return ((GlobalTransactionIDBatchRequestContext)o).getBatchSize() == batchSize;
        } else {
          return false;
        }
      }

      @Override
      public void describeTo(final Description description) {
        //
      }
    };
  }

}
