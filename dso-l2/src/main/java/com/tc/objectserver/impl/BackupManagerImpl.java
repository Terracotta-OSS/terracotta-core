package com.tc.objectserver.impl;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.api.BackupManager;
import com.tc.objectserver.persistence.Persistor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author tim
 */
public class BackupManagerImpl implements BackupManager {
  private static final TCLogger logger = TCLogging.getLogger(BackupManager.class);
  private static final String STATUS_FILE_NAME = "status";

  private final AtomicReference<String> backupInProgress = new AtomicReference<String>();
  private final Persistor persistor;
  private final File backupPath;
  private final Sink backupSink;

  public BackupManagerImpl(final Persistor persistor, final File backupPath, final Sink backupSink) {
    this.persistor = persistor;
    this.backupPath = backupPath;
    this.backupSink = backupSink;
  }

  private File getStatusFile(final String name) {
    return new File(backupPath, name + File.separator + STATUS_FILE_NAME);
  }

  @Override
  public BackupStatus getBackupStatus(final String name) throws IOException {
    File statusFile = getStatusFile(name);
    if (!statusFile.exists()) {
      return BackupStatus.UNKNOWN;
    }
    RandomAccessFile raf = new RandomAccessFile(statusFile, "r");
    try {
      return BackupStatus.getStatus(raf);
    } finally {
      raf.close();
    }
  }

  @Override
  public String getRunningBackup() {
    return backupInProgress.get();
  }

  @Override
  public void backup(final String name) throws IOException {
    if (!backupInProgress.compareAndSet(null, name)) {
      throw new IllegalStateException("Backup is in progress.");
    }
    RandomAccessFile raf = null;
    try {
      final File backupDir = new File(backupPath, name);
      if (backupDir.exists()) {
        throw new IllegalStateException("Backup named " + name + " already exists.");
      }
      if (!backupDir.mkdirs()) {
        throw new IllegalStateException("Unable to create backup directory " + backupDir.getAbsolutePath());
      }
      final File statusFile = getStatusFile(name);
      raf = new RandomAccessFile(statusFile, "rwd");
      BackupStatus.INIT.putStatus(raf);
      backupSink.add(new BackupContext(backupDir, statusFile));
    } catch (Exception e) {
      backupInProgress.set(null);
      if (raf != null) {
        BackupStatus.FAILED.putStatus(raf);
      }
      throw new RuntimeException(e);
    } finally {
      if (raf != null) {
        raf.close();
      }
    }
  }

  private class BackupContext implements Callable<Void>, EventContext {
    private final File backupDir;
    private final File statusFile;

    private BackupContext(final File backupDir, final File statusFile) {
      this.backupDir = backupDir;
      this.statusFile = statusFile;
    }

    @Override
    public Void call() throws Exception {
      RandomAccessFile raf = new RandomAccessFile(statusFile, "rwd");
      try {
        BackupStatus.RUNNING.putStatus(raf);
        persistor.backup(new File(backupDir, L2DSOConfig.OBJECTDB_DIRNAME));
        BackupStatus.COMPLETE.putStatus(raf);
      } catch (Exception e) {
        BackupStatus.FAILED.putStatus(raf);
        logger.error("Backup failed with exception.", e);
      } finally {
        raf.close();
        backupInProgress.set(null);
      }
      return null;
    }
  }
}
