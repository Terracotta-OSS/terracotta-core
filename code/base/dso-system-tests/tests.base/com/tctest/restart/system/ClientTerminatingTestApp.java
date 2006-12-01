/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.restart.system;

import org.apache.commons.io.FileUtils;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClientTerminatingTestApp extends AbstractTransparentApp {

  public static final String CONFIG_FILE = "config-file";
  public static final String PORT_NUMBER = "port-number";
  public static final String HOST_NAME   = "host-name";

  private static final int      LOOP_COUNT = 2;
  private static final List     queue      = new ArrayList();

  private int                   id         = -1;
  private long                  count      = 0;
  private ExtraL1ProcessControl client;
  private final String          hostName;
  private final int             port;
  private final File            config;

  public ClientTerminatingTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    config = new File(cfg.getAttribute(CONFIG_FILE));
    hostName = cfg.getAttribute(HOST_NAME);
    port = Integer.parseInt(cfg.getAttribute(PORT_NUMBER));
  }

  public void run() {
    List myList = new ArrayList();
    synchronized (queue) {
      if (id != -1) { throw new AssertionError("Only one controller per Instance allowed. Check the Execution count"); }
      id = queue.size();
      queue.add(myList);
    }

    Random random = new Random();
    int times = LOOP_COUNT;
    try {
      while (times-- >= 0) {
        long toAdd = random.nextInt(10) * 10L + 1;
        File workingDir = new File(config.getParentFile(), "client-" + id + "-" + times);
        FileUtils.forceMkdir(workingDir);
        System.err.println(this + "Creating Client with args " + id + " , " + toAdd);
        client = new ExtraL1ProcessControl(hostName, port, Client.class, config.getAbsolutePath(), new String[] {
            "" + id, "" + toAdd }, workingDir);
        client.start(20000);
        int exitCode = client.waitFor();
        if (exitCode == 0) {
          System.err.println(this + "Client existed Normally");
          verify(myList, toAdd);
        } else {
          String errorMsg = this + "Client existed Abnormally. Exit code = " + exitCode;
          System.err.println(errorMsg);
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

  public String toString() {
    return "Controller(" + id + ") :";
  }

  public static TerracottaConfigBuilder createConfig(int port) {
    String testClassName = ClientTerminatingTestApp.class.getName();
    String testClassSuperName = AbstractTransparentApp.class.getName();
    String clientClassName = Client.class.getName();

    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setMethodExpression("* " + testClassName + ".run(..)");
    lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + clientClassName + ".execute(..)");
    lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    out.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2 });

    RootConfigBuilder root = new RootConfigBuilderImpl();
    root.setFieldName(testClassName + ".queue");
    // root.setRootName("queue");
    out.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root });

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassSuperName + "*");

    out.getApplication().getDSO().setInstrumentedClasses(
                                                         new InstrumentedClassConfigBuilder[] { instrumented1,
                                                             instrumented2 });

    return out;
  }

  public static class Client {
    private int  id;
    private long addCount;

    public Client(int i, long addCount) {
      this.id = i;
      this.addCount = addCount;
    }

    public static void main(String args[]) {
      if (args.length != 2) { throw new AssertionError("Usage : Client <id> <num of increments>"); }

      Client client = new Client(Integer.parseInt(args[0]), Long.parseLong(args[1]));
      client.execute();
    }

    // Writen so that many transactions are created ...
    public void execute() {
      List myList = null;
      long count = 0;
      System.err.println(this + " execute : addCount = " + addCount);
      synchronized (queue) {
        myList = (List) queue.get(id);
      }
      synchronized (myList) {
        if (myList.size() > 0) {
          count = ((Long) myList.get(myList.size() - 1)).longValue();
          myList.clear();
        }
      }
      while (addCount-- > 0) {
        synchronized (myList) {
          myList.add(new Long(++count));
        }
      }
      System.err.println(this + " put till :" + count);
      System.exit(0);
    }

    public String toString() {
      return "Client(" + id + ") :";
    }
  }

}
