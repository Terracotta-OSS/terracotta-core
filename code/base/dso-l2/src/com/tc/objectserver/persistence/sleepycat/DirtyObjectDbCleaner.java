/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import org.apache.commons.io.FileUtils;

import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.logging.TCLogger;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.object.persistence.api.SleepycatClusterStateMapStore;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
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
    String dbState = clusterStateStore.get(SleepycatClusterStateMapStore.DBKEY_STATE);
    Assert.eval((dbState == null) || (dbState.equals(SleepycatClusterStateMapStore.DB_STATE_CLEAN))
                || (dbState.equals(SleepycatClusterStateMapStore.DB_STATE_DIRTY)));

    objectDbClean = true;
    if (dbState == null) {
      clusterStateStore.put(SleepycatClusterStateMapStore.DBKEY_STATE, SleepycatClusterStateMapStore.DB_STATE_CLEAN);
    } else if (dbState.equals(SleepycatClusterStateMapStore.DB_STATE_DIRTY)) {
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
                                                       + NewL2DSOConfig.DIRTY_OBJECTDB_BACKUP_DIRNAME));
    if (!dirtyDbBackupPath.exists()) {
      logger.info("Creating dirtyDbBackupPath : " + dirtyDbBackupPath.getFile().getAbsolutePath());
      try {
        dirtyDbBackupPath.forceMkdir();
      } catch (IOException ioe) {
        throw new TCDatabaseException("Not able to create Dirty DB Backup Directory '"
                                      + dirtyDbBackupPath.getFile().getAbsolutePath() + "'. Reason:" + ioe.getCause());
      }
    } else {
      logger.info("dirtyDbBackupPath : " + dirtyDbBackupPath.getFile().getAbsolutePath());
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
    Date d = new Date();
    String timeStamp = dateFormat.format(d);
    File dirtyDbSourcedir = new File(dataPath + File.separator + NewL2DSOConfig.OBJECTDB_DIRNAME + File.separator);
    File dirtyDbBackupDestDir = new File(dirtyDbBackupPath + File.separator
                                         + NewL2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX + timeStamp);

    try {
      boolean success = dirtyDbSourcedir.renameTo(dirtyDbBackupDestDir);
      if (!success) { throw new TCDatabaseException("Not able to move dirty objectdbs to "
                                                    + dirtyDbBackupDestDir.getAbsolutePath()); }
    } catch (Exception e) {
      throw new TCDatabaseException("Not able to move dirty objectdbs to " + dirtyDbBackupDestDir.getAbsolutePath()
                                    + ". Reason: " + e.getCause());
    }

    Assert.eval(!dirtyDbSourcedir.exists());
    try {
      FileUtils.forceMkdir(dirtyDbSourcedir);
    } catch (IOException e) {
      throw new TCDatabaseException("Not able to create dbhome " + dirtyDbSourcedir.getAbsolutePath() + ". Reason: "
                                    + e.getCause());
    }

    File reasonFile = new File(dirtyDbBackupDestDir, "reason.txt");
    try {
      FileOutputStream out = new FileOutputStream(reasonFile);
      out.write(d.toString().getBytes());
      // out.write(errorMessage.getBytes());
      // PrintStream ps = new PrintStream(out);
      // t.printStackTrace(ps);
      out.close();
    } catch (Exception e1) {
      throw new RuntimeException(e1);
    }
    logger.info("Successfully moved dirty objectdb to " + dirtyDbBackupDestDir.getAbsolutePath() + ".");
  }
}
