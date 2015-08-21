package com.tc.objectserver.api;

import java.io.IOException;
import java.util.Map;

/**
 * @author tim
 */
public interface BackupManager {
  enum BackupStatus {
    UNKNOWN, INIT, RUNNING, COMPLETE, FAILED
  }

  BackupStatus getBackupStatus(String name) throws IOException;

  String getBackupFailureReason(String name) throws IOException;

  Map<String, BackupStatus> getBackupStatuses() throws IOException;

  String getRunningBackup();

  void backup(String name) throws IOException;
}
