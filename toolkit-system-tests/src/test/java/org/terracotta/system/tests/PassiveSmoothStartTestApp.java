/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;

import com.tc.object.config.schema.L2DSOConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.Serializable;

import junit.framework.Assert;

public class PassiveSmoothStartTestApp extends ClientBase {
  private final String server1DataLocation;
  private final String server2DataLocation;

  public PassiveSmoothStartTestApp(String[] args) {
    super(args);
    this.server1DataLocation = args[1];
    this.server2DataLocation = args[2];
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    getTestControlMbean().waitUntilPassiveStandBy(0);

    // crate some data to populate db

    ToolkitMap<Integer, Serializable> map = toolkit.getMap("testMap", null, null);
    for (int i = 0; i < 500; i++) {
      map.put(i, new MyObject());
    }
    // Initially,
    // 0 - Active Server Index
    // 1 - Passive Server Index

    File dataHome0 = new File(server1DataLocation);
    File objectDB0 = new File(dataHome0, L2DSOConfig.OBJECTDB_DIRNAME);
    File dirtyObjectDB0 = new File(dataHome0, L2DSOConfig.DIRTY_OBJECTDB_BACKUP_DIRNAME);

    File dataHome1 = new File(server2DataLocation);
    File objectDB1 = new File(dataHome1, L2DSOConfig.OBJECTDB_DIRNAME);
    File dirtyObjectDB1 = new File(dataHome1, L2DSOConfig.DIRTY_OBJECTDB_BACKUP_DIRNAME);

    // Pre-test verifications
    Assert.assertTrue(dataHome1.exists());
    Assert.assertTrue(objectDB1.exists());
    Assert.assertTrue(!dirtyObjectDB1.exists());

    Assert.assertTrue(dataHome0.exists());
    Assert.assertTrue(objectDB0.exists());
    Assert.assertTrue(!dirtyObjectDB0.exists());

    // Test-1 : Crash the passive and restart once. while restarting, dirty-object-db wouldn't allow it to come up. but
    // with RMP-309, it should solve the problem on its own and come back to passive stand-by position
    // in this test automatic restart with clean db is disabled so passive will not comeup in first go
    getTestControlMbean().crashAllPassiveServers(0);
    Thread.sleep(10 * 1000);
    getTestControlMbean().restartCrashedServer(0, 1);
    Thread.sleep(10 * 1000);
    getTestControlMbean().restartCrashedServer(0, 1);
    getTestControlMbean().waitUntilPassiveStandBy(0);

    // Verification 1: parent dirty-objectdb-backup directory created ??
    while (!dirtyObjectDB1.exists()) {
      ThreadUtil.reallySleep(5000);
      System.out.println("XXX waiting for crashed server to create backup for dirty db");
    }

    // Verification 2: dirty-objectdb-<timestamp> directory created ??
    verifyDirtyObjectDbBackupDirs(dirtyObjectDB1, 1);

    System.out.println("XXXXXXXX step 1 completed");

    // Test-2 : Repeating test 1 again, to check if the timestamped back-ups are happening
    getTestControlMbean().crashAllPassiveServers(0);
    Thread.sleep(10 * 1000);
    getTestControlMbean().restartCrashedServer(0, 1);
    Thread.sleep(10 * 1000);
    getTestControlMbean().restartCrashedServer(0, 1);
    getTestControlMbean().waitUntilPassiveStandBy(0);

    // Verification 3: dirty-objectdb-<timestamp> directory created ??
    verifyDirtyObjectDbBackupDirs(dirtyObjectDB1, 2);

    System.out.println("XXXXXXXX step 2 completed");

    // Test-3 : Crash the Active and restart once. Actually speaking, crashing the server node and restarting it when
    // there is no shared objects by the client should not trigger any dirty object db problems. The old active should
    // be able to join the cluster back as a passive even without RMP-309. but since the test framework uses some shared
    // objects, dirty-db problem is expected to happen.
    getTestControlMbean().crashActiveServer(0);
    Thread.sleep(10 * 1000);
    // won't restart since it will have a dirty db
    getTestControlMbean().restartCrashedServer(0, 0);
    Thread.sleep(10 * 1000);
    // now restart, this should start
    getTestControlMbean().restartCrashedServer(0, 0);
    getTestControlMbean().waitUntilPassiveStandBy(0);

    // Verification 1: parent dirty-objectdb-backup directory created ??
    while (!dirtyObjectDB0.exists()) {
      ThreadUtil.reallySleep(5000);
      System.out.println("XXX waiting for crashed server to create backup for dirty db");
    }

    // Verification 2: dirty-objectdb-<timestamp> directory created ??
    verifyDirtyObjectDbBackupDirs(dirtyObjectDB0, 1);

    System.out.println("XXXXXXXX step 3 completed");
    // Test-4 : Crash the new Passive
    getTestControlMbean().crashAllPassiveServers(0);
    Thread.sleep(10 * 1000);
    System.out.println("crahsed passive");
    getTestControlMbean().restartCrashedServer(0, 0);
    System.out.println("here");
    Thread.sleep(10 * 1000);
    getTestControlMbean().restartCrashedServer(0, 0);
    System.out.println("here 1");
    getTestControlMbean().waitUntilPassiveStandBy(0);

    // Verification 3: dirty-objectdb-<timestamp> directory created ??
    verifyDirtyObjectDbBackupDirs(dirtyObjectDB0, 2);

    System.out.println("XXX Success");
    return;
  }

  private void verifyDirtyObjectDbBackupDirs(File dirtyObjectDB, int expectedBackupCount) {
    File[] dirtyObjectDBTimeStampedDirs = dirtyObjectDB.listFiles();

    while (dirtyObjectDBTimeStampedDirs.length != expectedBackupCount) {
      System.out.println("XXX waiting for data backup dir creation. current backups: "
                         + dirtyObjectDBTimeStampedDirs.length + "; expected: " + expectedBackupCount);
      ThreadUtil.reallySleep(5000);
    }

    for (File dirtyObjectDBTimeStampedDir : dirtyObjectDBTimeStampedDirs) {
      Assert.assertTrue(new String(dirtyObjectDBTimeStampedDirs[0].getName())
          .startsWith(L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX));
      System.out.println("XXX Successfully created Timestamped DirtyObjectDB Backup dir "
                         + dirtyObjectDBTimeStampedDir.getAbsolutePath());
    }
  }

  private static class MyObject implements Serializable {
    //
  }
}
