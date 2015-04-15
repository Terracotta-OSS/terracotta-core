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

import com.terracotta.management.resource.LogEntity;
import com.terracotta.management.resource.services.utils.TimeStringParser;
import com.terracotta.management.service.LogsService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class LogsServiceImpl implements LogsService {

  private final ServerManagementService serverManagementService;

  public LogsServiceImpl(ServerManagementService serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getLogs(serverNames, null);
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames, long sinceWhen) throws ServiceExecutionException {
    return serverManagementService.getLogs(serverNames, sinceWhen);
  }

  @Override
  public Collection<LogEntity> getLogs(Set<String> serverNames, String sinceWhen) throws ServiceExecutionException {
    if (sinceWhen == null) {
      return getLogs(serverNames);
    } else {
      try {
        return getLogs(serverNames, TimeStringParser.parseTime(sinceWhen));
      } catch (NumberFormatException nfe) {
        throw new ServiceExecutionException("Illegal time string: [" + sinceWhen + "]", nfe);
      }
    }
  }

}
