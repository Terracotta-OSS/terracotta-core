/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.web.proxy.ProxyException;

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
   * Get the name of the L2 containing the tunneled JMX MBeans
   * @return the name, or null if there is no server containing the MBeans
   * @throws ServiceExecutionException
   */
  String getActiveL2ContainingMBeansName() throws ServiceExecutionException;

  /**
   * Notify that the current request should be proxied to the L2 containing the L1 MBeans. This method always
   * returns by throwing an exception.
   * @throws ProxyException thrown to notify that the current request should be proxied. This exception should be
   *                        caught and processed to perform the actual proxying.
   * @throws ServiceExecutionException
   */
  void proxyClientRequest() throws ProxyException, ServiceExecutionException;

}
