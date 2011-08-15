/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

public class LinkedBlockingQueueSpawningTestApp extends AbstractTransparentApp {
  public static String                               CONFIG_FILE        = "config-file";

  private static final long                          SPAWN_SLEEP_PERIOD = 1000;
  private static final int                           SPAWN_COUNT        = 4;
  private static final int                           PRODUCERS          = 5;
  private static final int                           CONSUMERS          = 3;
  private static final int                           LBQ_CAPACITY       = 300;
  private static final long                          CLIENT_RUNTIME     = 30 * 1000;

  // roots
  private static final LinkedBlockingQueue<WorkItem> queue              = new LinkedBlockingQueue<WorkItem>(
                                                                                                            LBQ_CAPACITY);
  private static final Object                        inputLock          = new Object();
  private static final Object                        outputLock         = new Object();
  private static final AtomicLong                    counter            = new AtomicLong(0L);
  private static final AtomicLong                    outCounter         = new AtomicLong(0L);

  // end roots
  private final ApplicationConfig                    config;

  public LinkedBlockingQueueSpawningTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.config = cfg;
    System.out.println("XXX Test for spawning LBQClient " + SPAWN_COUNT + " times and sleep " + SPAWN_SLEEP_PERIOD
                       + "ms in between.");
  }

  public void run() {

    try {
      for (int i = 0; i < SPAWN_COUNT; ++i) {
        spawnNewClientAndWaitForCompletion("" + i, LBQClient.class);
        ThreadUtil.reallySleep(SPAWN_SLEEP_PERIOD);
      }
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LinkedBlockingQueueSpawningTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "*");
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

    spec.addRoot("queue", "queue");
    spec.addRoot("counter", "counter");
    spec.addRoot("outCounter", "outCounter");
    spec.addRoot("inputLock", "inputLock");
    spec.addRoot("outputLock", "outputLock");
  }

  protected ExtraL1ProcessControl spawnNewClientAndWaitForCompletion(String clientID, Class clientClass)
      throws Exception {
    final String hostName = "localhost";
    final int port = 0;
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-" + clientID);
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, clientClass, configFile.getAbsolutePath(),
                                                             Collections.EMPTY_LIST, workingDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    Assert.assertEquals("Client exits with error", 0, client.waitFor());
    return client;
  }

  protected final void addTestTcPropertiesFile(List jvmArgs) {
    URL url = getClass().getResource("/com/tc/properties/tests.properties");
    if (url == null) { return; }
    String pathToTestTcProperties = url.getPath();
    if (pathToTestTcProperties == null || pathToTestTcProperties.equals("")) { return; }
    jvmArgs.add("-Dcom.tc.properties=" + pathToTestTcProperties);
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
    private final ExecutorService pool     = Executors.newFixedThreadPool(PRODUCERS + CONSUMERS);

    private final long            timeout;
    private long                  runtime;
    private int                   exitCode = 0;

    public LBQClient(long runtime) {

      this.runtime = runtime;
      this.timeout = runtime > 0 ? System.currentTimeMillis() + runtime : 0;

    }

    public void run() {
      try {
        System.out.println("Starting... with counter=" + counter.get() + " counter:"
                           + ((Manageable) counter).__tc_managed().getObjectID());

        for (int i = 0; i < PRODUCERS; i++) {
          pool.execute(new Producer(this, timeout));
        }

        for (int i = 0; i < CONSUMERS; i++) {
          pool.execute(new Consumer(this, timeout));
        }

      } catch (Throwable e) {
        exitCode = 1;
        throw new RuntimeException(e);
      }
    }

    public void setFail() {
      exitCode = 1;
    }

    public void await() {
      try {
        if (runtime == 0) {
          pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } else {
          pool.awaitTermination(runtime + 60000L, TimeUnit.MILLISECONDS);
        }
      } catch (InterruptedException e) {
        System.out.println("Got interrupted while waiting for all threads to finish");
        exitCode = 1;
      }
    }

    public static void main(String[] args) {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          System.out.println(LBQClient.class.getName() + " shutdown.");
        }
      });

      final LBQClient t = new LBQClient(CLIENT_RUNTIME);
      t.run();
      t.await();
      System.out.println("One client done. exit=" + t.exitCode);
      System.exit(t.exitCode);
    }
  }

  public static class Producer implements Runnable {
    private long timeout;

    public Producer(LBQClient driver, long timeout) {
      this.timeout = timeout;
    }

    public void run() {
      try {
        while (canRun()) {
          WorkItem d = null;
          synchronized (inputLock) {
            if (queue.remainingCapacity() > 0) {
              d = new WorkItem(counter.getAndIncrement());
              queue.put(d);
            }
          }
          if (d != null && d.getID() % 100 == 0) {
            System.out.println("XXX produce " + d.getID());
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
    private long            timeout;
    private final LBQClient client;

    public Consumer(LBQClient client, long timeout) {
      this.timeout = timeout;
      this.client = client;
    }

    public void run() {
      try {
        while (canRun()) {
          WorkItem data = null;
          synchronized (outputLock) {
            if (queue.size() > 0) {
              data = queue.take();
              Assert.assertEquals("Sequence mismatch!", outCounter.getAndIncrement(), data.getID());
            }
          }
          if (data != null) {
            if (data.getID() % 100 == 0) {
              System.out.println("XXX consume " + data.getID());
            }
          } else {
            Thread.sleep(3);
          }
        }
      } catch (Throwable e) {
        client.setFail();
        e.printStackTrace();
        throw new TCRuntimeException(e);
      }
    }

    private boolean canRun() {
      return timeout == 0L || System.currentTimeMillis() < timeout;
    }
  }

}
