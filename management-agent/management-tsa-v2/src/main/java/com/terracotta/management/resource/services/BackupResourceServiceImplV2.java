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
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.BackupEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.service.BackupServiceV2;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for performing TSA backups.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/backups")
public class BackupResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(BackupResourceServiceImplV2.class);

  private final BackupServiceV2 backupService;
  private final RequestValidator requestValidator;

  public BackupResourceServiceImplV2() {
    this.backupService = ServiceLocator.locate(BackupServiceV2.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  public final static String ATTR_BACKUP_NAME_KEY = "name";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<BackupEntityV2> getBackupStatus(@Context UriInfo info) {
    LOG.debug(String.format("Invoking BackupResourceServiceImplV2.getBackupStatus: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "serverNames");

    try {
      return backupService.getBackupStatus(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA backup status", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2<BackupEntityV2> backup(@Context UriInfo info) {
    LOG.debug(String.format("Invoking BackupResourceServiceImplV2.backup: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    Set<String> serverNames = UriInfoUtils.extractLastSegmentMatrixParameterAsSet(info, "serverNames");

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    String backupName = qParams.getFirst(ATTR_BACKUP_NAME_KEY);

    try {
      return backupService.backup(serverNames, backupName);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA backup", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
