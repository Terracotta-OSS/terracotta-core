/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.LicenseEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing license properties
 * 
 * @author Hung Huynh
 */
public interface LicenseService {

  /**
   * Get the license properties
   * 
   * @return a collection of license properties
   * @throws ServiceExecutionException
   */
  Collection<LicenseEntity> getLicenseProperties(Set<String> serverNames) throws ServiceExecutionException;

}
