/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.BackupEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.BackupService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
@Path("/agents/backups")
public class BackupResourceServiceImpl {

  private static final Logger LOG = LoggerFactory.getLogger(BackupResourceServiceImpl.class);

  private final BackupService backupService;
  private final RequestValidator requestValidator;

  public BackupResourceServiceImpl() {
    this.backupService = ServiceLocator.locate(BackupService.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  public final static String ATTR_BACKUP_NAME_KEY = "name";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<BackupEntity> getBackupStatus(@Context UriInfo info) {
    LOG.debug(String.format("Invoking BackupResourceServiceImpl.getBackupStatus: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("serverNames");
    Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

    try {
      return backupService.getBackupStatus(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA backup status", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<BackupEntity> backup(@Context UriInfo info) {
    LOG.debug(String.format("Invoking BackupResourceServiceImpl.backup: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("serverNames");
    Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    String backupName = qParams.getFirst(ATTR_BACKUP_NAME_KEY);

    try {
      return backupService.backup(serverNames, backupName);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA backup", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
