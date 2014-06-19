/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
