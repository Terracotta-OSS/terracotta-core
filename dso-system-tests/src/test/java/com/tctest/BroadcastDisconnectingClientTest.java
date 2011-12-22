/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BroadcastDisconnectingClientTest extends ServerCrashingTestBase {

  private static final int   INTERNAL_CLIENT_COUNT  = 3;                                    // includes one that just
  // spawns external clients
  static final int           LONG_RUNNERS_DURATION  = 10 * 60 * 1000;
  static final int           SHORT_RUNNERS_DURATION = 30 * 1000;
  static final int           SHORT_RUNNERS_INTERVAL = 30 * 1000;
  static final int           LIST_MAX               = 25;
  static final int           INNER_LIST_SIZE        = 1;
  private static final int   TIMEOUT_SECONDS        = 25 * 60;

  private static final Class externalClientClass    = DisconnectingALClient.class;
  private static final Class internalClientClass    = BroadcastDisconnectingClientApp.class;

  public BroadcastDisconnectingClientTest() {
    super(INTERNAL_CLIENT_COUNT, new String[] { "-Xmx512m", "-Xms512m" });
  }

  @Override
  protected Class getApplicationClass() {
    return BroadcastDisconnectingClientApp.class;
  }

  @Override
  public int getTimeoutValueInSeconds() {
    return TIMEOUT_SECONDS;
  }

  @Override
  protected void createConfig(TerracottaConfigBuilder cb) {

    InstrumentedClassConfigBuilder[] instrClasses = new InstrumentedClassConfigBuilder[] {
        new InstrumentedClassConfigBuilderImpl(SynchronizedInt.class),
        new InstrumentedClassConfigBuilderImpl(internalClientClass),
        new InstrumentedClassConfigBuilderImpl(externalClientClass) };
    cb.getApplication().getDSO().setInstrumentedClasses(instrClasses);

    RootConfigBuilder[] roots = new RootConfigBuilder[] {
        new RootConfigBuilderImpl(internalClientClass, "arrayList", "arrayList"),
        new RootConfigBuilderImpl(externalClientClass, "arrayList", "arrayList"), };
    cb.getApplication().getDSO().setRoots(roots);

    LockConfigBuilder[] locks = new LockConfigBuilder[] {
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, SynchronizedInt.class, LockConfigBuilder.LEVEL_WRITE),
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, internalClientClass, LockConfigBuilder.LEVEL_WRITE),
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, externalClientClass, LockConfigBuilder.LEVEL_WRITE) };
    cb.getApplication().getDSO().setLocks(locks);
  }

  public static class BroadcastDisconnectingClientApp extends ServerCrashingAppBase {

    final ArrayList<ArrayList> arrayList = new ArrayList<ArrayList>();

    public BroadcastDisconnectingClientApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
      super(appId, config, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addIncludePattern(internalClientClass.getName());
      config.addIncludePattern(externalClientClass.getName());

      String internalClientName = internalClientClass.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(internalClientName);
      spec.addRoot("arrayList", "arrayList");
      String methodExpression = "* " + internalClientName + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      String externalClientName = externalClientClass.getName();
      TransparencyClassSpec spec2 = config.getOrCreateSpec(externalClientName);
      spec2.addRoot("arrayList", "arrayList");
      methodExpression = "* " + externalClientName + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      new SynchronizedIntSpec().visit(visitor, config);
    }

    @Override
    public void runTest() throws Exception {

      boolean first;
      synchronized (arrayList) {
        if (arrayList.size() == 0) {
          arrayList.add(new ArrayList(0));
          first = true;
        } else {
          first = false;
        }
      }

      if (first) {
        spawnDisconnectingClients();
      } else {
        runTestApp();
      }
    }

    private void runTestApp() {

      log("Starting internal client " + getApplicationId() + " with duration " + LONG_RUNNERS_DURATION);

      Stopwatch stopwatch = new Stopwatch().start();
      while (stopwatch.getElapsedTime() < (LONG_RUNNERS_DURATION)) {

        synchronized (arrayList) {
          if (arrayList.size() >= LIST_MAX) {
            arrayList.remove(0);
          }
          ArrayList newList = new ArrayList();
          for (int i = 0; i < INNER_LIST_SIZE; i++) {
            newList.add(new Object());
          }
          arrayList.add(newList);
        }
      }
      log("Internal client " + getApplicationId() + " completed");
    }

    static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

    private static void log(String message) {
      System.err.println(Thread.currentThread().getName() + " :: "
                         + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
    }

    private void spawnDisconnectingClients() throws Exception {

      log("BroadcastDisconnectingClientApp " + getApplicationId() + " now spawning clients");
      ArrayList<ExtraL1ProcessControl> clients = new ArrayList<ExtraL1ProcessControl>();
      int id = 1;
      for (long t = 0; t < LONG_RUNNERS_DURATION; t += SHORT_RUNNERS_INTERVAL) {
        Thread.sleep(SHORT_RUNNERS_INTERVAL);
        clients.add(createClient(id++, SHORT_RUNNERS_DURATION));
      }

      id = 1;
      for (ExtraL1ProcessControl client : clients) {
        log("BroadcastDisconnectingClientApp waiting for client " + id);
        int exitCode = client.waitFor();
        if (exitCode == 0) {
          log("DisconnectingALClient " + id + " exited normally");
        } else {
          String errorMsg = "DisconnectingALClient " + id + " EXITED ABNORMALLY with exit code " + exitCode;
          log(errorMsg);
          throw new AssertionError(errorMsg);
        }
        id++;
      }
    }

    private ExtraL1ProcessControl createClient(int id, long duration) throws Exception {
      File workingDir = new File(getConfigFileDirectoryPath(), "client-" + id);
      FileUtils.forceMkdir(workingDir);
      log("Creating DisconnectingALClient " + id + " with duration " + duration);
      List jvmArgs = new ArrayList();
      addTestTcPropertiesFile(jvmArgs);
      ExtraL1ProcessControl client = new ExtraL1ProcessControl(getHostName(), getPort(), DisconnectingALClient.class,
                                                               getConfigFilePath(), Arrays.asList("" + id, ""
                                                                                                           + duration),
                                                               workingDir, jvmArgs);
      client.start();
      return client;
    }

    private static class Stopwatch {
      private long       startTime = -1;
      private final long stopTime  = -1;
      private boolean    running   = false;

      public Stopwatch start() {
        startTime = System.currentTimeMillis();
        running = true;
        return this;
      }

      public long getElapsedTime() {
        if (startTime == -1) { return 0; }
        if (running) {
          return System.currentTimeMillis() - startTime;
        } else {
          return stopTime - startTime;
        }
      }

    }
  }
}
