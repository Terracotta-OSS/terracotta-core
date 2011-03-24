/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.db.DirtyObjectDbCleaner;
import com.tc.objectserver.persistence.db.TCMapStore;
import com.tc.objectserver.persistence.inmemory.NullPersistenceTransactionProvider;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.FilenameFilter;

/**
 * There is already a system test for checking creation of backup directory for old database: PassiveSmoothStartTest .
 * This test checks only the rollback mechanism for dirty db backups: CDV-1108
 */
public class DirtyObjectDBRollbackTest extends TCTestCase {

  private File                     dbHome;
  private File                     dataPath;
  private BerkeleyDBEnvironment    dbenv;
  private TCFile                   dirtyDbBackupPath = null;
  private final TCLogger           logger            = TCLogging.getLogger(DirtyObjectDBRollbackTest.class);
  private TestDirtyObjectDBCleaner dbCleaner;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // data path is tests' temp directory
    dataPath = getTempDirectory();

    // create dbHome dir
    dbHome = new File(dataPath.getAbsolutePath(), L2DSOConfig.OBJECTDB_DIRNAME);
    dbHome.mkdir();

    dbenv = new BerkeleyDBEnvironment(true, dbHome);
    dbenv.open();

    PersistenceTransactionProvider persistentTxProvider = new NullPersistenceTransactionProvider();
    PersistentMapStore persistentMapStore = new TCMapStore(persistentTxProvider, logger,
                                                           dbenv.getClusterStateStoreDatabase());

    // create db backup dir
    dirtyDbBackupPath = new TCFileImpl(new File(dataPath.getAbsolutePath() + File.separator
                                                + L2DSOConfig.DIRTY_OBJECTDB_BACKUP_DIRNAME));
    dirtyDbBackupPath.forceMkdir();

    System.out.println("XXX dbHome: " + dbHome.getAbsolutePath());
    System.out.println("XXX dbBckup: " + dirtyDbBackupPath.getFile().getAbsolutePath());

    dbCleaner = new TestDirtyObjectDBCleaner(persistentMapStore, dbHome, logger);
  }

  private void createBackupDirs(File parentDir, String prefix, int count) {
    Assert.eval(parentDir.exists());
    for (int i = 1; i <= count; i++) {
      File tmp = new File(parentDir, prefix + i);
      if (!tmp.mkdir()) { throw new AssertionError("Unable to create backup dirs"); }
      ThreadUtil.reallySleep(1000);
    }
  }

  private void cleanBackupDirs(File parentDir, String prefix) {
    Assert.eval(parentDir.exists());
    File[] backupDirs = getDbBackupDirs(parentDir);
    for (File dir : backupDirs) {
      if (dir.delete()) {
        System.out.println("XXX cleanup - deleted bkup dir :" + dir);
      } else {
        System.out.println("XXX cleanup - not able to delete bkup dir : " + dir);
      }
    }
  }

  private File[] getDbBackupDirs(File parentDir) {
    Assert.eval(parentDir.exists());
    File[] dirs = parentDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        if (name.startsWith(L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX)) { return true; }
        return false;
      }
    });
    return dirs;
  }

  public void testRollbackMore() throws Exception {
    createBackupDirs(dirtyDbBackupPath.getFile(), L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX, 5);
    dbCleaner.rollDirtyObjectDbBackups(dirtyDbBackupPath.getFile(), 3);
    File[] backupDirs = getDbBackupDirs(dirtyDbBackupPath.getFile());

    // check rollback
    Assert.assertEquals(3, backupDirs.length);

    // check is latest backup is retained
    boolean found = false;
    for (File dir : backupDirs) {
      if (dir.getAbsolutePath().contains("-5")) {
        found = true;
      }
    }
    Assert.eval(found);
  }

  public void testRollbackLess() throws Exception {
    createBackupDirs(dirtyDbBackupPath.getFile(), L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX, 3);
    dbCleaner.rollDirtyObjectDbBackups(dirtyDbBackupPath.getFile(), 5);
    Assert.assertEquals(3, getDbBackupDirs(dirtyDbBackupPath.getFile()).length);
  }

  public void testRollbackNone() throws Exception {
    createBackupDirs(dirtyDbBackupPath.getFile(), L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX, 5);
    dbCleaner.rollDirtyObjectDbBackups(dirtyDbBackupPath.getFile(), 0);
    Assert.assertEquals(5, getDbBackupDirs(dirtyDbBackupPath.getFile()).length);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    dbenv.close();
    cleanBackupDirs(dirtyDbBackupPath.getFile(), L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX);
  }

  static class TestDirtyObjectDBCleaner extends DirtyObjectDbCleaner {

    public TestDirtyObjectDBCleaner(PersistentMapStore clusterStateStore, File dataPath, TCLogger logger) {
      super(clusterStateStore, dataPath, logger);
    }

    @Override
    protected void rollDirtyObjectDbBackups(File dirtyDbBackupPath1, int nofBackups) {
      super.rollDirtyObjectDbBackups(dirtyDbBackupPath1, nofBackups);
    }

  }

}
