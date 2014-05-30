/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.LicenseEntityV2;
import com.terracotta.management.service.LicenseServiceV2;

import java.util.Collection;
import java.util.Set;

/**
 * @author Hung Huynh
 */
public class LicenseServiceImplV2 implements LicenseServiceV2 {

  private final ServerManagementServiceV2 serverManagementService;

  public LicenseServiceImplV2(ServerManagementServiceV2 serverManagementService) {
    this.serverManagementService = serverManagementService;
  }


  @Override
  public Collection<LicenseEntityV2> getLicenseProperties(Set<String> serverNames) throws ServiceExecutionException {
    return serverManagementService.getLicenseProperties(serverNames);
  }

}
