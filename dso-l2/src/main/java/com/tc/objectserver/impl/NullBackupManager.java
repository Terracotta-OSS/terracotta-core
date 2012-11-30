package com.tc.objectserver.impl;

import com.tc.objectserver.api.BackupManager;

import java.io.IOException;

/**
 * @author tim
 */
public class NullBackupManager implements BackupManager {
  public static final NullBackupManager INSTANCE = new NullBackupManager();

  @Override
  public BackupStatus getBackupStatus(final String name) throws IOException {
    return BackupStatus.UNKNOWN;
  }

  @Override
  public String getRunningBackup() {
    return null;
  }

  @Override
  public void backup(final String name) throws IOException {
    throw new UnsupportedOperationException("Backups not supported for non-restartable mode.");
  }
}
