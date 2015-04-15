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
package com.tc.objectserver.impl;

import com.tc.objectserver.api.BackupManager;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

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
  public String getBackupFailureReason(String name) throws IOException {
    return null;
  }

  @Override
  public String getRunningBackup() {
    return null;
  }

  @Override
  public void backup(final String name) {
    throw new UnsupportedOperationException("Backups not supported for non-restartable mode.");
  }

  @Override
  public Map<String, BackupStatus> getBackupStatuses() {
    return Collections.emptyMap();
  }
}
