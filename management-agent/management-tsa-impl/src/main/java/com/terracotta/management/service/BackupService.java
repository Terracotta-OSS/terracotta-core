/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.BackupEntity;

import java.util.Collection;

/**
 * An interface for service implementations providing TSA backing up facilities.

 * @author Ludovic Orban
 */
public interface BackupService {

  /**
   * Get a collection {@link com.terracotta.management.resource.BackupEntity} objects each representing a server
   * config. Only requested servers are included, or all of them if serverNames is null.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<BackupEntity> getBackupStatus() throws ServiceExecutionException;

  Collection<BackupEntity> backup() throws ServiceExecutionException;

}
