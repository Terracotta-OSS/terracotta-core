/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.LicenseEntity;
import com.terracotta.management.service.LicenseService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Hung Huynh
 */
public class LicenseServiceImpl implements LicenseService {

  private final ServerManagementService serverManagementService;

  public LicenseServiceImpl(ServerManagementService serverManagementService) {
    this.serverManagementService = serverManagementService;
  }


  @Override
  public Collection<LicenseEntity> getLicenseProperties(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getLicenseProperties(serverNames);
  }

}
