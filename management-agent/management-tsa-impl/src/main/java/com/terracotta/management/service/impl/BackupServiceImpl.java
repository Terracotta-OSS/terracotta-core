/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.BackupEntity;
import com.terracotta.management.service.BackupService;
import com.terracotta.management.service.TsaManagementClientService;

import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public class BackupServiceImpl implements BackupService {

  private final TsaManagementClientService tsaManagementClientService;

  public BackupServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public Collection<BackupEntity> getBackupStatus() throws ServiceExecutionException {
    return tsaManagementClientService.getBackupsStatus();
  }

  @Override
  public Collection<BackupEntity> backup() throws ServiceExecutionException {
    return tsaManagementClientService.backup();
  }
}
