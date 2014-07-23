/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

/**
 * @author Ludovic Orban
 */
public interface L1MBeansSource {

  /**
   * Check if the current server contains the tunneled JMX MBeans
   * @return true if the current server contains the tunneled JMX MBeans, false otherwise
   */
  boolean containsJmxMBeans();

  /**
   * Get the URL of the L2 containing the tunneled JMX MBeans
   * @return the URL, or null if there is no server containing the MBeans
   * @throws ServiceExecutionException
   */
  String getActiveL2ContainingMBeansUrl() throws ServiceExecutionException;

}
