/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.tc.objectserver.api.ManagedEntity;
import com.tc.util.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;

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
    PlatformServer server = mock(PlatformServer.class);
    InternalServiceRegistry internal = mock(InternalServiceRegistry.class);
    when(internal.getService(any(ServiceConfiguration.class))).thenReturn(mock(IStripeMonitoring.class));
    when(reg.subRegistry(anyLong())).thenReturn(internal);
    ServerEnv.setDefaultServer(mock(Server.class));
    producer = new LocalMonitoringProducer(getClass().getClassLoader(), reg, server, null);
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
