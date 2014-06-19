/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.BackupEntityV2;

import java.util.Set;

/**
 * An interface for service implementations providing TSA backing up facilities.

 * @author Ludovic Orban
 */
public interface BackupServiceV2 {

  /**
   * Get a collection {@link com.terracotta.management.resource.BackupEntityV2} objects each representing a server
   * config. Only requested servers are included, or all of them if serverNames is null.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<BackupEntityV2> getBackupStatus(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Perform backup on the specified servers using the specified backup file name. If the backup name is left to null
   * one is generated automatically.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @param backupName The name of the backup, or null to generate a name.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntityV2} objects.
   * @throws ServiceExecutionException
   */
  ResponseEntityV2<BackupEntityV2> backup(Set<String> serverNames, String backupName) throws ServiceExecutionException;

}
