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

import com.tc.objectserver.api.ManagedEntity;
import com.tc.util.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

/**
 *
 * @author mscott
 */
public class LocalMonitoringProducerTest {
  
  private LocalMonitoringProducer producer;
  
  public LocalMonitoringProducerTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() throws Exception {
    TerracottaServiceProviderRegistry reg = mock(TerracottaServiceProviderRegistry.class);
    ISimpleTimer timer = mock(ISimpleTimer.class);
    PlatformServer server = mock(PlatformServer.class);
    InternalServiceRegistry internal = mock(InternalServiceRegistry.class);
    when(internal.getService(any(ServiceConfiguration.class))).thenReturn(mock(IStripeMonitoring.class));
    when(reg.subRegistry(anyInt())).thenReturn(internal);
    producer = new LocalMonitoringProducer(reg, server, timer);
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testLifecycle() {
    ManagedEntity entity = mock(ManagedEntity.class);
    Assert.assertNotNull(producer.getService(1, entity, new BasicServiceConfiguration<>(IMonitoringProducer.class)));
    verify(entity).addLifecycleListener(any(ManagedEntity.LifecycleListener.class));
  }
}
