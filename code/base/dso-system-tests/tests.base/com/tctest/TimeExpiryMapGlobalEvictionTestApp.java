/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tcclient.ehcache.TimeExpiryMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TimeExpiryMapGlobalEvictionTestApp extends ServerCrashingAppBase {
  private final static int NUM_OF_L1 = 2;

  public TimeExpiryMapGlobalEvictionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void runTest() throws Throwable {
    basicGlobalEvictionTest();

  }

  private void basicGlobalEvictionTest() throws Exception {
    DebugUtil.DEBUG = true;

    final List jvmArgs = new ArrayList();
    String modules_url = System.getProperty(EmbeddedOSGiRuntime.TESTS_CONFIG_MODULE_REPOSITORIES);
    if (modules_url != null) {
      jvmArgs.add("-D"+EmbeddedOSGiRuntime.TESTS_CONFIG_MODULE_REPOSITORIES+"="+modules_url);
    }
    Thread t1 = new Thread(new Runnable() {
      public void run() {
        try {
          spawnNewClient("0", L1Client.class, new String[] { "0" }, jvmArgs);
        } catch (Exception e) {
          e.printStackTrace(System.err);
        }
      }

    });
    Thread t2 = new Thread(new Runnable() {
      public void run() {
        try {
          spawnNewClient("1", L1Client.class, new String[] { "1" }, jvmArgs);
        } catch (Exception e) {
          e.printStackTrace(System.err);
        }
      }

    });

    t1.start();
    t2.start();

    Thread.sleep(60000);

    DebugUtil.DEBUG = false;
  }

  protected ExtraL1ProcessControl spawnNewClient(String clientId, Class clientClass, String[] mainArgs, List jvmArgs)
      throws Exception {
    final String hostName = getHostName();
    final int port = getPort();
    final File configFile = new File(getConfigFilePath());
    File workingDir = new File(configFile.getParentFile(), "client-" + clientId);
    FileUtils.forceMkdir(workingDir);

    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, clientClass, configFile.getAbsolutePath(),
                                                             mainArgs, workingDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    client.waitFor();
    System.err.println("\n### Started New Client");
    return client;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = TimeExpiryMapGlobalEvictionTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$DataRoot");
    config.addIncludePattern(testClass + "$MockTimeExpiryMap");
    config.addIncludePattern(testClass + "$L1Client");
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec = config.getOrCreateSpec(testClass + "$L1Client");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("dataRoot", "dataRoot");

    config.addNewModule("clustered-ehcache-1.2.4", "1.0.0"); // this is just a quick way to add TimeExpiryMap to the
    // instrumentation list
  }

  public static class L1Client {
    private CyclicBarrier barrier = new CyclicBarrier(NUM_OF_L1);
    private DataRoot      dataRoot;
    private final int     index;

    public L1Client(int index) {
      this.index = index;
      if (index == 0) {
        dataRoot = new DataRoot();
        dataRoot.setMap(new MockTimeExpiryMap(3, 50, 6));
      }
    }

    public static void main(String args[]) throws Exception {
      DebugUtil.DEBUG = true;

      int index = Integer.parseInt(args[0]);
      L1Client l1 = new L1Client(index);
      l1.execute();

      DebugUtil.DEBUG = false;
    }

    public void execute() throws Exception {
      barrier.barrier();
      addData(index, 1);
      barrier.barrier();
      
      Thread.sleep(1000);
      Assert.assertEquals("val01", dataRoot.get("key01"));
      Assert.assertEquals("val02", dataRoot.get("key02"));
      Assert.assertEquals("val03", dataRoot.get("key03"));
      Assert.assertEquals("val11", dataRoot.get("key11"));
      Assert.assertEquals("val12", dataRoot.get("key12"));
      Assert.assertEquals("val13", dataRoot.get("key13"));
      Assert.assertEquals(6, dataRoot.size());
      
      Thread.sleep(10000);
      Assert.assertTrue(dataRoot.isExpired("key01"));
      Assert.assertTrue(dataRoot.isExpired("key02"));
      Assert.assertTrue(dataRoot.isExpired("key03"));
      Assert.assertTrue(dataRoot.isExpired("key11"));
      Assert.assertTrue(dataRoot.isExpired("key12"));
      Assert.assertTrue(dataRoot.isExpired("key13"));
      Assert.assertEquals(0, dataRoot.size());
      Assert.assertEquals(6, dataRoot.getNumOfEvicted());
      
      barrier.barrier();
      
      addData(index, 4);
      Thread.sleep(1000);
      Assert.assertEquals("val04", dataRoot.get("key04"));
      Assert.assertEquals("val05", dataRoot.get("key05"));
      Assert.assertEquals("val06", dataRoot.get("key06"));
      Assert.assertEquals("val14", dataRoot.get("key14"));
      Assert.assertEquals("val15", dataRoot.get("key15"));
      Assert.assertEquals("val16", dataRoot.get("key16"));
      Assert.assertEquals(6, dataRoot.size());
      
      if (index == 0) {
        Thread.sleep(20000);

        Assert.assertTrue(dataRoot.isExpired("key04"));
        Assert.assertTrue(dataRoot.isExpired("key05"));
        Assert.assertTrue(dataRoot.isExpired("key06"));
        Assert.assertTrue(dataRoot.isExpired("key14"));
        Assert.assertTrue(dataRoot.isExpired("key15"));
        Assert.assertTrue(dataRoot.isExpired("key16"));
        Assert.assertEquals(0, dataRoot.size());
        Assert.assertEquals(12, dataRoot.getNumOfEvicted());
      }
    }

    private void addData(int index, int startIndex) {
      dataRoot.put("key" + index + startIndex, "val" + index + startIndex);
      dataRoot.put("key" + index + (startIndex+1), "val" + index + (startIndex+1));
      dataRoot.put("key" + index + (startIndex+2), "val" + index + (startIndex+2));
    }
  }

  public static class DataRoot {
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

    public synchronized int getNumOfEvicted() {
      return map.getNumOfEvicted();
    }

    public synchronized void setMap(MockTimeExpiryMap map) {
      this.map = map;
      this.map.initialize();
    }

    public synchronized boolean isExpired(Object key) {
      return map.isExpired(key);
    }
  }

  public static class MockTimeExpiryMap extends TimeExpiryMap {
    private int numOfEvicted = 0;

    public MockTimeExpiryMap(int invalidatorSleepSeconds, int maxIdleTimeoutSeconds, int maxTTLSeconds) {
      super(invalidatorSleepSeconds, maxIdleTimeoutSeconds, maxTTLSeconds, "MockCache", true, 3, 4, 2);
    }

    protected final synchronized void processExpired(Object key) {
      numOfEvicted++;
      if (DebugUtil.DEBUG) {
        System.err.println("Client " + ManagerUtil.getClientID() + " expiring ... key: " + key + ", numOfExpired: "
                           + numOfEvicted);
      }
    }

    public synchronized int getNumOfEvicted() {
      return this.numOfEvicted;
    }
  }

}
