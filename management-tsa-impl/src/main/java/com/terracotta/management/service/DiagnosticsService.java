/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.ThreadDumpEntity;

import java.util.Collection;

/**
 *
 * @author Ludovic Orban
 */
public interface DiagnosticsService {

  Collection<ThreadDumpEntity> getClusterThreadDump() throws ServiceExecutionException;

}
