/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tcclient.ehcache.TimeExpiryMap;
import com.tctest.runner.AbstractTransparentApp;

public class TimeExpiryMapTestApp extends AbstractTransparentApp {
  private final CyclicBarrier barrier;
  private DataRoot            dataRoot = null;

  public TimeExpiryMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      basicMapTTLTest(index);
      basicMapTest(index);
      expiredItemsTest(index);

    } catch (Throwable t) {
      notifyError(t);
    }
  }
  
  private void basicMapTTLTest(int index) throws Exception {
    DebugUtil.DEBUG = true;
    if (index == 0) {
      dataRoot = new DataRoot();
      dataRoot.setMap(new MockTimeExpiryMap(1, 50, 5));
    }
    
    barrier.barrier();
    
    if (index == 0) {
      dataRoot.put("key1", "val1");
      dataRoot.put("key2", "val2");
      dataRoot.put("key3", "val3");
    }
    
    barrier.barrier();
    
    Thread.sleep(1000);
    
    barrier.barrier();
    
    Assert.assertEquals(3, dataRoot.size());
    Assert.assertFalse(dataRoot.isExpired("key1"));
    Assert.assertFalse(dataRoot.isExpired("key2"));
    Assert.assertFalse(dataRoot.isExpired("key3"));
    Assert.assertEquals("val1", dataRoot.get("key1"));
    Assert.assertEquals("val2", dataRoot.get("key2"));
    Assert.assertEquals("val3", dataRoot.get("key3"));
    
    barrier.barrier();
    
    Thread.sleep(9000);
    
    barrier.barrier();
    
    Assert.assertTrue(dataRoot.isExpired("key1"));
    Assert.assertTrue(dataRoot.isExpired("key2"));
    Assert.assertTrue(dataRoot.isExpired("key3"));
    Assert.assertEquals(0, dataRoot.size());
    Assert.assertEquals(3, dataRoot.getNumOfExpired());
    
    barrier.barrier();
    DebugUtil.DEBUG = false;
  }
  
  private void basicMapTest(int index) throws Exception {
    DebugUtil.DEBUG = true;
    
    if (index == 0) {
      dataRoot.setMap(new MockTimeExpiryMap(1, 5, 10));
    }
    
    barrier.barrier();
    
    if (index == 0) {
      dataRoot.put("key1", "val1");
      dataRoot.put("key2", "val2");
      dataRoot.put("key3", "val3");
    }
    
    barrier.barrier();
    
    Assert.assertEquals(3, dataRoot.size());
    Assert.assertEquals("val1", dataRoot.get("key1"));
    Assert.assertEquals("val2", dataRoot.get("key2"));
    Assert.assertEquals("val3", dataRoot.get("key3"));
    
    barrier.barrier();
    
    Thread.sleep(10000);
    
    barrier.barrier();
    
    Assert.assertEquals(0, dataRoot.size());
    
    barrier.barrier();
    
    Assert.assertEquals(3, dataRoot.getNumOfExpired());
    
    barrier.barrier();
    
    DebugUtil.DEBUG = false;
  }
  
  private void expiredItemsTest(int index) throws Exception {
    DebugUtil.DEBUG = true;
    if (index == 0) {
      dataRoot.setMap(new MockTimeExpiryMap(1, 5, 10));
    }
    
    barrier.barrier();
    
    if (index == 1) {
      dataRoot.put("key1", "val1");
      dataRoot.put("key2", "val2");
      dataRoot.put("key3", "val3");
    }
    
    barrier.barrier();
    
    Assert.assertEquals(3, dataRoot.size());
    Assert.assertEquals("val1", dataRoot.get("key1"));
    Assert.assertEquals("val2", dataRoot.get("key2"));
    Assert.assertEquals("val3", dataRoot.get("key3"));
    
    barrier.barrier();
    
    Thread.sleep(3000);
    
    if (index == 0) {
      Assert.assertEquals("val3", dataRoot.get("key3"));
      
      Thread.sleep(4000);
    }
    
    barrier.barrier();
    
    if (index == 0) {
      Assert.assertEquals(2, dataRoot.getNumOfExpired());
      Assert.assertEquals(null, dataRoot.get("key1"));
      Assert.assertEquals(null, dataRoot.get("key2"));
    }
    
    barrier.barrier();
    DebugUtil.DEBUG = false;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = TimeExpiryMapTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$DataRoot");
    config.addIncludePattern(testClass + "$MockTimeExpiryMap");
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("dataRoot", "dataRoot");
    
    config.addNewModule("clustered-ehcache-1.2.4", "1.0.0"); // this is just a quick way to add TimeExpiryMap to the instrumentation list
  }

  private static class DataRoot {
    private MockTimeExpiryMap map;

    public DataRoot() {
      super();
    }
    
    public synchronized void put(Object key, Object val) {
      map.put(key, val);
    }
    
    public synchronized Object get(Object key) {
      return map.get(key);
    }
    
    public synchronized int size() {
      return map.size();
    }
    
    public synchronized int getNumOfExpired() {
      return map.getNumOfExpired();
    }
    
    public synchronized void setMap(MockTimeExpiryMap map) {
      if (this.map != null) {
        this.map.stopTimeMonitoring();
      }
      this.map = map;
      this.map.initialize();
    }
    
    public synchronized boolean isExpired(Object key) {
      return map.isExpired(key);
    }
  }
  
  private static class MockTimeExpiryMap extends TimeExpiryMap {
    private int numOfExpired = 0;
    
    public MockTimeExpiryMap(int invalidatorSleepSeconds, int maxIdleTimeoutSeconds, int maxTTLSeconds) {
      super(invalidatorSleepSeconds, maxIdleTimeoutSeconds, maxTTLSeconds, "MockCache");
    }
    
    protected final synchronized void processExpired(Object key) {
      numOfExpired++;
      if (DebugUtil.DEBUG) {
        System.err.println("Client " + ManagerUtil.getClientID() + " expiring ... key: " + key + ", numOfExpired: " + numOfExpired);
      }
    }
    
    public synchronized int getNumOfExpired() {
      return this.numOfExpired;
    }
  }

}
