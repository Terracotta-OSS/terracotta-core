/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
