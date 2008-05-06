/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
    final List jvmArgs = new ArrayList();
    final List errorList = Collections.synchronizedList(new ArrayList());
    final L1ClientWrapper l1Wrapper = new L1ClientWrapper(getHostName(), getPort(), new File(getConfigFilePath()));

    addTestTcPropertiesFile(jvmArgs);

    Thread t1 = new Thread(new Runnable() {
      public void run() {
        try {
          l1Wrapper.spawn("0", L1Client.class, new String[] { "0" }, jvmArgs);
        } catch (Exception e) {
          errorList.add(e);
        }
      }
    });

    Thread t2 = new Thread(new Runnable() {
      public void run() {
        try {
          l1Wrapper.spawn("1", L1Client.class, new String[] { "1" }, jvmArgs);
        } catch (Exception e) {
          errorList.add(e);
        }
      }
    });

    t1.start();
    t2.start();

    Thread.sleep(60000L);

    t1.join();
    t2.join();

    if (errorList.size() > 0) { throw (Exception) errorList.get(0); }
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
      Logger rootLogger = Logger.getRootLogger();
      if (!rootLogger.getAllAppenders().hasMoreElements()) {
        rootLogger.setLevel(org.apache.log4j.Level.DEBUG);
        rootLogger.addAppender(new ConsoleAppender(new PatternLayout("%-5p [%t]: %m%n")));
      }
      int index = Integer.parseInt(args[0]);
      L1Client l1 = new L1Client(index);
      l1.execute();
    }

    public void execute() throws Exception {
      barrier.barrier();
      Cache cache = cacheManager.getCache("sampleCache1");
      populateCache(cache, index, 1);
      barrier.barrier();

      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key01", "val01"), cache.get("key01"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key02", "val02"), cache.get("key02"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key03", "val03"), cache.get("key03"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key11", "val11"), cache.get("key11"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key12", "val12"), cache.get("key12"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key13", "val13"), cache.get("key13"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), 6, cache.getSize());

      Thread.sleep(40000);
      Assert.assertTrue("Client " + ManagerUtil.getClientID(), cache.isExpired(new Element("key01", "val01")));
      Assert.assertTrue("Client " + ManagerUtil.getClientID(), cache.isExpired(new Element("key02", "val02")));
      Assert.assertTrue("Client " + ManagerUtil.getClientID(), cache.isExpired(new Element("key03", "val03")));
      Assert.assertTrue("Client " + ManagerUtil.getClientID(), cache.isExpired(new Element("key11", "val11")));
      Assert.assertTrue("Client " + ManagerUtil.getClientID(), cache.isExpired(new Element("key12", "val12")));
      Assert.assertTrue("Client " + ManagerUtil.getClientID(), cache.isExpired(new Element("key13", "val13")));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), 0, cache.getSize());

      barrier.barrier();

      populateCache(cache, index, 4);
      barrier.barrier();

      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key04", "val04"), cache.get("key04"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key05", "val05"), cache.get("key05"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key06", "val06"), cache.get("key06"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key14", "val14"), cache.get("key14"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key15", "val15"), cache.get("key15"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), new Element("key16", "val16"), cache.get("key16"));
      Assert.assertEquals("Client " + ManagerUtil.getClientID(), 6, cache.getSize());

      barrier.barrier();

      if (index == 0) {
        Thread.sleep(80000);

        Assert.assertTrue(cache.isExpired(new Element("key04", "val04")));
        Assert.assertTrue(cache.isExpired(new Element("key05", "val05")));
        Assert.assertTrue(cache.isExpired(new Element("key06", "val06")));
        Assert.assertTrue(cache.isExpired(new Element("key14", "val14")));
        Assert.assertTrue(cache.isExpired(new Element("key15", "val15")));
        Assert.assertTrue(cache.isExpired(new Element("key16", "val16")));

        System.out.println("Cache content: " + cache);

        Assert.assertEquals(0, cache.getSize());
      }
    }

    private CacheManager getCacheManager() {
      return CacheManager.create(getClass().getResource("cache-global-evictor-test.xml"));
    }

    private void populateCache(Cache cache, int indexArg, int startValue) throws Exception {
      cache.put(new Element("key" + indexArg + startValue, "val" + indexArg + startValue));
      cache.put(new Element("key" + indexArg + (startValue + 1), "val" + indexArg + (startValue + 1)));
      cache.put(new Element("key" + indexArg + (startValue + 2), "val" + indexArg + (startValue + 2)));
    }
  }

  private static class L1ClientWrapper {
    private String hostname;
    private int    port;
    private File   configFile;

    public L1ClientWrapper(String hostname, int port, File configFile) {
      this.hostname = hostname;
      this.port = port;
      this.configFile = configFile;
    }

    public void spawn(String clientId, Class clientClass, String[] mainArgs, List jvmArgs) throws Exception {
      ExtraL1ProcessControl client = spawnNewClient(clientId, clientClass, mainArgs, jvmArgs);
      if (client.waitFor() != 0) { throw new Exception(clientClass.getName() + " exited with non zero code"); }
    }

    protected ExtraL1ProcessControl spawnNewClient(String clientId, Class clientClass, String[] mainArgs, List jvmArgs)
        throws Exception {

      File workingDir = new File(configFile.getParentFile(), "client-" + clientId);
      FileUtils.forceMkdir(workingDir);

      ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostname, port, clientClass, configFile
          .getAbsolutePath(), mainArgs, workingDir, jvmArgs);
      client.start();
      System.err.println("\n### Started New Client");

      client.mergeSTDERR();
      client.mergeSTDOUT();

      return client;
    }
  }

}
