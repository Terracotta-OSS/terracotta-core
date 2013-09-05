/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.MBeanEntity;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for service implementations providing TSA JMX querying facilities.
 *
 * @author Ludovic Orban
 */
public interface JmxService {

  /**
   * Get all the MBeans of the specified servers
   * @param serverNames A set of server names, null meaning all of them.
   * @param query A JMX query
   * @return a collection of MBeans
   * @throws org.terracotta.management.ServiceExecutionException
   */
  Collection<MBeanEntity> queryMBeans(Set<String> serverNames, String query) throws ServiceExecutionException;


}
