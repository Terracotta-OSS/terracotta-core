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

import java.io.Serializable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

import com.tc.services.LocalMonitoringProducer.ActivePipeWrapper;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BestEffortsMonitoringTest {
  private TestTimeSource source;
  private SingleThreadedTimer timer;
  private BestEffortsMonitoring monitoring;


  @Before
  public void setUp() throws Exception {
    this.source = new TestTimeSource(1);
    this.timer = new SingleThreadedTimer(this.source);
    this.monitoring = new BestEffortsMonitoring(this.timer);
    this.timer.start();
  }

  @After
  public void tearDown() throws Exception {
    this.timer.stop();
  }

  @Test
  public void testNoActive() throws Exception {
    // Enqueue a bunch of data and just stop.
    long id1 = 1;
    long id2 = 2;
    String name1 = "name1";
    String name2 = "name2";
    Serializable data1 = "data1";
    Serializable data2 = "data2";
    this.monitoring.pushBestEfforts(id1, name1, data1);
    this.monitoring.pushBestEfforts(id2, name2, data2);
  }

  @Test
  public void testImmediateActive() throws Exception {
    // Change to active immediate, with no data.
    TestPipeWrapper wrapper = new TestPipeWrapper();
    this.monitoring.attachToNewActive(wrapper);
    // Nothing should have happened.
    Assert.assertEquals(0, wrapper.pushCount);
    
    // Step time ahead to make sure that nothing happens.
    // (we currently push these, immediately when a new active arrives, so this check is just to make sure we don't enqueue something else).
    this.source.passTime(BestEffortsMonitoring.ASYNC_FLUSH_DELAY_MILLIS);
    this.timer.poke();
    Assert.assertEquals(0, wrapper.pushCount);
  }

  @Test
  public void testBecomeActive() throws Exception {
    // Enqueue some data, then change to active.
    long id1 = 1;
    long id2 = 2;
    String name1 = "name1";
    String name2 = "name2";
    Serializable data1 = "data1";
    Serializable data2 = "data2";
    this.monitoring.pushBestEfforts(id1, name1, data1);
    this.monitoring.pushBestEfforts(id2, name2, data2);
    
    TestStripeMonitoring consumer1 = new TestStripeMonitoring();
    TestStripeMonitoring consumer2 = new TestStripeMonitoring();
    TerracottaServiceProviderRegistry globalRegistry = mockRegistry(consumer1, consumer2);
    this.monitoring.flushAfterActivePromotion(mock(PlatformServer.class), globalRegistry);
    Assert.assertEquals(1, consumer1.pushCount);
    Assert.assertEquals(1, consumer2.pushCount);
  }

  @Test
  public void testBecomeActiveWithRedundantWrites() throws Exception {
    // Enqueue some data to the same key, then change to active.
    long id1 = 1;
    String name1 = "name1";
    Serializable data1 = "data1";
    Serializable data2 = "data2";
    this.monitoring.pushBestEfforts(id1, name1, data1);
    this.monitoring.pushBestEfforts(id1, name1, data2);
    
    TestStripeMonitoring consumer1 = new TestStripeMonitoring();
    TestStripeMonitoring consumer2 = new TestStripeMonitoring();
    TerracottaServiceProviderRegistry globalRegistry = mockRegistry(consumer1, consumer2);
    this.monitoring.flushAfterActivePromotion(mock(PlatformServer.class), globalRegistry);
    Assert.assertEquals(1, consumer1.pushCount);
    Assert.assertEquals(0, consumer2.pushCount);
  }

  @Test
  public void testBecomeActiveWhileInFlight() throws Exception {
    // Attach an active, enqueue some data, then change to active while a timer is still pending.
    TestPipeWrapper wrapper = new TestPipeWrapper();
    this.monitoring.attachToNewActive(wrapper);
    Assert.assertEquals(0, wrapper.pushCount);
    
    long id1 = 1;
    String name1 = "name1";
    Serializable data1 = "data1";
    this.monitoring.pushBestEfforts(id1, name1, data1);
    
    TestStripeMonitoring consumer1 = new TestStripeMonitoring();
    TerracottaServiceProviderRegistry globalRegistry = mockRegistry(consumer1, null);
    this.monitoring.flushAfterActivePromotion(mock(PlatformServer.class), globalRegistry);
    Assert.assertEquals(1, consumer1.pushCount);
    
    // Step time ahead to make sure that nothing happens.
    this.source.passTime(BestEffortsMonitoring.ASYNC_FLUSH_DELAY_MILLIS);
    this.timer.poke();
  }

  @Test
  public void testActiveLater() throws Exception {
    // Enqueue some data, then attach active.
    long id1 = 1;
    String name1 = "name1";
    Serializable data1 = "data1";
    this.monitoring.pushBestEfforts(id1, name1, data1);
    
    TestPipeWrapper wrapper = new TestPipeWrapper();
    this.monitoring.attachToNewActive(wrapper);
    // These should be pushed, immediately (at least in the current implementation).
    Assert.assertEquals(1, wrapper.pushCount);
    
    // Step time ahead to make sure nothing changes.
    this.source.passTime(BestEffortsMonitoring.ASYNC_FLUSH_DELAY_MILLIS);
    this.timer.poke();
    
    // The same count should be there.
    Assert.assertEquals(1, wrapper.pushCount);
  }

  @Test
  public void testActiveChange() throws Exception {
    // Enqueue some data, then attach active, then continue to enqueue data, then attach a new active (after any active timers have finished).
    long id1 = 1;
    String name1 = "name1";
    Serializable data1 = "data1";
    this.monitoring.pushBestEfforts(id1, name1, data1);
    
    TestPipeWrapper wrapper = new TestPipeWrapper();
    this.monitoring.attachToNewActive(wrapper);
    // These should be pushed, immediately (at least in the current implementation).
    Assert.assertEquals(1, wrapper.pushCount);
    
    // Step time ahead to make sure nothing changes.
    this.source.passTime(BestEffortsMonitoring.ASYNC_FLUSH_DELAY_MILLIS);
    this.timer.poke();
    
    // Add some more data (we can send redundant data since we already received it).
    this.monitoring.pushBestEfforts(id1, name1, data1);
    // Nothing should change until the timer.
    Assert.assertEquals(1, wrapper.pushCount);
    this.source.passTime(BestEffortsMonitoring.ASYNC_FLUSH_DELAY_MILLIS);
    this.timer.poke();
    Assert.assertEquals(2, wrapper.pushCount);
    
    // Now, switch to a new active - it should see nothing.
    TestPipeWrapper lateWrapper = new TestPipeWrapper();
    this.monitoring.attachToNewActive(lateWrapper);
    Assert.assertEquals(0, lateWrapper.pushCount);
  }

  @Test
  public void testActiveChangeWhileInFlight() throws Exception {
    // Enqueue some data, then attach active, then continue to enqueue data, then attach a new active while a timer is still
    //  pending.
    long id1 = 1;
    String name1 = "name1";
    Serializable data1 = "data1";
    this.monitoring.pushBestEfforts(id1, name1, data1);
    
    TestPipeWrapper wrapper = new TestPipeWrapper();
    this.monitoring.attachToNewActive(wrapper);
    // These should be pushed, immediately (at least in the current implementation).
    Assert.assertEquals(1, wrapper.pushCount);
    
    // Step time ahead to make sure nothing changes.
    this.source.passTime(BestEffortsMonitoring.ASYNC_FLUSH_DELAY_MILLIS);
    this.timer.poke();
    
    // Add some more data (we can send redundant data since we already received it).
    this.monitoring.pushBestEfforts(id1, name1, data1);
    // Nothing should change until the timer.
    Assert.assertEquals(1, wrapper.pushCount);
    // (a timer will have been requested so attach the new passive while there).
    
    // Now, switch to a new active - it should see the data.
    TestPipeWrapper lateWrapper = new TestPipeWrapper();
    this.monitoring.attachToNewActive(lateWrapper);
    Assert.assertEquals(1, lateWrapper.pushCount);
    
    // Wait to make sure neither change from a stale timer (it should have been cancelled).
    this.source.passTime(BestEffortsMonitoring.ASYNC_FLUSH_DELAY_MILLIS);
    this.timer.poke();
    Assert.assertEquals(1, wrapper.pushCount);
    Assert.assertEquals(1, lateWrapper.pushCount);
  }


  @SuppressWarnings("unchecked")
  private TerracottaServiceProviderRegistry mockRegistry(IStripeMonitoring consumer1, IStripeMonitoring consumer2) throws Exception {
    TerracottaServiceProviderRegistry globalRegistry = mock(TerracottaServiceProviderRegistry.class);
    if (null != consumer1) {
      InternalServiceRegistry registry1 = mock(InternalServiceRegistry.class);
      when(registry1.getService(any(ServiceConfiguration.class))).thenReturn(consumer1);
      when(globalRegistry.subRegistry(1)).thenReturn(registry1);
    }
    if (null != consumer2) {
      InternalServiceRegistry registry2 = mock(InternalServiceRegistry.class);
      when(registry2.getService(any(ServiceConfiguration.class))).thenReturn(consumer2);
      when(globalRegistry.subRegistry(2)).thenReturn(registry2);
    }
    return globalRegistry;
  }


  private static class TestPipeWrapper implements ActivePipeWrapper {
    public int pushCount = 0;
    
    @Override
    public void addNode(long consumerID, String[] parents, String name, Serializable value) {
      // No call expected.
      Assert.fail();
    }
    @Override
    public void removeNode(long consumerID, String[] parents, String name) {
      // No call expected.
      Assert.fail();
    }
    @Override
    public void pushBestEffortsBatch(long[] consumerIDs, String[] keys, Serializable[] values) {
      this.pushCount += 1;
    }
  }


  private static class TestStripeMonitoring implements IStripeMonitoring {
    public int pushCount = 0;
    
    @Override
    public void serverDidBecomeActive(PlatformServer self) {
      // No call expected.
      Assert.fail();
    }
    @Override
    public void serverDidJoinStripe(PlatformServer server) {
      // No call expected.
      Assert.fail();
    }
    @Override
    public void serverDidLeaveStripe(PlatformServer server) {
      // No call expected.
      Assert.fail();
    }
    @Override
    public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value) {
      // No call expected.
      Assert.fail();
      return false;
    }
    @Override
    public boolean removeNode(PlatformServer sender, String[] parents, String name) {
      // No call expected.
      Assert.fail();
      return false;
    }
    @Override
    public void pushBestEffortsData(PlatformServer sender, String name, Serializable data) {
      this.pushCount += 1;
    }
  }
}
