/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ServerControl;
import com.tc.serverdbbackuprunner.RunnerUtility;
import com.tc.serverdbbackuprunner.ServerDBBackupRunner;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;

public class ServerDBBackupTestApp extends AbstractTransparentApp {
  public static final String   SERVER_DB_BACKUP = "data-backup";
  public static final String   JMX_PORT         = "jmx-port";

  private ArrayList<IntNumber> mySharedArrayList;
  private String               serverDbBackup;
  private int                  jmxPort;
  private ServerControl        serverControl;
  private String               dbHome;
  private CyclicBarrier        barrier;
  private ServerDBBackupRunner serverDBBackupRunner;

  public ServerDBBackupTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    mySharedArrayList = new ArrayList<IntNumber>();
    serverDbBackup = cfg.getAttribute(SERVER_DB_BACKUP);
    jmxPort = Integer.parseInt(cfg.getAttribute(JMX_PORT));
    serverControl = cfg.getServerControl();
    serverDBBackupRunner = new ServerDBBackupRunner("localhost", jmxPort);
  }

  public void run() {
    System.out.println("The server backup path as mentioned in the tc-config file is  " + serverDbBackup);
    setDbHome();
    System.out.println("The DB home is  " + dbHome);

    verify(0);
    waitOnBarrier();

    int totalAdditions = 5000;
    int currentNoOfObjects = 0;

    addToList(totalAdditions);
    currentNoOfObjects += 2 * totalAdditions;
    
    testServerDataBackupPath();

    int objectsAdded = testIncrementalBackup(totalAdditions, currentNoOfObjects);
    currentNoOfObjects += objectsAdded;

    objectsAdded = testFullBackup(totalAdditions, currentNoOfObjects);
    currentNoOfObjects += objectsAdded;

    objectsAdded = testInValidDirectoies(totalAdditions, currentNoOfObjects);
    currentNoOfObjects += objectsAdded;

    testConcurrentBackups(totalAdditions, currentNoOfObjects);
  }
  
  private void testServerDataBackupPath() {
    if (waitOnBarrier() != 0) {
      final String dbBackupPath = serverDbBackup + File.separator + "serverDbBackupPathTestDbFiles";
      try {
        serverDBBackupRunner.runBackup(dbBackupPath);
      } catch (IOException e) {
        Assert.fail(e.getMessage());
      }
      Assert.assertEquals(dbBackupPath, serverDBBackupRunner.getBackupPath());
    }
    
    waitOnBarrier();
  }

  private int testIncrementalBackup(final int totalAdditions, int currentNoOfObjects) {
    final String dbBackupPath = serverDbBackup + File.separator + "incrementalBackupTestDbFiles";

    if (waitOnBarrier() != 0) {
      try {
        final SynchronizedBoolean isBackupComplete = new SynchronizedBoolean(false);

        Runnable dbBackUpIncremental = new Runnable() {
          public void run() {
            for (int i = 0; i < 100; i++) {
              try {
                serverDBBackupRunner.runBackup(dbBackupPath);
              } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
              }
            }
            isBackupComplete.commit(false, true);
            synchronized (isBackupComplete) {
              isBackupComplete.notify();
            }
          }
        };

        Thread t1 = new Thread(dbBackUpIncremental);
        t1.start();

        addToList(totalAdditions);

        if (!isBackupComplete.get()) {
          synchronized (isBackupComplete) {
            isBackupComplete.wait();
          }
        }

        cleanDBAndRestartServer(dbBackupPath);
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    waitOnBarrier();
    currentNoOfObjects += totalAdditions;

    verify(currentNoOfObjects);
    waitOnBarrier();

    return totalAdditions;
  }

  private int testFullBackup(int totalAdditions, int currentNoOfObjects) {
    addToList(totalAdditions);
    currentNoOfObjects += 2 * totalAdditions;

    String dbBackupPath = serverDbBackup + File.separator + "fullBackupTestDbFiles";

    if (waitOnBarrier() != 0) {
      try {
        serverDBBackupRunner.runBackup(dbBackupPath);
        cleanDBAndRestartServer(dbBackupPath);
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    waitOnBarrier();
    verify(currentNoOfObjects);
    waitOnBarrier();

    return totalAdditions * 2;
  }

  private int testInValidDirectoies(int totalAdditions, int currentNoOfObjects) {
    String dbBackupPath = "xyz";

    if (waitOnBarrier() != 0) {
      try {
        File file = new File(dbBackupPath);
        file.createNewFile();
        
        // create and add notifications
        final NotificationListener listener = new NotificationListenerImpl();
        final NotificationFilter filter = new NotificationFilterImpl();

        serverDBBackupRunner.runBackupWithListener(dbBackupPath, listener, filter, filter, "testBackupFailed");
        throw new AssertionError("Should throw an exception when invalid direcoties are passed in");
      } catch (IOException e) {
        // do nothing
      }
    }
    
    waitOnBarrier();
    addToList(totalAdditions);
    currentNoOfObjects += 2 * totalAdditions;
    waitOnBarrier();
    verify(currentNoOfObjects);
    waitOnBarrier();

    return totalAdditions * 2;
  }

  private void testConcurrentBackups(int totalAdditions, int currentNoOfObjects) {
    final String dbBackupPath = serverDbBackup + File.separator + "concurrentBackupTestDbFiles";
    final int MAX_ATTEMPTS = 20;
    final AtomicInteger backupResult = new AtomicInteger(0);
    if (waitOnBarrier() != 0) {
      Runnable takeBackup = new Runnable() {
        public void run() {
          try {
            ServerDBBackupRunner runner = new ServerDBBackupRunner("localhost", jmxPort);
            runner.runBackup(dbBackupPath);
          } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          } catch (RuntimeException e) {
            synchronized (backupResult) {
              backupResult.decrementAndGet();
              backupResult.notify();
            }
            return;
          }
          synchronized (backupResult) {
            backupResult.incrementAndGet();
            backupResult.notify();
          }
        }
      };

      int attempts = 0;
      while (attempts < MAX_ATTEMPTS) {
        Thread t = new Thread(takeBackup);
        t.start();
        try {
          serverDBBackupRunner.runBackup(dbBackupPath);
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        } catch (RuntimeException e) {
          backupResult.set(-2);
          return;
        }
        synchronized (backupResult) {
          if (backupResult.get() == 0) {
            try {
              backupResult.wait();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
        }

        if (backupResult.get() == -1) return;
        attempts++;
      }
    }

    if (backupResult.get() >= 0) System.out.println("Failed to catch Exception while running concurrent backups");
  }

  private void cleanDBAndRestartServer(String copyFrom) {
    try {
      serverControl.crash();

      FileUtils.cleanDirectory(new File(dbHome));
      FileUtils.copyDirectory(new File(copyFrom), new File(dbHome));

      System.out.println("File copied from " + copyFrom + " to " + dbHome);
      serverControl.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void addToList(int totalAdditions) {
    for (int i = 0; i < totalAdditions; i++) {
      synchronized (mySharedArrayList) {
        IntNumber intNumber = new IntNumber(mySharedArrayList.size());
        mySharedArrayList.add(intNumber);
      }
    }
  }

  private void setDbHome() {
    ServerDBBackupMBean mbean = null;
    final JMXConnector jmxConnector = RunnerUtility.getJMXConnector(null, "localhost", jmxPort);
    MBeanServerConnection mbs;
    try {
      mbs = jmxConnector.getMBeanServerConnection();
    } catch (IOException e1) {
      System.err.println("Unable to connect to host '" + "localhost" + "', port " + jmxPort
                         + ". Are you sure there is a Terracotta server running there?");
      return;
    }
    mbean = (ServerDBBackupMBean) MBeanServerInvocationProxy.newProxyInstance(mbs, L2MBeanNames.SERVER_DB_BACKUP,
                                                                              ServerDBBackupMBean.class, false);
    dbHome = mbean.getDbHome();
    if (dbHome == null) throw new RuntimeException(
                                                   "The DB home is still not set. Check if persistence mode is enabled.");
  }

  private int waitOnBarrier() {
    int index = -1;
    try {
      index = barrier.await();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return index;
  }

  private void verify(int no) {
    synchronized (mySharedArrayList) {
      Iterator<IntNumber> iter = mySharedArrayList.iterator();
      int temp = -1;
      int counter = 0;

      Assert.assertEquals(mySharedArrayList.size(), no);

      while (iter.hasNext()) {
        temp = iter.next().get();
        Assert.assertEquals(counter, temp);
        counter++;
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ServerDBBackupTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(ServerDBBackupTestApp.class.getName());
    config.addIncludePattern(ServerDBBackupTestApp.IntNumber.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";

    spec.addRoot("barrier", "barrier");
    config.addWriteAutolock(methodExpression);
    spec.addRoot("mySharedArrayList", "mySharedArrayList");
  }

  class IntNumber {
    private int i;

    public IntNumber(int i) {
      this.i = i;
    }

    public int get() {
      return i;
    }

    public void set(int i) {
      this.i = i;
    }
  }
}

class NotificationListenerImpl implements NotificationListener, Serializable {
  public void handleNotification(Notification notification, Object handback) {
    System.out.println("Notified for invalid directories: " + notification.getMessage());
  }
}

class NotificationFilterImpl implements NotificationFilter, Serializable {
  public boolean isNotificationEnabled(Notification notification) {
    if(notification.getType().equals(ServerDBBackupMBean.BACKUP_FAILED))
      return true;
    return false;
  } 
}