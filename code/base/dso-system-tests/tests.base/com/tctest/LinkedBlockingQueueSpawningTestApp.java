/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.process.StreamCopier;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

public class LinkedBlockingQueueSpawningTestApp extends AbstractTransparentApp {
  private static final Logger                        LOG                = Logger
                                                                            .getLogger(LinkedBlockingQueueSpawningTestApp.class);
  public static String                               CONFIG_FILE        = "config-file";

  private static final long                          SPAWN_SLEEP_PERIOD = 1000;
  private static final int                           SPAWN_COUNT        = 10;
  private static final int                           PRODUCERS          = 5;
  private static final int                           CONSUMERS          = 3;
  private static final int                           LBQ_CAPACITY       = 300;
  private static final int                           CLIENT_RUNTIME     = 30 * 1000;

  // roots
  private static final LinkedBlockingQueue<WorkItem> queue              = new LinkedBlockingQueue<WorkItem>(
                                                                                                            LBQ_CAPACITY);
  private static final Object                        inputLock          = new Object();
  private static final Object                        outputLock         = new Object();
  private static final AtomicLong                    counter            = new AtomicLong(0L);
  private static final AtomicLong                    outCounter         = new AtomicLong(0L);

  // end roots

  public LinkedBlockingQueueSpawningTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    System.setProperty("tc.config", cfg.getAttribute(CONFIG_FILE));

    System.out.println("XXX Test for spawning LBQClient " + SPAWN_COUNT + " times and sleep " + SPAWN_SLEEP_PERIOD
                       + "ms in between.");
  }

  public void run() {

    Thread spawnerThread = new Thread(new Spawner(), "Spawner");
    spawnerThread.start();
    try {
      spawnerThread.join();
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LinkedBlockingQueueSpawningTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + WorkItem.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + LBQClient.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + Producer.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + Consumer.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + Spawner.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + Utils.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("queue", "queue");
    spec.addRoot("counter", "counter");
    spec.addRoot("outCounter", "outCounter");
    spec.addRoot("inputLock", "inputLock");
    spec.addRoot("outputLock", "outputLock");
  }

  public static class WorkItem {
    private final long id;

    public WorkItem(long id) {
      this.id = id;
    }

    public long getID() {
      return this.id;
    }
  }

  public static class LBQClient {
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private final long            timeout;
    private long                  runtime;

    public LBQClient(long runtime) {

      this.runtime = runtime;
      this.timeout = runtime > 0 ? System.currentTimeMillis() + runtime : 0;

    }

    public void run() {
      try {
        LOG.info("Starting... with counter=" + counter.get() + " counter:"
                 + ((Manageable) counter).__tc_managed().getObjectID());

        for (int i = 0; i < PRODUCERS; i++) {
          pool.execute(new Producer(this, timeout));
        }

        for (int i = 0; i < CONSUMERS; i++) {
          pool.execute(new Consumer(this, timeout));
        }

      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }

    public void await() {
      try {
        if (runtime == 0) {
          pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } else {
          pool.awaitTermination(runtime + 200L, TimeUnit.MILLISECONDS);
        }
      } catch (InterruptedException e) {
        LOG.error("Got interrupted while waiting for all threads to finish");
        System.exit(1); // not a graceful exit
      }
    }

    public static void main(String[] args) {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          LOG.info(LBQClient.class.getName() + " shutdown.");
        }
      });

      final LBQClient t = new LBQClient(CLIENT_RUNTIME);
      t.run();
      t.await();
    }

  }

  public static class Producer implements Runnable {
    private long timeout;

    public Producer(LBQClient driver, long timeout) {
      this.timeout = timeout;
    }

    public void run() {
      try {
        while (canRun() || (queue.size() < CONSUMERS)) {
          synchronized (inputLock) {
            WorkItem d = new WorkItem(counter.getAndIncrement());
            queue.put(d);
            LOG.info("XXX produce " + d.getID());
          }

          Thread.sleep(3);
        }
      } catch (Throwable e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    private boolean canRun() {
      return timeout == 0L || System.currentTimeMillis() < timeout;
    }
  }

  public static class Consumer implements Runnable {
    private long timeout;

    public Consumer(LBQClient driver, long timeout) {
      this.timeout = timeout;
    }

    public void run() {
      try {
        while (canRun() || (queue.remainingCapacity() < PRODUCERS)) {
          synchronized (outputLock) {
            WorkItem data = queue.take();
            Assert.assertEquals("Sequence mismatch!", outCounter.getAndIncrement(), data.getID());
            LOG.info("XXX consume " + data.getID());
          }
        }
      } catch (Throwable e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    private boolean canRun() {
      return timeout == 0L || System.currentTimeMillis() < timeout;
    }
  }

  public class Spawner implements Runnable {

    private List<String> spawnL1cmd() {
      List<String> cmdList = new ArrayList<String>();
      cmdList.add(Utils.getProperty("java.home") + "/bin/java");
      Enumeration<?> e = System.getProperties().propertyNames();
      while (e.hasMoreElements()) {
        String prop = (String) e.nextElement();
        if (prop.startsWith("tc.") || prop.startsWith("lbq.")) {
          cmdList.add("-D" + prop + "=" + quoteIfNeeded(Utils.getProperty(prop)));
        }
      }
      cmdList.add("-Xbootclasspath/p:" + Utils.getProperty("sun.boot.class.path").split(File.pathSeparator)[0]);
      cmdList.add("-Dtc.nodeName=spawn");
      cmdList.add("-cp");
      cmdList.add(quoteIfNeeded(Utils.getProperty("java.class.path")));
      cmdList.add(com.tctest.LinkedBlockingQueueSpawningTestApp.LBQClient.class.getName());
      return cmdList;
    }

    private Process createNewL1() throws IOException {
      List<String> cmdList = spawnL1cmd();
      LOG.info("Spawn cmd: " + cmdList);
      Process p = Runtime.getRuntime().exec(cmdList.toArray(new String[0]), null,
                                            new File(Utils.getProperty("user.dir")));
      Utils.merge(p.getInputStream(), System.out);
      Utils.merge(p.getErrorStream(), System.err);
      return p;
    }

    private String quoteIfNeeded(String path) {
      if (path.indexOf(" ") > 0) { return "\"" + path + "\""; }
      return path;
    }

    public void run() {
      try {
        for (int i = 0; i < SPAWN_COUNT; ++i) {
          LOG.info("Spawning LBQLongRunning...");
          Process l1 = createNewL1();
          int returnedCode = l1.waitFor();
          IOUtils.closeQuietly(l1.getInputStream());
          IOUtils.closeQuietly(l1.getOutputStream());
          IOUtils.closeQuietly(l1.getErrorStream());
          l1.destroy();

          if (returnedCode != 0) {
            LOG.error("L1 returned code: " + returnedCode);
            System.exit(-1);
          } else {
            LOG.info("L1 finished.");
          }
          Thread.sleep(SPAWN_SLEEP_PERIOD);
        }
      } catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  public static class Utils {
    private static final Properties props = new Properties();
    static {
      props.putAll(System.getProperties());
    }

    public static String getProperty(String key) {
      return props.getProperty(key);
    }

    public static void merge(InputStream in, OutputStream out) {
      StreamCopier sc = new StreamCopier(in, out);
      sc.start();
    }
  }

}
