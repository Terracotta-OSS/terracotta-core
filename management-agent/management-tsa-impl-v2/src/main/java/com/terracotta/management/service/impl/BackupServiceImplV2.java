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
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.BackupEntityV2;
import com.terracotta.management.service.BackupServiceV2;

import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class BackupServiceImplV2 implements BackupServiceV2 {

  private final ServerManagementServiceV2 serverManagementService;

  public BackupServiceImplV2(ServerManagementServiceV2 serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public ResponseEntityV2<BackupEntityV2> getBackupStatus(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getBackupsStatus(serverNames);
  }

  @Override
  public ResponseEntityV2<BackupEntityV2> backup(Set<String> serverNames, String backupName) throws ServiceExecutionException {
    return serverManagementService.backup(serverNames, backupName);
  }
}
