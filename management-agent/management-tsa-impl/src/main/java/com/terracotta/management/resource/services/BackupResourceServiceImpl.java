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

import java.util.Collection;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
@Path("/agents/backups")
public class BackupResourceServiceImpl implements BackupResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(BackupResourceServiceImpl.class);

  private final BackupService backupService;
  private final RequestValidator requestValidator;

  public BackupResourceServiceImpl() {
    this.backupService = ServiceLocator.locate(BackupService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }

  @Override
  public Collection<BackupEntity> getBackupStatus(UriInfo info) {
    LOG.debug(String.format("Invoking BackupResourceServiceImpl.getBackupStatus: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return backupService.getBackupStatus();
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA backup status", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<BackupEntity> backup(UriInfo info) {
    LOG.debug(String.format("Invoking BackupResourceServiceImpl.backup: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      return backupService.backup();
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to perform TSA backup", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}
