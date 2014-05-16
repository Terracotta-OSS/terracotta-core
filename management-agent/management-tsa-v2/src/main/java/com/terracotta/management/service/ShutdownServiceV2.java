/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import java.util.Set;

/**
 * An interface for service implementations providing TSA shutdown facilities.
 *
 * @author Ludovic Orban
 */
public interface ShutdownServiceV2 {

  void shutdown(Set<String> serverNames) throws ServiceExecutionException;

}
