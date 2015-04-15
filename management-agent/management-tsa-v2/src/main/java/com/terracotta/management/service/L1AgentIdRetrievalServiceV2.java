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

/**
 * @author Anthony Dahanne
 *
 * This service allows you to retrieve the agentId of the L1, from its remote address.
 * Several L1s can be started from a single VM (a L1 for each CM), and while they share their agentId,
 * they each have their own RemoteAddress (toolkit connection)
 *
 */
public interface L1AgentIdRetrievalServiceV2 {
  String getAgentIdFromRemoteAddress(String remoteAddress, String clientID) throws ServiceExecutionException;
}
