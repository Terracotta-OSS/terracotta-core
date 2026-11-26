/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.services;

import java.io.Serializable;
import java.util.Collections;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

/**
 *
 */
public class IStripeMonitoringWrapperTest {

  public IStripeMonitoringWrapperTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of serverDidBecomeActive method, of class IStripeMonitoringWrapper.
   */
  @Test
  public void testRequireLocalDeliveryOnPassiveDefaultDoesnotDeliver() {
    PlatformServer self = mock(PlatformServer.class);
    IStripeMonitoring under = mock(IStripeMonitoring.class);
    Logger logger = mock(Logger.class);
    IStripeMonitoringWrapper instance = new IStripeMonitoringWrapper(Collections.singletonList(under), logger);
    // instance is passive
    instance.addNode(self, new String[] {"test"}, "name", mock(Serializable.class));
    verify(under, never()).addNode(any(), any(), any(), any());
    instance.removeNode(self, new String[] {"test"}, "name");
    verify(under, never()).removeNode(any(), any(), any());
  }

  /**
   * Test of serverDidJoinStripe method, of class IStripeMonitoringWrapper.
   */
  @Test
  public void testRequireLocalDeliveryOnPassiveDoesDeliverWhenRequested() {
    PlatformServer self = mock(PlatformServer.class);
    IStripeMonitoring under = mock(IStripeMonitoring.class);
    when(under.requiresLocalDeliveryOnPassive()).thenReturn(Boolean.TRUE);
    Logger logger = mock(Logger.class);
    IStripeMonitoringWrapper instance = new IStripeMonitoringWrapper(Collections.singletonList(under), logger);
    // instance is passive
    instance.addNode(self, new String[] {"test"}, "name", mock(Serializable.class));
    verify(under).addNode(any(), any(), any(), any());
    instance.removeNode(self, new String[] {"test"}, "name");
    verify(under).removeNode(any(), any(), any());
  }

  /**
   * Test of serverDidLeaveStripe method, of class IStripeMonitoringWrapper.
   */
  @Test
  public void testReplayIsDeliveredByDefault() {
    PlatformServer self = mock(PlatformServer.class);
    IStripeMonitoring under = mock(IStripeMonitoring.class);
    Mockito.doCallRealMethod().when(under).replayNode(any(), any(), any(), any());
    Logger logger = mock(Logger.class);
    IStripeMonitoringWrapper instance = new IStripeMonitoringWrapper(Collections.singletonList(under), logger);
    // instance is passive
    instance.replayNode(self, new String[] {"test"}, "name", mock(Serializable.class));
    verify(under).addNode(any(), any(), any(), any());
  }

  /**
   * Test of addNode method, of class IStripeMonitoringWrapper.
   */
  @Test
  public void testReplayDoesNothingIfOverridden() {
    PlatformServer self = mock(PlatformServer.class);
    IStripeMonitoring under = mock(IStripeMonitoring.class);
    Mockito.doNothing().when(under).replayNode(any(), any(), any(), any());
    Logger logger = mock(Logger.class);
    IStripeMonitoringWrapper instance = new IStripeMonitoringWrapper(Collections.singletonList(under), logger);
    // instance is passive
    instance.replayNode(self, new String[] {"test"}, "name", mock(Serializable.class));
    verify(under, never()).addNode(any(), any(), any(), any());
  }
}
