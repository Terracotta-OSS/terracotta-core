/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.MBeanEntity;
import com.terracotta.management.service.JmxService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class JmxServiceImpl implements JmxService {

  private final ServerManagementService serverManagementService;

  public JmxServiceImpl(ServerManagementService serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<MBeanEntity> queryMBeans(Set<String> serverNames, String query) throws ServiceExecutionException {
    return serverManagementService.queryMBeans(serverNames, query);
  }
}
