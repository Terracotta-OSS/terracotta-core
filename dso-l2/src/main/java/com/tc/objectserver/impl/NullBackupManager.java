package com.tc.objectserver.impl;

import com.tc.objectserver.api.BackupManager;

/**
 * @author tim
 */
public class NullBackupManager implements BackupManager {
  public static final NullBackupManager INSTANCE = new NullBackupManager();

  @Override
  public BackupStatus getBackupStatus(final String name) {
    return BackupStatus.UNKNOWN;
  }

  @Override
  public String getRunningBackup() {
    return null;
  }

  @Override
  public void backup(final String name) {
    throw new UnsupportedOperationException("Backups not supported for non-restartable mode.");
  }
}
