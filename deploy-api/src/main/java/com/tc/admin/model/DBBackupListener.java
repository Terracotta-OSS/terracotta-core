/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import java.util.EventListener;

public interface DBBackupListener extends EventListener {
  void backupEnabled();

  void backupStarted();

  void backupCompleted();

  void backupFailed(String message);

  void backupProgress(int percentCopied);
}
