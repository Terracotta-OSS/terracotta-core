/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.management.TerracottaMBean;

import java.io.IOException;

public interface ServerDBBackupMBean extends TerracottaMBean {

  public static final String BACKUP_ENABLED    = "com.tc.management.beans.object.serverdbbackup.enabled";
  public static final String PERCENTAGE_COPIED = "com.tc.management.beans.object.serverdbbackup.percentagecopied";
  public static final String BACKUP_STARTED    = "com.tc.management.beans.object.serverdbbackup.backupstarted";
  public static final String BACKUP_COMPLETED  = "com.tc.management.beans.object.serverdbbackup.backupcompleted";
  public static final String BACKUP_FAILED     = "com.tc.management.beans.object.serverdbbackup.backupfailed";

  public String getDefaultPathForBackup();

  public boolean isBackupEnabled();

  public boolean isBackUpRunning();

  public void runBackUp() throws IOException;

  public void runBackUp(String path) throws IOException;

  public String getDbHome();
}
