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
