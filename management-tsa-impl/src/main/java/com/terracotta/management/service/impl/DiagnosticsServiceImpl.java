/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.service.DiagnosticsService;
import com.terracotta.management.service.JmxClientService;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public class DiagnosticsServiceImpl implements DiagnosticsService {

  private final JmxClientService jmxClientService;

  public DiagnosticsServiceImpl(JmxClientService jmxClientService) {
    this.jmxClientService = jmxClientService;
  }

  @Override
  public Collection<ThreadDumpEntity> getClusterThreadDump() throws ServiceExecutionException {
    try {
      Collection<ThreadDumpEntity> threadDumpEntities = new ArrayList<ThreadDumpEntity>();

      threadDumpEntities.addAll(jmxClientService.serversThreadDump());
      threadDumpEntities.addAll(jmxClientService.clientsThreadDump());

      return threadDumpEntities;
    } catch (Exception e) {
      throw new ServiceExecutionException("error making JMX call", e);
    }
  }

  @Override
  public boolean runDgc() throws ServiceExecutionException {
    return jmxClientService.runDgc();
  }

}
