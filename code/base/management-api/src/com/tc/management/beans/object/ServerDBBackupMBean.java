/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.management.TerracottaMBean;

import java.io.IOException;

import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

public interface ServerDBBackupMBean extends TerracottaMBean {
  
  public static final String PERCENTAGE_COPIED = "com.tc.management.beans.object.serverdbbackup.percentagecopied";
  public static final String BACKUP_STARTED = "com.tc.management.beans.object.serverdbbackup.backupstarted";
  public static final String BACKUP_COMPLETED = "com.tc.management.beans.object.serverdbbackup.backupcompleted";
  public static final String BACKUP_FAILED = "com.tc.management.beans.object.serverdbbackup.backupfailed";

  public boolean isBackUpRunning();
  
  public String getAbsolutePathForBackup();
  
  public void runBackUp() throws IOException;

  public void runBackUp(String path) throws IOException;
  
  public boolean isBackupEnabled();

  public String getDbHome();
  
  // add and remove notification methods 
  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter,
                                      final Object obj, String listenerName);
  
  public void removeNotificationListener(String listenerName) throws ListenerNotFoundException;
}
