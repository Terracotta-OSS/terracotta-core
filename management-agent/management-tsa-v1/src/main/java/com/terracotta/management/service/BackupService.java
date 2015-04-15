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
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.BackupEntity;

import java.util.Collection;
import java.util.Set;

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
  Collection<BackupEntity> getBackupStatus(Set<String> serverNames) throws ServiceExecutionException;

  /**
   * Perform backup on the specified servers using the specified backup file name. If the backup name is left to null
   * one is generated automatically.
   *
   * @param serverNames A set of server names, null meaning all of them.
   * @param backupName The name of the backup, or null to generate a name.
   * @return a collection {@link com.terracotta.management.resource.ConfigEntity} objects.
   * @throws ServiceExecutionException
   */
  Collection<BackupEntity> backup(Set<String> serverNames, String backupName) throws ServiceExecutionException;

}
