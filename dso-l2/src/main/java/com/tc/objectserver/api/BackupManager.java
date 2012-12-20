package com.tc.objectserver.api;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

/**
 * @author tim
 */
public interface BackupManager {
  enum BackupStatus {
    UNKNOWN, INIT, RUNNING, COMPLETE, FAILED;

    public void putStatus(RandomAccessFile raf) throws IOException {
      raf.seek(0);
      raf.write(new byte[] { (byte) ordinal() }, 0, 1);
    }

    public static BackupStatus getStatus(RandomAccessFile raf) throws IOException {
      raf.seek(0);
      byte b = raf.readByte();
      if (b < 0 || b >= values().length) {
        return UNKNOWN;
      } else {
        return values()[b];
      }
    }
  }

  BackupStatus getBackupStatus(String name) throws IOException;

  Map<String, BackupStatus> getBackupStatuses() throws IOException;

  String getRunningBackup();

  void backup(String name) throws IOException;
}
