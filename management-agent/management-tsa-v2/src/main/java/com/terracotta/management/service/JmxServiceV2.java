/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.service;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.MBeanEntityV2;

import java.util.Set;

/**
 * An interface for service implementations providing TSA JMX querying facilities.
 *
 * @author Ludovic Orban
 */
public interface JmxServiceV2 {

  /**
   * Get all the MBeans of the specified servers
   * @param serverNames A set of server names, null meaning all of them.
   * @param query A JMX query
   * @return a collection of MBeans
   * @throws org.terracotta.management.ServiceExecutionException
   */
  ResponseEntityV2<MBeanEntityV2> queryMBeans(Set<String> serverNames, String query) throws ServiceExecutionException;


}
