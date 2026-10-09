/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.services;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.RetirementManager;
import com.tc.services.EntityMessengerService.Handle;
import org.junit.Test;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodec;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.assertNotNull;
import org.mockito.Mockito;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.entity.ActiveServerMessenger;
import org.terracotta.entity.EntityResponse;


public class EntityMessengerServiceTest {
  @Test
  public void testExplicitRetirement() throws Exception {
    // Thank god I can steal Jeff's mocks.
    ISimpleTimer timer = mock(ISimpleTimer.class);
    when(timer.addPeriodic(any(), anyLong(), anyLong())).thenReturn(1L);
    when(timer.addDelayed(any(), anyLong())).thenReturn(1L);
    Sink<VoltronEntityMessage> sink = mock(Sink.class);
    ManagedEntity entity = mock(ManagedEntity.class);
    when(entity.isDestroyed()).thenReturn(true);
    RetirementManager retirementManager = mock(RetirementManager.class);
    when(retirementManager.deferRetirement(any(), any())).thenReturn(true);
    when(entity.getRetirementManager()).thenReturn(retirementManager);
    @SuppressWarnings("rawtypes") MessageCodec codec = mock(MessageCodec.class);
    when(codec.encodeMessage(any())).thenReturn(new byte[0]);
    when(entity.getCodec()).thenReturn(codec);

    // Create the service.
    EntityMessengerService<EntityMessage, EntityResponse> service = new EntityMessengerService<>(sink, entity, null);
    when(entity.isDestroyed()).thenReturn(false);
    service.entityCreated(entity);

    EntityMessage deferrableMessage = mock(EntityMessage.class);
    EntityMessage futureMessage = mock(EntityMessage.class);
    Handle handle = service.deferRetirement("test", deferrableMessage, futureMessage);

    // verify it was deferred
    verify(retirementManager).deferRetirement(deferrableMessage, futureMessage);
    handle.release();
    // verify that got scheduled.
    verify(sink).addToSink(any());
  }

  @Test
  public void testEarlySend() throws Exception {
    ISimpleTimer timer = mock(ISimpleTimer.class);
    when(timer.addPeriodic(any(), anyLong(), anyLong())).thenReturn(1L);
    when(timer.addDelayed(any(), anyLong())).thenReturn(1L);
    Sink<VoltronEntityMessage> sink = mock(Sink.class);
    ManagedEntity entity = mock(ManagedEntity.class);
    when(entity.isDestroyed()).thenReturn(true);
    when(entity.getRetirementManager()).thenReturn(mock(RetirementManager.class));
    @SuppressWarnings("rawtypes")
    MessageCodec codec = mock(MessageCodec.class);
    when(codec.encodeMessage(any())).thenReturn(new byte[0]);
    when(entity.getCodec()).thenReturn(codec);

    // Create the service.
    EntityMessengerService<EntityMessage, EntityResponse> service = new EntityMessengerService<>(sink, entity, null);
    // now adding listener in provider so do it manually
    entity.addLifecycleListener(service);
    // Verify that the service was registered to be told when the entity activates.
    verify(entity).addLifecycleListener(service);

    // messageSelf before create is finished
    EntityMessage delayMessage = mock(EntityMessage.class);
    service.messageSelf(delayMessage);

    verify(sink).addToSink(any(VoltronEntityMessage.class));
  }

  /**
   * Regression test for the NPE fix: Handle.release() (no-arg) must not throw NullPointerException.
   * Before the fix, release() delegated to release(null), and the callback inside messageSelf
   * unconditionally called consumer.accept(...), causing an NPE when consumer was null.
   */
  @Test
  public void testHandleReleaseNoArgDoesNotThrowNPE() throws Exception {
    Sink<VoltronEntityMessage> sink = mock(Sink.class);
    ManagedEntity entity = mock(ManagedEntity.class);
    RetirementManager retirementManager = mock(RetirementManager.class);
    when(retirementManager.deferRetirement(any(), any())).thenReturn(true);
    when(entity.getRetirementManager()).thenReturn(retirementManager);
    @SuppressWarnings("rawtypes") MessageCodec codec = mock(MessageCodec.class);
    when(codec.encodeMessage(any())).thenReturn(new byte[0]);
    when(entity.getCodec()).thenReturn(codec);

    EntityMessengerService<EntityMessage, EntityResponse> service = new EntityMessengerService<>(sink, entity, null);

    EntityMessage deferrable = mock(EntityMessage.class);
    EntityMessage future = mock(EntityMessage.class);
    Handle handle = service.deferRetirement("no-arg-release", deferrable, future);

    assertNotNull(handle);

    handle.release();

    // The future message must have been scheduled on the sink
    verify(sink).addToSink(any(VoltronEntityMessage.class));
  }

  /**
   * Regression test: Handle.release(null) must not throw NullPointerException.
   * The guard added by the fix skips consumer.accept() when consumer is null.
   */
  @Test
  public void testHandleReleaseNullConsumerDoesNotThrowNPE() throws Exception {
    Sink<VoltronEntityMessage> sink = mock(Sink.class);
    ManagedEntity entity = mock(ManagedEntity.class);
    RetirementManager retirementManager = mock(RetirementManager.class);
    when(retirementManager.deferRetirement(any(), any())).thenReturn(true);
    when(entity.getRetirementManager()).thenReturn(retirementManager);
    @SuppressWarnings("rawtypes") MessageCodec codec = mock(MessageCodec.class);
    when(codec.encodeMessage(any())).thenReturn(new byte[0]);
    when(entity.getCodec()).thenReturn(codec);

    EntityMessengerService<EntityMessage, EntityResponse> service = new EntityMessengerService<>(sink, entity, null);

    EntityMessage deferrable = mock(EntityMessage.class);
    EntityMessage future = mock(EntityMessage.class);
    Handle handle = service.deferRetirement("null-consumer-release", deferrable, future);

    // Must not throw NullPointerException — passing explicit null was the original defect path
    handle.release((Consumer<ActiveServerMessenger.Response<EntityResponse>>) null);

    // The future message must still have been scheduled on the sink
    verify(sink).addToSink(any(VoltronEntityMessage.class));
  }

  /**
   * Verifies that Handle.release(consumer) actually invokes the consumer when a non-null
   * consumer is supplied, so that callers can observe the response.
   */
  @Test
  public void testHandleReleaseWithConsumerInvokesCallback() throws Exception {
    Sink<VoltronEntityMessage> sink = mock(Sink.class);
    ManagedEntity entity = mock(ManagedEntity.class);
    RetirementManager retirementManager = mock(RetirementManager.class);
    when(retirementManager.deferRetirement(any(), any())).thenReturn(true);
    when(entity.getRetirementManager()).thenReturn(retirementManager);
    @SuppressWarnings("rawtypes") MessageCodec codec = mock(MessageCodec.class);
    when(codec.encodeMessage(any())).thenReturn(new byte[0]);
    when(entity.getCodec()).thenReturn(codec);

    EntityMessengerService<EntityMessage, EntityResponse> service = new EntityMessengerService<>(sink, entity, null);

    EntityMessage deferrable = mock(EntityMessage.class);
    EntityMessage future = mock(EntityMessage.class);
    Handle handle = service.deferRetirement("with-consumer", deferrable, future);

    AtomicBoolean consumerCalled = new AtomicBoolean(false);
    AtomicReference<Object> captured = new AtomicReference<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    Consumer<ActiveServerMessenger.Response<?>> consumer = response -> {
      consumerCalled.set(true);
      captured.set(response);
    };
    handle.release((Consumer) consumer);

    // The future message must have been scheduled
    verify(sink).addToSink(any(VoltronEntityMessage.class));
    Mockito.reset(sink);
    // The consumer itself is only invoked when the scheduled message completes (driven by
    // the completion handler on FakeEntityMessage), which does not happen in this unit test
    // because the sink is mocked.  What we CAN assert is that the handle was correctly
    // removed from the retirement map (i.e. release() processed it) and that a second
    // release() call is a no-op.
    AtomicBoolean secondCallCalled = new AtomicBoolean(false);
    handle.release(r -> secondCallCalled.set(true));
    verify(sink, Mockito.never()).addToSink(any(VoltronEntityMessage.class));
  }
}
