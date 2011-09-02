/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import org.apache.commons.io.FileUtils;

import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.logging.TCLogger;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.object.persistence.api.ClusterStatePersistentMapStore;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class DirtyObjectDbCleaner {
  private final File               l2DataPath;
  private final PersistentMapStore clusterStateStore;
  private final TCLogger           logger;
  private boolean                  objectDbClean;

  public DirtyObjectDbCleaner(PersistentMapStore clusterStateStore, File l2DataPath, TCLogger logger) {
    this.l2DataPath = l2DataPath;
    this.clusterStateStore = clusterStateStore;
    this.logger = logger;

    init();
  }

  private void init() {
    String dbState = clusterStateStore.get(ClusterStatePersistentMapStore.DBKEY_STATE);
    Assert.eval((dbState == null) || (dbState.equals(ClusterStatePersistentMapStore.DB_STATE_CLEAN))
                || (dbState.equals(ClusterStatePersistentMapStore.DB_STATE_DIRTY)));

    objectDbClean = true;
    if (dbState == null) {
      clusterStateStore.put(ClusterStatePersistentMapStore.DBKEY_STATE, ClusterStatePersistentMapStore.DB_STATE_CLEAN);
    } else if (dbState.equals(ClusterStatePersistentMapStore.DB_STATE_DIRTY)) {
      Assert.eval(l2DataPath != null);
      objectDbClean = false;
    }
  }

  protected boolean isObjectDbDirty() {
    return !objectDbClean;
  }

  // callers should close any open db environment before doing backup
  protected void backupDirtyObjectDb() throws TCDatabaseException {

    String dataPath = this.l2DataPath.getAbsolutePath();
    Assert.assertNotBlank(dataPath);

    TCFile dirtyDbBackupPath = new TCFileImpl(new File(dataPath + File.separator
                                                       + L2DSOConfig.DIRTY_OBJECTDB_BACKUP_DIRNAME));
    if (!dirtyDbBackupPath.exists()) {
      logger.info("Creating dirtyDbBackupPath : " + dirtyDbBackupPath.getFile().getAbsolutePath());
      try {
        dirtyDbBackupPath.forceMkdir();
      } catch (IOException ioe) {
        throw new TCDatabaseException("Not able to create Dirty DB Backup Directory '"
                                      + dirtyDbBackupPath.getFile().getAbsolutePath() + "'. Reason:"
                                      + ioe.getClass().getName());
      }
    } else {
      logger.info("dirtyDbBackupPath : " + dirtyDbBackupPath.getFile().getAbsolutePath());
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
    Date d = new Date();
    String timeStamp = dateFormat.format(d);
    File dirtyDbSourcedir = new File(dataPath + File.separator + L2DSOConfig.OBJECTDB_DIRNAME + File.separator);
    File dirtyDbBackupDestDir = new File(dirtyDbBackupPath + File.separator + L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX
                                         + timeStamp);

    try {
      boolean success = dirtyDbSourcedir.renameTo(dirtyDbBackupDestDir);
      if (!success) {
        logger.warn("Unable to move the dirty objectdb, performing a copy and delete.");
        FileUtils.copyDirectory(dirtyDbSourcedir, dirtyDbBackupDestDir);
        FileUtils.deleteDirectory(dirtyDbSourcedir);
      }
    } catch (Exception e) {
      throw new TCDatabaseException("Not able to move dirty objectdbs to " + dirtyDbBackupDestDir.getAbsolutePath()
                                    + ". Reason: " + e.getMessage());
    }

    Assert.eval(!dirtyDbSourcedir.exists());
    try {
      FileUtils.forceMkdir(dirtyDbSourcedir);
    } catch (IOException e) {
      throw new TCDatabaseException("Not able to create dbhome " + dirtyDbSourcedir.getAbsolutePath() + ". Reason: "
                                    + e.getClass().getName());
    }
    logger.info("Successfully moved dirty objectdb to " + dirtyDbBackupDestDir.getAbsolutePath() + ".");
    rollDirtyObjectDbBackups(dirtyDbBackupPath.getFile(),
                             TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_DIRTYDB_ROLLING, 0));
  }

  protected void rollDirtyObjectDbBackups(File dirtyDbBackupPath, int nofBackups) {
    if (nofBackups <= 0) { return; // nothing to do, there is no rolling of old backups
    }
    Assert.assertNotNull(dirtyDbBackupPath);
    File[] contents = dirtyDbBackupPath.listFiles();
    if (null == contents) return;

    String pathPrefix = dirtyDbBackupPath.getAbsolutePath() + File.separator + L2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX;

    Arrays.sort(contents, new Comparator<File>() {
      public int compare(File o1, File o2) {
        // have the newest first...
        if (o1.lastModified() < o2.lastModified()) return 1;
        else if (o1.lastModified() > o2.lastModified()) return -1;
        else return 0;
      }
    });
    int count = 0;
    for (File file : contents) {
      if (file.isFile()) continue;
      if (!file.getAbsolutePath().startsWith(pathPrefix)) continue;
      if (count++ < nofBackups) continue;
      boolean deleted = deleteDir(file);
      if (deleted) {
        logger.info("Deleted old database backup - " + file.getAbsolutePath());
      } else {
        logger.info("Unable to delete old database backup - " + file.getAbsolutePath());
      }
    }
  }

  private boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      File[] contents = dir.listFiles();
      for (File file : contents) {
        if (!deleteDir(file)) return false;
      }
    }
    return dir.delete();
  }
}
