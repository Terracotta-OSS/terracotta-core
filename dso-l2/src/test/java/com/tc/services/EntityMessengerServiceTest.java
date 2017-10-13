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
package com.tc.services;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.RetirementManager;
import org.junit.Assert;
import org.junit.Test;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ExplicitRetirementHandle;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import org.terracotta.entity.ActiveServerEntity;


public class EntityMessengerServiceTest {
  /**
   * Test that we can register a bunch of operations before the entity has started-up and they will be registered, once
   * it does.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testEarlyCallRegistration() throws Exception {
    ISimpleTimer timer = mock(ISimpleTimer.class);
    when(timer.addPeriodic(any(), anyLong(), anyLong())).thenReturn(1L);
    when(timer.addDelayed(any(), anyLong())).thenReturn(1L);
    Sink<VoltronEntityMessage> sink = mock(Sink.class);
    ManagedEntity entity = mock(ManagedEntity.class);
    when(entity.isDestroyed()).thenReturn(true);
    when(entity.getRetirementManager()).thenReturn(mock(RetirementManager.class));
    @SuppressWarnings("rawtypes")
    MessageCodec codec = mock(MessageCodec.class);
    when(entity.getCodec()).thenReturn(codec);

    // Create the service.
    EntityMessengerService service = new EntityMessengerService(timer, sink, entity, true);
    // now adding listener in provider so do it manually
    entity.addLifecycleListener(service);
    // Verify that the service was registered to be told when the entity activates.
    verify(entity).addLifecycleListener(service);

    // Register a few calls.
    EntityMessage delayMessage = mock(EntityMessage.class);
    EntityMessage periodicMessage = mock(EntityMessage.class);
    IEntityMessenger.ScheduledToken delayToken = service.messageSelfAfterDelay(delayMessage, 1L);
    Assert.assertNotNull(delayToken);
    IEntityMessenger.ScheduledToken periodicToken = service.messageSelfPeriodically(periodicMessage, 1L);
    Assert.assertNotNull(periodicToken);

    // Verify that the timer didn't see them.
    verify(timer, never()).addDelayed(any(), anyLong());
    verify(timer, never()).addPeriodic(any(), anyLong(), anyLong());

    // Activate the entity.
    when(entity.isDestroyed()).thenReturn(false);
    ActiveServerEntity ae = mock(ActiveServerEntity.class);
    service.entityCreated(ae);

    // Verify that the timer did see them.
    verify(timer).addDelayed(any(), anyLong());
    verify(timer).addPeriodic(any(), anyLong(), anyLong());
    
    // destroy entity
    service.entityDestroyed(ae);
    when(entity.isDestroyed()).thenReturn(true);
    // Verify everything cancelled
    verify(timer, times(2)).cancel(anyLong());
  }

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
    when(entity.getRetirementManager()).thenReturn(retirementManager);
    @SuppressWarnings("rawtypes") MessageCodec codec = mock(MessageCodec.class);
    when(entity.getCodec()).thenReturn(codec);

    // Create the service.
    EntityMessengerService service = new EntityMessengerService(timer, sink, entity, true);
    when(entity.isDestroyed()).thenReturn(false);
    ActiveServerEntity ae = mock(ActiveServerEntity.class);
    service.entityCreated(ae);

    EntityMessage deferrableMessage = mock(EntityMessage.class);
    EntityMessage futureMessage = mock(EntityMessage.class);
    ExplicitRetirementHandle handle = service.deferRetirement("test", deferrableMessage, futureMessage);

    // verify it was deferred
    verify(retirementManager).deferRetirement(deferrableMessage, futureMessage);
    handle.release();
    // verify that got scheduled.
    verify(sink).addSingleThreaded(any());
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
    when(entity.getCodec()).thenReturn(codec);

    // Create the service.
    EntityMessengerService service = new EntityMessengerService(timer, sink, entity, true);
    // now adding listener in provider so do it manually
    entity.addLifecycleListener(service);
    // Verify that the service was registered to be told when the entity activates.
    verify(entity).addLifecycleListener(service);

    // messageSelf before create is finished
    EntityMessage delayMessage = mock(EntityMessage.class);
    service.messageSelf(delayMessage);

    verify(sink).addSingleThreaded(any(VoltronEntityMessage.class));
  }
}
