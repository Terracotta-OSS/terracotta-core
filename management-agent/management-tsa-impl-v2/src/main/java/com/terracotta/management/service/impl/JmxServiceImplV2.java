/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.MBeanEntityV2;
import com.terracotta.management.service.JmxServiceV2;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class JmxServiceImplV2 implements JmxServiceV2 {

  private final ServerManagementServiceV2 serverManagementService;

  public JmxServiceImplV2(ServerManagementServiceV2 serverManagementService) {
    this.serverManagementService = serverManagementService;
  }

  @Override
  public Collection<MBeanEntityV2> queryMBeans(Set<String> serverNames, String query) throws ServiceExecutionException {
    return serverManagementService.queryMBeans(serverNames, query);
  }
}
