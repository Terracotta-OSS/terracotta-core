/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.MBeanEntity;
import com.terracotta.management.service.JmxService;
import com.terracotta.management.service.TsaManagementClientService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class JmxServiceImpl implements JmxService {

  private final TsaManagementClientService tsaManagementClientService;

  public JmxServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }

  @Override
  public Collection<MBeanEntity> queryMBeans(Set<String> serverNames, String query) throws ServiceExecutionException {
    return tsaManagementClientService.queryMBeans(serverNames, query);
  }
}
