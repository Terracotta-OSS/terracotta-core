/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.io.FileUtils;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class EhcacheGlobalEvictionTestApp extends ServerCrashingAppBase {
  private final static int NUM_OF_L1 = 2;

  public EhcacheGlobalEvictionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
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
      jvmArgs.add("-D" + EmbeddedOSGiRuntime.TESTS_CONFIG_MODULE_REPOSITORIES + "=" + modules_url);
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

  public static class L1Client {
    private CyclicBarrier barrier = new CyclicBarrier(NUM_OF_L1); // root
    private CacheManager  cacheManager;                          // root
    private int           index;

    public L1Client(int index) {
      this.index = index;
      if (index == 0) {
        cacheManager = getCacheManager();
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
      Cache cache = cacheManager.getCache("sampleCache1");
      populateCache(cache, index, 1);
      barrier.barrier();

      Thread.sleep(1000);
      Assert.assertEquals(new Element("key01", "val01"), cache.get("key01"));
      Assert.assertEquals(new Element("key02", "val02"), cache.get("key02"));
      Assert.assertEquals(new Element("key03", "val03"), cache.get("key03"));
      Assert.assertEquals(new Element("key11", "val11"), cache.get("key11"));
      Assert.assertEquals(new Element("key12", "val12"), cache.get("key12"));
      Assert.assertEquals(new Element("key13", "val13"), cache.get("key13"));
      Assert.assertEquals(6, cache.getSize());

      Thread.sleep(20000);
      Assert.assertTrue(cache.isExpired(new Element("key01", "val01")));
      Assert.assertTrue(cache.isExpired(new Element("key02", "val02")));
      Assert.assertTrue(cache.isExpired(new Element("key03", "val03")));
      Assert.assertTrue(cache.isExpired(new Element("key11", "val11")));
      Assert.assertTrue(cache.isExpired(new Element("key12", "val12")));
      Assert.assertTrue(cache.isExpired(new Element("key13", "val13")));
      Assert.assertEquals(0, cache.getSize());

      barrier.barrier();

      populateCache(cache, index, 4);
      Thread.sleep(1000);
      Assert.assertEquals(new Element("key04", "val04"), cache.get("key04"));
      Assert.assertEquals(new Element("key05", "val05"), cache.get("key05"));
      Assert.assertEquals(new Element("key06", "val06"), cache.get("key06"));
      Assert.assertEquals(new Element("key14", "val14"), cache.get("key14"));
      Assert.assertEquals(new Element("key15", "val15"), cache.get("key15"));
      Assert.assertEquals(new Element("key16", "val16"), cache.get("key16"));
      Assert.assertEquals(6, cache.getSize());

      if (index == 0) {
        Thread.sleep(10000);

        Assert.assertTrue(cache.isExpired(new Element("key04", "val04")));
        Assert.assertTrue(cache.isExpired(new Element("key05", "val05")));
        Assert.assertTrue(cache.isExpired(new Element("key06", "val06")));
        Assert.assertTrue(cache.isExpired(new Element("key14", "val14")));
        Assert.assertTrue(cache.isExpired(new Element("key15", "val15")));
        Assert.assertTrue(cache.isExpired(new Element("key16", "val16")));
        Assert.assertEquals(0, cache.getSize());
      }
    }

    private CacheManager getCacheManager() {
      return CacheManager.create(getClass().getResource("cache-global-evictor-test.xml"));
    }

    private void populateCache(Cache cache, int index, int startValue) throws Exception {
      cache.put(new Element("key" + index + startValue, "val" + index + startValue));
      cache.put(new Element("key" + index + (startValue + 1), "val" + index + (startValue + 1)));
      cache.put(new Element("key" + index + (startValue + 2), "val" + index + (startValue + 2)));
    }
  }

}
