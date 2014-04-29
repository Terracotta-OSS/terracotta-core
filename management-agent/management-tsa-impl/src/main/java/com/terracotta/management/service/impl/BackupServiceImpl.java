/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.BackupEntity;
import com.terracotta.management.service.BackupService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class BackupServiceImpl implements BackupService {

  private final ServerManagementService serverManagementService;

  public BackupServiceImpl(ServerManagementService serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<BackupEntity> getBackupStatus(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getBackupsStatus(serverNames);
  }

  @Override
  public Collection<BackupEntity> backup(Set<String> serverNames, String backupName) throws ServiceExecutionException {
    return serverManagementService.backup(serverNames, backupName);
  }
}
