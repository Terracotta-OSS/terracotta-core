/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.ServerCrashingAppBase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ClientTerminatingTestApp extends ServerCrashingAppBase {
  private static boolean        DEBUG      = false;
  private static boolean        isSynchronousWrite;

  public static final String    FORCE_KILL = "force-kill";

  private static final int      LOOP_COUNT = 2;
  private static final List     queue      = new ArrayList();

  private int                   id         = -1;
  private long                  count      = 0;
  private ExtraL1ProcessControl client;
  private final boolean         forceKill;
  private final String          appId;

  public ClientTerminatingTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    String forceKillVal = cfg.getAttribute(FORCE_KILL);
    if (forceKillVal != null && !forceKillVal.equals("")) {
      forceKill = true;
    } else {
      forceKill = false;
    }

    this.appId = appId;
  }

  public static void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  @Override
  public void runTest() throws Throwable {
    final List myList = new ArrayList();
    synchronized (queue) {
      if (id != -1) { throw new AssertionError("Only one controller per Instance allowed. Check the Execution count"); }
      id = queue.size();

      debugPrintln("******* appId=[" + appId + "] id=[" + id + "]");
      queue.add(myList);
      debugPrintln("*******  adding to queue: mylistInQueue=[" + queue.contains(myList) + "]");
    }

    Random random = new Random();
    int times = LOOP_COUNT;
    try {
      while (times-- > 0) {
        long toAdd = random.nextInt(10) * 50L + 1;
        File workingDir = new File(getConfigFileDirectoryPath(), "client-" + id + "-" + times);
        FileUtils.forceMkdir(workingDir);
        System.err.println(this + "Creating Client with args " + id + " , " + toAdd);
        List jvmArgs = new ArrayList();
        addTestTcPropertiesFile(jvmArgs);
        client = new ExtraL1ProcessControl(getHostName(), getPort(), Client.class, getConfigFilePath(),
                                           Arrays.asList("" + id, "" + toAdd, "" + forceKill), workingDir, jvmArgs);
        client.start();
        int exitCode = client.waitFor();
        if (exitCode == 0) {
          System.err.println(this + "Client exited normally");
          verify(myList, toAdd);
        } else {
          String errorMsg = this + "Client exited abnormally. Exit code = " + exitCode;
          System.err.println("error message: " + errorMsg);
          throw new AssertionError(errorMsg);
        }
      }
    } catch (Exception e) {
      System.err.println(this + " Got - " + e);
      throw new AssertionError(e);
    }

  }

  private void verify(List myList, long toAdd) {
    synchronized (myList) {
      if (toAdd != myList.size()) {
        String errorMsg = this + " Expected " + toAdd + " elements in the list. But Found " + myList.size();
        System.err.println(errorMsg);
        throw new AssertionError(errorMsg);
      }
    }
    for (int i = 0; i < myList.size(); i++) {
      synchronized (myList) {
        if ((++count != ((Long) myList.get(i)).longValue())) {
          String errorMsg = this + " Expected " + count + " value in the list. But Found " + myList.get(i);
          System.err.println(errorMsg);
          throw new AssertionError(errorMsg);
        }
      }
    }
  }

  @Override
  public String toString() {
    return "Controller(" + id + ") :";
  }

  public static class Client {
    private final int     id;
    private long          addCount;
    private final boolean shouldForceKill;
    private final List    clientQueue = new ArrayList();

    public Client(int i, long addCount, boolean shouldForceKill) {
      this.id = i;
      this.addCount = addCount;
      this.shouldForceKill = shouldForceKill;

      debugPrintln("*******  id=[" + id + "]  addCount=[" + this.addCount + "]  shoudlForceKill=["
                   + this.shouldForceKill + "]");
    }

    public static void main(String args[]) {
      if (args.length < 2 || args.length > 3) { throw new AssertionError(
                                                                         "Usage : Client <id> <num of increments> [shouldForceKill]"); }

      boolean shouldForceKill;
      if (args.length == 3 && args[2] != null && !args[2].equals("")) {
        shouldForceKill = Boolean.valueOf(args[2]).booleanValue();
      } else {
        shouldForceKill = false;
      }

      Client client = new Client(Integer.parseInt(args[0]), Long.parseLong(args[1]), shouldForceKill);
      client.execute();
    }

    // Written so that many transactions are created ...
    public void execute() {
      List myList = null;
      long count = 0;
      System.err.println(this + " execute : addCount = " + addCount);
      synchronized (clientQueue) {
        myList = (List) clientQueue.get(id);
      }
      synchronized (myList) {
        if (myList.size() > 0) {
          count = ((Long) myList.get(myList.size() - 1)).longValue();
          myList.clear();
        }
      }
      while (addCount-- > 0) {
        synchronized (myList) {
          myList.add(Long.valueOf(++count));
        }
      }

      if (shouldForceKill) {
        System.err.println(this + " killed forceably :" + count);
        Runtime.getRuntime().halt(0);
      } else {
        System.err.println(this + " put till :" + count);
        System.exit(0);
      }
    }

    @Override
    public String toString() {
      return "Client(" + id + ") :";
    }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = ClientTerminatingTestApp.class.getName();
    String clientClassName = Client.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
    TransparencyClassSpec spec2 = config.getOrCreateSpec(clientClassName);

    String methodExpression = "* " + testClassName + "*.*(..)";
    setLockLevel(config, methodExpression);
    spec.addRoot("queue", "queue");

    methodExpression = "* " + clientClassName + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec2.addRoot("clientQueue", "queue");
  }

  private static void setLockLevel(DSOClientConfigHelper config, String methodExpression) {
    if (isSynchronousWrite) {
      config.addSynchronousWriteAutolock(methodExpression);
      debugPrintln("****** doing synch write");
    } else {
      config.addWriteAutolock(methodExpression);
      debugPrintln("***** doing regular write");
    }
  }

  public static void setSynchronousWrite(boolean isSynchWrite) {
    isSynchronousWrite = isSynchWrite;
  }

}
